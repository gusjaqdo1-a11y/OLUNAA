import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
export async function GET() { const me = await getCurrentUser(); if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 }); return NextResponse.json({ user: me }); }
export async function PATCH(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { displayName, bio, avatarUrl } = await req.json().catch(() => ({}));
  const user = await db.user.update({ where: { id: me.id }, data: { ...(displayName !== undefined ? { displayName: String(displayName).trim().slice(0, 80) } : {}), ...(bio !== undefined ? { bio: String(bio).trim().slice(0, 500) } : {}), ...(avatarUrl !== undefined ? { avatarUrl: avatarUrl ? String(avatarUrl).slice(0, 2000) : null } : {}) });
  return NextResponse.json({ user });
}
