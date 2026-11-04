import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
export async function GET(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const conversationId = new URL(req.url).searchParams.get("conversationId");
  if (!conversationId) return NextResponse.json({ error: "conversationId is required" }, { status: 400 });
  const member = await db.conversationMember.findUnique({ where: { conversationId_userId: { conversationId, userId: me.id } } });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const cutoff = new Date(Date.now() - 8000);
  const rows = await db.typingState.findMany({ where: { conversationId, userId: { not: me.id }, updatedAt: { gt: cutoff } } });
  const users = await db.user.findMany({ where: { id: { in: rows.map(x => x.userId) } }, select: { id: true, displayName: true, settings: true } });
  const typing = users.filter(u => u.settings?.typingIndicators !== false).map(u => ({ id: u.id, displayName: u.displayName }));
  return NextResponse.json({ typing });
}
export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const conversationId = String(body.conversationId || "");
  if (!conversationId) return NextResponse.json({ error: "conversationId is required" }, { status: 400 });
  const member = await db.conversationMember.findUnique({ where: { conversationId_userId: { conversationId, userId: me.id } } });
  if (!member) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  await db.user.update({ where: { id: me.id }, data: { lastActiveAt: new Date() } });
  if (body.typing === true) {
    await db.typingState.upsert({ where: { conversationId_userId: { conversationId, userId: me.id } }, create: { conversationId, userId: me.id }, update: { updatedAt: new Date() } });
  } else {
    await db.typingState.deleteMany({ where: { conversationId, userId: me.id } });
  }
  return NextResponse.json({ ok: true });
}