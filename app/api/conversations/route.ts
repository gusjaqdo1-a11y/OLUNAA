import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";

export const runtime = "nodejs";

export async function GET() {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  await db.user.update({ where: { id: me.id }, data: { lastActiveAt: new Date() } });
  const rows = await db.conversation.findMany({ where: { members: { some: { userId: me.id } } }, include: { members: { include: { user: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true, lastActiveAt: true, settings: true } } } }, messages: { orderBy: { createdAt: "desc" }, take: 1 } }, orderBy: { updatedAt: "desc" }, take: 100 });
  // Keep only the newest direct conversation for each peer; preserve every group thread.
  const seenDirectPeers = new Set<string>();
  const conversations = rows.flatMap(c => {
    const other = c.members.find(m => m.userId !== me.id)?.user;
    if (c.type === "direct" && other) {
      if (seenDirectPeers.has(other.id)) return [];
      seenDirectPeers.add(other.id);
    }
    const last = c.messages[0];
    return [{ id: c.id, type: c.type, other: other ? { id: other.id, username: other.username, displayName: other.displayName, avatarUrl: other.avatarUrl, verified: other.verified, online: other.settings?.showOnlineStatus !== false && Date.now() - other.lastActiveAt.getTime() < 45000, lastSeen: other.settings?.showLastSeen !== false ? other.lastActiveAt : null } : null, lastMessage: last ? { id: last.id, body: last.body, senderId: last.senderId, createdAt: last.createdAt, deliveredAt: last.deliveredAt, seenAt: last.seenAt } : null }];
  });
  return NextResponse.json({ conversations });
}

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { userId } = await req.json();
  if (!userId || userId === me.id) return NextResponse.json({ error: "Invalid recipient" }, { status: 400 });
  const existing = await db.conversation.findFirst({ where: { type: "direct", members: { every: { userId: { in: [me.id, userId] } } } }, include: { members: true } });
  if (existing && existing.members.length === 2) return NextResponse.json({ conversationId: existing.id });
  const conversation = await db.conversation.create({ data: { type: "direct", members: { create: [{ userId: me.id }, { userId }] } } });
  return NextResponse.json({ conversationId: conversation.id }, { status: 201 });
}
