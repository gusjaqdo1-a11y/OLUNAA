import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";

export async function GET() {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  return NextResponse.json({ user: me });
}

export async function PATCH(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const { displayName, bio, avatarUrl, username } = body;
  const data: Record<string, unknown> = {};
  if (displayName !== undefined) data.displayName = String(displayName).trim().slice(0, 80);
  if (bio !== undefined) data.bio = String(bio).trim().slice(0, 500);
  if (avatarUrl !== undefined) data.avatarUrl = avatarUrl ? String(avatarUrl).trim().slice(0, 2000) : null;
  if (username !== undefined) {
    const next = String(username).trim().replace(/^@/, "").toLowerCase();
    if (!/^[a-z0-9_]{3,30}$/.test(next)) return NextResponse.json({ error: "Username must be 3-30 characters: letters, numbers, underscore." }, { status: 400 });
    const taken = await db.user.findFirst({ where: { username: next, NOT: { id: me.id } }, select: { id: true } });
    if (taken) return NextResponse.json({ error: "That username is already taken." }, { status: 409 });
    data.username = next;
  }
  const user = await db.user.update({ where: { id: me.id }, data });
  return NextResponse.json({ user });
}
