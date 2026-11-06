import { NextResponse } from "next/server";
import { db } from "../../../../../lib/db";
import { getCurrentUser } from "../../../../../lib/auth";
import { publishUserEvent } from "../../../../../lib/realtime";

export const runtime = "nodejs";

const allowed = new Set(["accepted", "declined", "busy", "unreachable", "ended", "cancelled"]);

export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const body = await req.json().catch(() => ({}));
  const status = typeof body.status === "string" ? body.status : "";
  if (!allowed.has(status)) return NextResponse.json({ error: "Invalid call status." }, { status: 400 });

  const call = await db.callSession.findUnique({ where: { id } });
  if (!call) return NextResponse.json({ error: "Call not found." }, { status: 404 });
  const isCaller = call.callerId === me.id;
  const isCallee = call.calleeIds.includes(me.id);
  if (!isCaller && !isCallee) return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  const terminal = ["declined", "busy", "unreachable", "ended", "cancelled"].includes(call.status);
  if (terminal) return NextResponse.json({ call, unchanged: true });
  if (status === "accepted" && (!isCallee || call.expiresAt.getTime() <= Date.now())) {
    return NextResponse.json({ error: "This call invitation has expired." }, { status: 409 });
  }
  if (["declined", "busy", "unreachable"].includes(status) && !isCallee) {
    return NextResponse.json({ error: "Only the invited participant can decline this call." }, { status: 403 });
  }
  if (["ended", "cancelled"].includes(status) && !isCaller && !isCallee) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const updated = await db.callSession.update({
    where: { id },
    data: {
      status,
      ...(status === "accepted" ? { answeredAt: new Date() } : {}),
      ...(["declined", "busy", "unreachable", "ended", "cancelled"].includes(status) ? { endedAt: new Date(), endedBy: me.id } : {}),
    },
  });
  const recipients = isCaller ? call.calleeIds : [call.callerId];
  await Promise.allSettled(recipients.map(userId => publishUserEvent(userId, "call:status", {
    callId: call.id, conversationId: call.conversationId, status, userId: me.id, updatedAt: updated.updatedAt ?? new Date().toISOString(),
  })));
  return NextResponse.json({ call: updated });
}
