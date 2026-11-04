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
  const settings = await db.userSettings.findUnique({ where: { userId: me.id } });
  await db.user.update({ where: { id: me.id }, data: { lastActiveAt: new Date() } });
  if (settings?.readReceipts !== false) await db.message.updateMany({ where: { conversationId: id, senderId: { not: me.id }, seenAt: null }, data: { seenAt: new Date(), deliveredAt: new Date() } });
  const messages = await db.message.findMany({ where: { conversationId: id }, orderBy: { createdAt: "asc" }, take: 500, include: { reactions: { select: { emoji: true, userId: true } }, sender: { select: { id: true, displayName: true, username: true, avatarUrl: true } } } });
  return NextResponse.json({ messages });
}
export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const conversationId = String(body.conversationId || "");
  const text = String(body.body ?? "").trim();
  const mediaUrl = body.mediaUrl ? String(body.mediaUrl) : null;
  const mediaType = body.mediaType ? String(body.mediaType) : null;
  const replyToId = body.replyToId ? String(body.replyToId) : null;
  const forwarded = body.forwarded === true;
  if (!conversationId || (!text && !mediaUrl)) return NextResponse.json({ error: "Message is required" }, { status: 400 });
  if (text.length > 12000) return NextResponse.json({ error: "Message is too long." }, { status: 413 });
  if (mediaUrl && mediaUrl.length > 3500000) return NextResponse.json({ error: "Media is too large. Choose a smaller image." }, { status: 413 });
  const member = await db.conversationMember.findUnique({ where: { conversationId_userId: { conversationId, userId: me.id } } });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  let replyData: { replyToId?: string; replyToBody?: string; replyToSender?: string } = {};
  if (replyToId) {
    const original = await db.message.findFirst({ where: { id: replyToId, conversationId }, include: { sender: { select: { displayName: true } } } });
    if (!original) return NextResponse.json({ error: "The message you are replying to is unavailable." }, { status: 404 });
    replyData = { replyToId, replyToBody: original.body || (original.mediaType === "video" ? "Video" : "Photo"), replyToSender: original.sender.displayName };
  }
  const message = await db.message.create({ data: { conversationId, senderId: me.id, body: text, mediaUrl, mediaType, deliveredAt: new Date(), forwarded, ...replyData } });
  await db.conversation.update({ where: { id: conversationId }, data: { updatedAt: new Date() } });
  const recipients = await db.conversationMember.findMany({ where: { conversationId, userId: { not: me.id } }, select: { userId: true } });
  if (recipients.length) await db.notification.createMany({ data: recipients.map(x => ({ recipientId: x.userId, actorId: me.id, type: "message", body: text || (mediaType === "video" ? "Sent a video" : "Sent a photo") })) });
  return NextResponse.json({ message }, { status: 201 });
}