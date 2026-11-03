import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { db } from "../../../../lib/db";
import { createSession } from "../../../../lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const email = String(body.email ?? "").trim().toLowerCase();
    const username = String(body.username ?? "").trim().toLowerCase().replace(/^@/, "");
    const displayName = String(body.displayName ?? "").trim();
    const password = String(body.password ?? "");
    if (!email || !username || !displayName || password.length < 8) return NextResponse.json({ error: "Enter a name, valid email, username and an 8+ character password." }, { status: 400 });
    if (!/^[a-z0-9_]{3,24}$/.test(username)) return NextResponse.json({ error: "Username must be 3–24 characters: letters, numbers or _." }, { status: 400 });
    const existing = await db.user.findFirst({ where: { OR: [{ email }, { username }] } });
    if (existing) return NextResponse.json({ error: existing.email === email ? "Email is already registered." : "Username is already taken." }, { status: 409 });
    const passwordHash = await bcrypt.hash(password, 12);
    const user = await db.user.create({ data: { email, username, displayName, passwordHash, settings: { create: {} } } });
    await createSession(user.id);
    return NextResponse.json({ user: { id: user.id, email: user.email, username: user.username, displayName: user.displayName } });
  } catch { return NextResponse.json({ error: "Registration is temporarily unavailable." }, { status: 500 }); }
}
