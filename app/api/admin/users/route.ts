import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { isAdmin } from "../../../../lib/admin";
export const runtime = "nodejs";

export async function GET(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const url = new URL(req.url);
  const q = (url.searchParams.get("q") || "").trim();
  const users = await db.user.findMany({
    where: q ? { OR: [{ username: { contains: q, mode: "insensitive" } }, { email: { contains: q, mode: "insensitive" } }, { displayName: { contains: q, mode: "insensitive" } }] } : undefined,
    orderBy: { createdAt: "desc" }, take: 100,
    select: {
      id: true, username: true, displayName: true, email: true, avatarUrl: true, bio: true,
      verified: true, lastActiveAt: true, createdAt: true,
      _count: { select: { sessions: true, sentMessages: true, stories: true, conversationMembers: true, memberships: true, channelMembers: true, channelMessages: true, reactions: true } },
    },
  });
  return NextResponse.json({ users });
}

export async function PATCH(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { userId, verified } = await req.json().catch(() => ({}));
  if (!userId || typeof verified !== "boolean") return NextResponse.json({ error: "userId and verified are required" }, { status: 400 });
  const user = await db.user.update({ where: { id: String(userId) }, data: { verified }, select: { id: true, username: true, verified: true } });
  return NextResponse.json({ user });
}

export async function DELETE(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { userId, action } = await req.json().catch(() => ({}));
  if (!userId) return NextResponse.json({ error: "userId is required" }, { status: 400 });
  if (action === "revoke-sessions") {
    await db.session.deleteMany({ where: { userId: String(userId) } });
    return NextResponse.json({ ok: true, action });
  }
  await db.user.delete({ where: { id: String(userId) } });
  return NextResponse.json({ ok: true, action: "delete-user" });
}
