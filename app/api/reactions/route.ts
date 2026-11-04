import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
const allowed = new Set(["👍","❤️","😂","😮","😢","🙏","🔥","🎉"]);
export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { messageId, emoji } = await req.json().catch(() => ({}));
  if (typeof messageId !== "string" || typeof emoji !== "string" || !allowed.has(emoji)) return NextResponse.json({ error: "Choose a valid reaction." }, { status: 400 });
  const message = await db.message.findUnique({ where: { id: messageId }, select: { conversationId: true } });
  if (!message) return NextResponse.json({ error: "Message not found." }, { status: 404 });
  const member = await db.conversationMember.findUnique({ where: { conversationId_userId: { conversationId: message.conversationId, userId: me.id } }, select: { id: true } });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const key = { userId_messageId_emoji: { userId: me.id, messageId, emoji } };
  const existing = await db.reaction.findUnique({ where: key });
  if (existing) await db.reaction.delete({ where: { id: existing.id } });
  else await db.reaction.create({ data: { userId: me.id, messageId, emoji } });
  const reactions = await db.reaction.findMany({ where: { messageId }, select: { emoji: true, userId: true } });
  return NextResponse.json({ reactions });
}