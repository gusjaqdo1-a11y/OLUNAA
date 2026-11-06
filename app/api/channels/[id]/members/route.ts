import { NextResponse } from "next/server";
import { db } from "../../../../../lib/db";
import { getCurrentUser } from "../../../../../lib/auth";
export const runtime = "nodejs";

export async function GET(_req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const membership = await db.channelMember.findUnique({ where: { channelId_userId: { channelId: id, userId: me.id } } });
  if (!membership) return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const members = await db.channelMember.findMany({ where: { channelId: id }, orderBy: { joinedAt: "asc" }, include: { user: { select: { id: true, username: true, displayName: true, avatarUrl: true } } } });
  return NextResponse.json({ members, role: membership.role });
}
export async function POST(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const membership = await db.channelMember.findUnique({ where: { channelId_userId: { channelId: id, userId: me.id } } });
  if (!membership || !["owner","admin"].includes(membership.role)) return NextResponse.json({ error: "Only channel admins can invite members." }, { status: 403 });
  const body = await req.json().catch(() => ({}));
  const rawIds: unknown[] = Array.isArray(body.userIds) ? body.userIds as unknown[] : [];
  const ids: string[] = [...new Set<string>(rawIds.filter((value): value is string => typeof value === "string" && value.trim().length > 0))].slice(0, 100);
  if (!ids.length) return NextResponse.json({ error: "Choose at least one user." }, { status: 400 });
  const users = await db.user.findMany({ where: { id: { in: ids } }, select: { id: true } });
  for (const user of users) await db.channelMember.upsert({ where: { channelId_userId: { channelId: id, userId: user.id } }, create: { channelId: id, userId: user.id, role: "member" }, update: {} });
  return NextResponse.json({ added: users.length });
}
export async function PATCH(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const owner = await db.channelMember.findUnique({ where: { channelId_userId: { channelId: id, userId: me.id } } });
  if (!owner || owner.role !== "owner") return NextResponse.json({ error: "Only the channel owner can manage admins." }, { status: 403 });
  const body = await req.json().catch(() => ({})); const userId = String(body.userId || ""); const role = body.role === "admin" ? "admin" : "member";
  if (!userId || userId === me.id) return NextResponse.json({ error: "Choose a channel member." }, { status: 400 });
  const target = await db.channelMember.findUnique({ where: { channelId_userId: { channelId: id, userId } } });
  if (!target || target.role === "owner") return NextResponse.json({ error: "That member cannot be changed." }, { status: 404 });
  await db.channelMember.update({ where: { channelId_userId: { channelId: id, userId } }, data: { role } });
  return NextResponse.json({ updated: true, role });
}
export async function DELETE(req: Request, { params }: { params: Promise<{ id: string }> }) {
  const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id } = await params;
  const owner = await db.channelMember.findUnique({ where: { channelId_userId: { channelId: id, userId: me.id } } });
  if (!owner || !["owner","admin"].includes(owner.role)) return NextResponse.json({ error: "Only channel admins can remove members." }, { status: 403 });
  const body = await req.json().catch(() => ({})); const userId = String(body.userId || "");
  if (!userId || userId === me.id) return NextResponse.json({ error: "Choose a member to remove." }, { status: 400 });
  await db.channelMember.deleteMany({ where: { channelId: id, userId } });
  return NextResponse.json({ removed: true });
}
