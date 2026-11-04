import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";

export async function GET(req: Request) {
  const me = await getCurrentUser();
  const handle = new URL(req.url).searchParams.get("handle");
  if (!handle) {
    const channels = await db.channel.findMany({
      where: me ? { OR: [{ privacy: "public" }, { members: { some: { userId: me.id } } }] } : { privacy: "public" },
      orderBy: { updatedAt: "desc" }, take: 100,
    });
    return NextResponse.json({ channels });
  }
  const channel = await db.channel.findUnique({ where: { handle }, include: { messages: { orderBy: { createdAt: "desc" }, take: 100, include: { author: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } } } });
  if (!channel) return NextResponse.json({ error: "Channel not found" }, { status: 404 });
  if (channel.privacy === "private") {
    if (!me) return NextResponse.json({ error: "Sign in to view this private channel." }, { status: 401 });
    const membership = await db.channelMember.findUnique({ where: { channelId_userId: { channelId: channel.id, userId: me.id } } });
    if (!membership) return NextResponse.json({ error: "This is a private channel." }, { status: 403 });
  }
  return NextResponse.json({ channel, authenticated: !!me });
}

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const name = String(body.name || "").trim().slice(0, 80);
  const handle = String(body.handle || "").replace(/^@/, "").toLowerCase();
  const description = String(body.description || "").trim().slice(0, 500);
  const privacy = body.privacy === "private" ? "private" : "public";
  if (name.length < 2 || !/^[a-z0-9_]{3,30}$/.test(handle)) return NextResponse.json({ error: "Enter a channel name and a 3-30 character username." }, { status: 400 });
  if (await db.channel.findUnique({ where: { handle }, select: { id: true } })) return NextResponse.json({ error: "That channel username is already taken." }, { status: 409 });
  const channel = await db.channel.create({ data: { name, handle, description, privacy, ownerId: me.id, verified: false, members: { create: { userId: me.id, role: "owner" } } } });
  return NextResponse.json({ channel }, { status: 201 });
}
