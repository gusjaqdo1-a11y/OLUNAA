import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
import { publishConversationEvent } from "../../../lib/realtime";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const conversationId = typeof body.conversationId === "string" ? body.conversationId : "";
  const question = typeof body.question === "string" ? body.question.trim().slice(0, 240) : "";
  const options = Array.isArray(body.options)
    ? [...new Set(body.options.filter((x: unknown): x is string => typeof x === "string").map((x: string) => x.trim()).filter(Boolean))].slice(0, 10)
    : [];
  if (!conversationId || question.length < 3 || options.length < 2 || options.some((x: string) => x.length > 80)) {
    return NextResponse.json({ error: "A question and at least two options are required." }, { status: 400 });
  }
  const member = await db.conversationMember.findUnique({
    where: { conversationId_userId: { conversationId, userId: me.id } },
  });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const message = await db.message.create({
    data: {
      conversationId,
      senderId: me.id,
      body: "📊 Poll",
      deliveredAt: new Date(),
      poll: { create: { question, multipleChoice: false, options: { create: options.map((text: string) => ({ text })) } } },
    },
    include: {
      sender: { select: { id: true, displayName: true, username: true, avatarUrl: true } },
      poll: { include: { options: true, votes: { select: { userId: true, optionId: true } } } },
    },
  });
  await db.conversation.update({ where: { id: conversationId }, data: { updatedAt: new Date() } });
  try { await publishConversationEvent(conversationId, "message:new", { message, conversationId }); }
  catch (error) { console.error("Poll realtime publish failed", error); }
  return NextResponse.json({ message }, { status: 201 });
}
