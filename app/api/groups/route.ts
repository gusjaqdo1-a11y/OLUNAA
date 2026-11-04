import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";

export async function GET() {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const groups = await db.group.findMany({
    where: { members: { some: { userId: me.id } } },
    orderBy: { updatedAt: "desc" },
    take: 100,
    include: {
      members: { include: { user: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } },
      conversations: { where: { members: { some: { userId: me.id } } }, select: { id: true }, take: 1 },
    },
  });
  return NextResponse.json({ groups: groups.map(g => ({ ...g, conversationId: g.conversations[0]?.id ?? null, conversations: undefined })) });
}

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const name = String(body.name || "").trim().slice(0, 80);
  const handle = String(body.handle || "").replace(/^@/, "").toLowerCase();
  const description = String(body.description || "").trim().slice(0, 500);
  const rawMemberIds: unknown[] = Array.isArray(body.memberIds) ? body.memberIds as unknown[] : [];\n  const inviteIds = [...new Set<string>(rawMemberIds.filter((x): x is string => typeof x === "string" && x.length > 0))].slice(0, 99);
  if (name.length < 2 || !/^[a-z0-9_]{3,30}$/.test(handle)) return NextResponse.json({ error: "Enter a group name and a 3-30 character handle." }, { status: 400 });
  if (await db.group.findUnique({ where: { handle }, select: { id: true } })) return NextResponse.json({ error: "That group handle is already taken." }, { status: 409 });
  const validUsers = await db.user.findMany({ where: { id: { in: inviteIds } }, select: { id: true } });
  const memberIds = [...new Set([me.id, ...validUsers.map(u => u.id)])];
  const group = await db.group.create({
    data: {
      name, handle, description,
      members: { create: memberIds.map((userId, i) => ({ userId, role: userId === me.id ? "owner" : "member" })) },
      conversations: { create: { type: "group", members: { create: memberIds.map(userId => ({ userId })) } } },
    },
    include: { members: { include: { user: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } }, conversations: { select: { id: true }, take: 1 } },
  });
  return NextResponse.json({ group: { ...group, conversationId: group.conversations[0]?.id ?? null, conversations: undefined } }, { status: 201 });
}