import { NextResponse } from "next/server";
import { db } from "../../../../../lib/db";
import { getCurrentUser } from "../../../../../lib/auth";
import { publishConversationEvent } from "../../../../../lib/realtime";

export const runtime = "nodejs";

export async function POST(req: Request, context: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id: pollId } = await context.params;
  const body = await req.json().catch(() => ({}));
  const optionId = typeof body.optionId === "string" ? body.optionId : "";
  const poll = await db.poll.findUnique({
    where: { id: pollId },
    include: { message: { select: { id: true, conversationId: true } }, options: true },
  });
  if (!poll) return NextResponse.json({ error: "Poll not found." }, { status: 404 });
  const member = await db.conversationMember.findUnique({
    where: { conversationId_userId: { conversationId: poll.message.conversationId, userId: me.id } },
  });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const option = poll.options.find((item: { id: string }) => item.id === optionId);
  if (!option) return NextResponse.json({ error: "Choose a valid option." }, { status: 400 });
  if (poll.closesAt && poll.closesAt.getTime() <= Date.now()) return NextResponse.json({ error: "This poll is closed." }, { status: 409 });
  const previous = await db.pollVote.findUnique({ where: { pollId_userId: { pollId, userId: me.id } } });
  if (previous?.optionId !== optionId) {
    if (previous) await db.pollOption.update({ where: { id: previous.optionId }, data: { voteCount: { decrement: 1 } } });
    await db.pollOption.update({ where: { id: optionId }, data: { voteCount: { increment: 1 } } });
    await db.pollVote.upsert({
      where: { pollId_userId: { pollId, userId: me.id } },
      create: { pollId, optionId, userId: me.id },
      update: { optionId, createdAt: new Date() },
    });
  }
  const updated = await db.poll.findUnique({
    where: { id: pollId },
    include: { options: true, votes: { select: { userId: true, optionId: true } } },
  });
  try { await publishConversationEvent(poll.message.conversationId, "poll:updated", { poll: updated, messageId: poll.message.id }); }
  catch (error) { console.error("Poll update publish failed", error); }
  return NextResponse.json({ poll: updated });
}
