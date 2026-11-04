import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { isAdmin } from "../../../../lib/admin";
export const runtime = "nodejs";

export async function POST(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { userId, groupId, channelId } = await req.json().catch(() => ({}));
  if (!userId || (!!groupId === !!channelId)) return NextResponse.json({ error: "Provide userId and exactly one of groupId or channelId." }, { status: 400 });
  if (groupId) {
    const [user, group] = await Promise.all([db.user.findUnique({ where: { id: String(userId) }, select: { id: true } }), db.group.findUnique({ where: { id: String(groupId) }, select: { id: true } })]);
    if (!user || !group) return NextResponse.json({ error: "User or group not found." }, { status: 404 });
    await db.groupMember.upsert({ where: { groupId_userId: { groupId: String(groupId), userId: String(userId) } }, create: { groupId: String(groupId), userId: String(userId), role: "member" }, update: {} });
  } else {
    const [user, channel] = await Promise.all([db.user.findUnique({ where: { id: String(userId) }, select: { id: true } }), db.channel.findUnique({ where: { id: String(channelId) }, select: { id: true } })]);
    if (!user || !channel) return NextResponse.json({ error: "User or channel not found." }, { status: 404 });
    await db.channelMember.upsert({ where: { channelId_userId: { channelId: String(channelId), userId: String(userId) } }, create: { channelId: String(channelId), userId: String(userId) }, update: {} });
  }
  return NextResponse.json({ ok: true });
}

export async function DELETE(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { userId, groupId, channelId } = await req.json().catch(() => ({}));
  if (!userId || (!!groupId === !!channelId)) return NextResponse.json({ error: "Provide userId and exactly one of groupId or channelId." }, { status: 400 });
  if (groupId) await db.groupMember.deleteMany({ where: { groupId: String(groupId), userId: String(userId) } });
  else await db.channelMember.deleteMany({ where: { channelId: String(channelId), userId: String(userId) } });
  return NextResponse.json({ ok: true });
}
