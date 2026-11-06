import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { isAdmin } from "../../../../lib/admin";
import { sendPushToUsers } from "../../../../lib/push";
export const runtime = "nodejs";

function normalizeNumber(value: unknown) {
  const raw = String(value ?? "").trim();
  if (!raw) return "";
  const digits = raw.replace(/[^0-9]/g, "");
  return digits ? "+" + digits : "";
}
function makeHandle(name: string, number: string) {
  const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 22) || "official";
  return `${slug}-${number.replace(/\D/g, "").slice(-8)}`;
}

export async function GET() {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const channels = await db.channel.findMany({
    orderBy: { updatedAt: "desc" }, take: 100,
    include: { _count: { select: { members: true, messages: true } } },
  });
  return NextResponse.json({ channels });
}

export async function POST(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const name = String(body.name ?? "").trim().slice(0, 80);
  const number = normalizeNumber(body.channelNumber ?? body.number);
  const description = String(body.description ?? "").trim().slice(0, 500);
  const avatarUrl = typeof body.avatarUrl === "string" ? body.avatarUrl.slice(0, 3500000) : undefined;
  const postBody = String(body.body ?? "").trim().slice(0, 10000);
  const mediaUrl = typeof body.mediaUrl === "string" ? body.mediaUrl : "";
  const mediaType = typeof body.mediaType === "string" ? body.mediaType : "";
  const channelId = typeof body.channelId === "string" ? body.channelId : "";
  if (!postBody && !mediaUrl) return NextResponse.json({ error: "Add text, a photo or a video before publishing." }, { status: 400 });
  if (mediaUrl.length > 3500000) return NextResponse.json({ error: "Media is too large. Use a compressed image or hosted media URL." }, { status: 413 });
  const adminUser = process.env.ADMIN_POST_AUTHOR_USERNAME
    ? await db.user.findUnique({ where: { username: process.env.ADMIN_POST_AUTHOR_USERNAME } })
    : null;
  if (!adminUser) return NextResponse.json({ error: "Set ADMIN_POST_AUTHOR_USERNAME in Vercel and make sure that user exists." }, { status: 503 });
  let channel = channelId ? await db.channel.findUnique({ where: { id: channelId } }) : null;
  if (channelId && !channel) return NextResponse.json({ error: "Selected channel was not found." }, { status: 404 });
  if (!channel) {
    if (name.length < 2 || !/^\\+[0-9]{3,15}$/.test(number)) {
      return NextResponse.json({ error: "Enter a channel name and a valid short number, e.g. +49991." }, { status: 400 });
    }
    if (await db.channel.findFirst({ where: { channelNumber: number } })) {
      return NextResponse.json({ error: "That channel number is already in use." }, { status: 409 });
    }
    let handle = makeHandle(name, number);
    if (await db.channel.findUnique({ where: { handle } })) handle = handle + "-" + Date.now().toString(36);
    channel = await db.channel.create({
      data: { name, handle, channelNumber: number, description, avatarUrl, verified: true, ownerId: adminUser.id },
    });
  }
  const message = await db.channelMessage.create({
    data: { channelId: channel.id, authorId: adminUser.id, body: postBody, mediaUrl: mediaUrl || null, mediaType: mediaType || null },
    include: { author: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } },
  });
  await db.channel.update({ where: { id: channel.id }, data: { updatedAt: new Date() } });
  const recipients = await db.user.findMany({ select: { id: true } });
  const userIds = recipients.map(user => user.id).filter(id => id !== adminUser.id);
  const push = await sendPushToUsers(userIds, {
    title: name,
    body: postBody || (mediaType.startsWith("video") ? "New video from an official channel" : "New media from an official channel"),
    data: { type: "channel_post", channelId: channel.id, channelNumber: number, senderAvatarUrl: avatarUrl || adminUser.avatarUrl || "" },
  });
  return NextResponse.json({ channel, message, broadcast: { recipients: userIds.length, pushSent: push.sent, pushConfigured: push.configured } }, { status: 201 });
}

export async function DELETE(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { channelId } = await req.json().catch(() => ({}));
  if (!channelId) return NextResponse.json({ error: "channelId is required" }, { status: 400 });
  await db.channel.delete({ where: { id: String(channelId) } });
  return NextResponse.json({ ok: true });
}
