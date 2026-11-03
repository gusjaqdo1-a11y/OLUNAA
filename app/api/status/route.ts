import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
export async function GET(req: Request) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const mine = new URL(req.url).searchParams.get("mine") === "1";
  const stories = await db.story.findMany({ where: mine ? { userId: me.id, expiresAt: { gt: new Date() } } : { expiresAt: { gt: new Date() } }, orderBy: { createdAt: "desc" }, take: 100, include: { user: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } }, views: { include: { viewer: { select: { id: true, username: true, displayName: true, avatarUrl: true } } }, orderBy: { viewedAt: "desc" }, take: 500 } } });
  return NextResponse.json({ stories });
}
export async function POST(req: Request) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { mediaUrl, caption, expiresHours = 24 } = await req.json().catch(() => ({}));
  if (!mediaUrl) return NextResponse.json({ error: "mediaUrl is required" }, { status: 400 });
  const hours = Math.min(Math.max(Number(expiresHours) || 24, 1), 48);
  const story = await db.story.create({ data: { userId: me.id, mediaUrl: String(mediaUrl), caption: caption ? String(caption) : null, expiresAt: new Date(Date.now() + hours * 3600000) } });
  return NextResponse.json({ story }, { status: 201 });
}
export async function PUT(req: Request) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { storyId } = await req.json().catch(() => ({}));
  if (!storyId) return NextResponse.json({ error: "storyId is required" }, { status: 400 });
  const story = await db.story.findUnique({ where: { id: storyId } });
  if (!story || story.expiresAt <= new Date()) return NextResponse.json({ error: "Story not found" }, { status: 404 });
  if (story.userId !== me.id) await db.storyView.upsert({ where: { storyId_viewerId: { storyId, viewerId: me.id } }, update: { viewedAt: new Date() }, create: { storyId, viewerId: me.id } });
  return NextResponse.json({ viewed: true, views: await db.storyView.count({ where: { storyId } }) });
}
