import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { getCurrentUser } from "../../../../lib/auth";
import { isConversationMember, publishUserEvent } from "../../../../lib/realtime";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const conversationId = typeof body.conversationId === "string" ? body.conversationId : "";
  const mode = body.mode === "video" ? "video" : "audio";
  if (!conversationId || !(await isConversationMember(conversationId, me.id))) {
    return NextResponse.json({ error: "Conversation not found." }, { status: 403 });
  }
  const members = await db.conversationMember.findMany({
    where: { conversationId, userId: { not: me.id } },
    select: { userId: true },
  });
  if (!members.length) return NextResponse.json({ error: "No other members in this conversation." }, { status: 400 });
  const invite = {
    conversationId,
    callerId: me.id,
    callerName: me.displayName,
    callerAvatar: me.avatarUrl,
    mode,
    roomName: `olunaa-${conversationId}`,
    createdAt: new Date().toISOString(),
  };
  const results = await Promise.allSettled(members.map(async member => {
    await db.notification.create({
      data: { recipientId: member.userId, actorId: me.id, type: "incoming_call", body: mode === "video" ? "Incoming video call" : "Incoming voice call" },
    });
    await publishUserEvent(member.userId, "call:invite", invite);
  }));
  await sendPushToUsers(members.map(x => x.userId), { title: me.displayName, body: mode === "video" ? "Incoming video call" : "Incoming voice call", data: { type: "incoming_call", conversationId, mode } });\n  const failed = results.filter(x => x.status === "rejected").length;
  if (failed === results.length) return NextResponse.json({ error: "Could not notify the other participants." }, { status: 502 });
  return NextResponse.json({ ok: true, notified: results.length - failed, realtimeConfigured: !!process.env.ABLY_API_KEY });
}
