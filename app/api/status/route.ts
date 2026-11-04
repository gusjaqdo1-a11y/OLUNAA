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
  if (String(mediaUrl).length > 3500000) return NextResponse.json({ error: "Media is too large. Choose a shorter video or smaller photo." }, { status: 413 });
  const allowedHours = [1, 6, 12, 24, 48, 72, 168];
  const requestedHours = Number(expiresHours) || 24;
  const hours = allowedHours.includes(requestedHours) ? requestedHours : 24;
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

export async function DELETE(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { storyId } = await req.json().catch(() => ({}));
  if (!storyId || typeof storyId !== "string") return NextResponse.json({ error: "storyId is required" }, { status: 400 });
  const story = await db.story.findUnique({ where: { id: storyId }, select: { id: true, userId: true } });
  if (!story) return NextResponse.json({ error: "Status not found" }, { status: 404 });
  if (story.userId !== me.id) return NextResponse.json({ error: "You can only delete your own status" }, { status: 403 });
  await db.story.delete({ where: { id: storyId } });
  return NextResponse.json({ deleted: true, storyId });
}
