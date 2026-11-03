import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
export async function GET() { const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 }); return NextResponse.json({ user: me }); }
export async function PATCH(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { displayName, bio, avatarUrl } = await req.json().catch(() => ({}));
  const data: Record<string, unknown> = {};
  if (displayName !== undefined) data.displayName = String(displayName).trim().slice(0, 80);
  if (bio !== undefined) data.bio = String(bio).trim().slice(0, 500);
  if (avatarUrl !== undefined) data.avatarUrl = avatarUrl ? String(avatarUrl).slice(0, 2000) : null;
  const user = await db.user.update({ where: { id: me.id }, data });
  return NextResponse.json({ user });
}
