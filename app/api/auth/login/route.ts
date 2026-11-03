import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { db } from "../../../../lib/db";
import { createSession } from "../../../../lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const identifier = String(body.identifier ?? "").trim().toLowerCase().replace(/^@/, "");
    const password = String(body.password ?? "");
    const user = await db.user.findFirst({ where: { OR: [{ email: identifier }, { username: identifier }] } });
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) return NextResponse.json({ error: "Incorrect username/email or password." }, { status: 401 });
    await createSession(user.id);
    return NextResponse.json({ user: { id: user.id, email: user.email, username: user.username, displayName: user.displayName } });
  } catch { return NextResponse.json({ error: "Sign in is temporarily unavailable." }, { status: 500 }); }
}
