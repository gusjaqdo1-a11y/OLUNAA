import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { isAdmin } from "../../../../lib/admin";
export const runtime = "nodejs";
export async function POST(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { name, handle, description, avatarUrl, body } = await req.json().catch(() => ({}));
  if (!name || !handle || !body) return NextResponse.json({ error: "name, handle and body are required" }, { status: 400 });
  let channel = await db.channel.findUnique({ where: { handle: String(handle).replace(/^@/, "").toLowerCase() } });
  if (!channel) channel = await db.channel.create({ data: { name, handle: String(handle).replace(/^@/, "").toLowerCase(), description, avatarUrl, verified: true } });
  const adminUser = process.env.ADMIN_POST_AUTHOR_USERNAME ? await db.user.findUnique({ where: { username: process.env.ADMIN_POST_AUTHOR_USERNAME } }) : null;
  if (!adminUser) return NextResponse.json({ error: "Set ADMIN_POST_AUTHOR_USERNAME in Vercel and make sure that user exists." }, { status: 503 });
  const message = await db.channelMessage.create({ data: { channelId: channel.id, authorId: adminUser.id, body: String(body).trim() }, include: { author: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } });
  await db.channel.update({ where: { id: channel.id }, data: { updatedAt: new Date() } });
  return NextResponse.json({ channel, message }, { status: 201 });
}
