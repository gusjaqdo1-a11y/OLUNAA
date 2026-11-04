import { NextResponse } from "next/server";
import { db } from "../../../../../lib/db";
import { getCurrentUser } from "../../../../../lib/auth";
export const runtime = "nodejs";
async function context(id: string, userId: string) {
  const membership = await db.groupMember.findUnique({ where: { groupId_userId: { groupId: id, userId } } });
  const conversation = await db.conversation.findFirst({ where: { groupId: id, type: "group" }, select: { id: true } });
  return { membership, conversation };
}
export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params; const { membership } = await context(id, me.id);
  if (!membership) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const members = await db.groupMember.findMany({ where: { groupId: id }, orderBy: { joinedAt: "asc" }, include: { user: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } });
  return NextResponse.json({ members });
}
export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params; const { membership, conversation } = await context(id, me.id);
  if (!membership || !["owner","admin"].includes(membership.role)) return NextResponse.json({ error: "Only group owners and admins can invite members." }, { status: 403 });
  const body = await req.json().catch(() => ({}));
  const ids = Array.isArray(body.userIds) ? [...new Set(body.userIds.map((x: unknown) => String(x)).filter(Boolean))].slice(0, 100) : [];
  if (!ids.length) return NextResponse.json({ error: "Choose at least one user." }, { status: 400 });
  const users = await db.user.findMany({ where: { id: { in: ids } }, select: { id: true } });
  let conversationId = conversation?.id;
  if (!conversationId) conversationId = (await db.conversation.create({ data: { type: "group", groupId: id }, select: { id: true } })).id;
  for (const user of users) {
    await db.groupMember.upsert({ where: { groupId_userId: { groupId: id, userId: user.id } }, create: { groupId: id, userId: user.id, role: "member" }, update: {} });
    await db.conversationMember.upsert({ where: { conversationId_userId: { conversationId, userId: user.id } }, create: { conversationId, userId: user.id }, update: {} });
  }
  return NextResponse.json({ added: users.length, conversationId });
}
export async function DELETE(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params; const { membership, conversation } = await context(id, me.id);
  if (!membership || !["owner","admin"].includes(membership.role)) return NextResponse.json({ error: "Only group owners and admins can remove members." }, { status: 403 });
  const { userId } = await req.json().catch(() => ({}));
  if (!userId || userId === me.id) return NextResponse.json({ error: "Choose a member to remove." }, { status: 400 });
  await db.groupMember.deleteMany({ where: { groupId: id, userId: String(userId) } });
  if (conversation) await db.conversationMember.deleteMany({ where: { conversationId: conversation.id, userId: String(userId) } });
  return NextResponse.json({ removed: true });
}