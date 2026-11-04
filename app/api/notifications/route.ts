import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
export async function GET() {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const notifications = await db.notification.findMany({ where: { recipientId: me.id }, orderBy: { createdAt: "desc" }, take: 100, include: { actor: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } });
  const unread = notifications.filter(n => !n.readAt).length;
  return NextResponse.json({ notifications, unread });
}
export async function PATCH(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  if (body.all === true) {
    await db.notification.updateMany({ where: { recipientId: me.id, readAt: null }, data: { readAt: new Date() } });
  } else if (typeof body.id === "string") {
    await db.notification.updateMany({ where: { id: body.id, recipientId: me.id, readAt: null }, data: { readAt: new Date() } });
  } else return NextResponse.json({ error: "Provide a notification id or all=true." }, { status: 400 });
  return NextResponse.json({ ok: true });
}