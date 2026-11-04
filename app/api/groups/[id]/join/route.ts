import { NextResponse } from "next/server";
import { db } from "../../../../../lib/db";
import { getCurrentUser } from "../../../../../lib/auth";
export const runtime = "nodejs";
export async function POST(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const group = await db.group.findUnique({ where: { id }, select: { id: true, name: true } });
  if (!group) return NextResponse.json({ error: "Group not found" }, { status: 404 });
  await db.groupMember.upsert({ where: { groupId_userId: { groupId: id, userId: me.id } }, create: { groupId: id, userId: me.id }, update: {} });
  let conversation = await db.conversation.findFirst({ where: { groupId: id, type: "group" }, select: { id: true } });
  if (!conversation) conversation = await db.conversation.create({ data: { type: "group", groupId: id }, select: { id: true } });
  await db.conversationMember.upsert({ where: { conversationId_userId: { conversationId: conversation.id, userId: me.id } }, create: { conversationId: conversation.id, userId: me.id }, update: {} });
  return NextResponse.json({ joined: true, group, conversationId: conversation.id });
}
export async function DELETE(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const membership = await db.groupMember.findUnique({ where: { groupId_userId: { groupId: id, userId: me.id } } });
  if (!membership) return NextResponse.json({ error: "You are not a group member" }, { status: 404 });
  await db.groupMember.delete({ where: { groupId_userId: { groupId: id, userId: me.id } } });
  const conversations = await db.conversation.findMany({ where: { groupId: id }, select: { id: true } });
  if (conversations.length) await db.conversationMember.deleteMany({ where: { userId: me.id, conversationId: { in: conversations.map(c => c.id) } } });
  return NextResponse.json({ left: true });
}