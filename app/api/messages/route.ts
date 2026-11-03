import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";

export const runtime = "nodejs";

export async function GET(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const id = new URL(req.url).searchParams.get("conversationId");
  if (!id) return NextResponse.json({ error: "conversationId is required" }, { status: 400 });
  const member = await db.conversationMember.findUnique({ where: { conversationId_userId: { conversationId: id, userId: me.id } } });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  await db.message.updateMany({ where: { conversationId: id, senderId: { not: me.id }, seenAt: null }, data: { seenAt: new Date(), deliveredAt: new Date() } });
  const messages = await db.message.findMany({ where: { conversationId: id }, orderBy: { createdAt: "asc" }, take: 500 });
  return NextResponse.json({ messages });
}

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { conversationId, body } = await req.json();
  const text = String(body ?? "").trim();
  if (!conversationId || !text) return NextResponse.json({ error: "Message is required" }, { status: 400 });
  const member = await db.conversationMember.findUnique({ where: { conversationId_userId: { conversationId, userId: me.id } } });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const message = await db.message.create({ data: { conversationId, senderId: me.id, body: text, deliveredAt: new Date() } });
  await db.conversation.update({ where: { id: conversationId }, data: { updatedAt: new Date() } });
  return NextResponse.json({ message }, { status: 201 });
}
