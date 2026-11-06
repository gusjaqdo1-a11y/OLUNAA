import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { getCurrentUser } from "../../../../lib/auth";
import { sendPushToUsers } from "../../../../lib/push";
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
  const active = await db.callSession.findFirst({ where: { OR: [{ callerId: me.id }, { calleeIds: { hasSome: members.map(x => x.userId) } }], status: { in: ["invited", "accepted"] }, expiresAt: { gt: new Date() } } });
  if (active) return NextResponse.json({ error: "One of the participants is already on another call.", status: "busy" }, { status: 409 });
  const call = await db.callSession.create({ data: { conversationId, callerId: me.id, calleeIds: members.map(x => x.userId), mode, status: "invited", expiresAt: new Date(Date.now() + 60000) } });
  const invite = {
    callId: call.id,
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
  await sendPushToUsers(members.map(x => x.userId), { title: me.displayName, body: mode === "video" ? "Incoming video call" : "Incoming voice call", data: { type: "incoming_call", conversationId, mode, callerName: me.displayName } });
  const failed = results.filter(x => x.status === "rejected").length;
  if (failed === results.length) return NextResponse.json({ error: "Could not notify the other participants." }, { status: 502 });
  return NextResponse.json({ ok: true, callId: call.id, status: call.status, expiresAt: call.expiresAt, notified: results.length - failed, realtimeConfigured: !!process.env.ABLY_API_KEY });
}
