import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { db } from "../../../../lib/db";
import { createSession } from "../../../../lib/auth";
import { sendPushToUsers } from "../../../../lib/push";

export const runtime = "nodejs";

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const identifier = String(body.identifier ?? "").trim().toLowerCase().replace(/^@/, "");
    const password = String(body.password ?? "");
    const user = await db.user.findFirst({ where: { OR: [{ email: identifier }, { username: identifier }] } });
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) return NextResponse.json({ error: "Incorrect username/email or password." }, { status: 401 });
    await createSession(user.id);
    const userAgent = req.headers.get("user-agent") || "Unknown device";
    const device = userAgent.slice(0, 120);
    await db.notification.create({ data: { recipientId: user.id, actorId: user.id, type: "security_login", body: "New sign-in detected: " + device } }).catch(error => console.error("Could not save login alert", error));
    await sendPushToUsers([user.id], { title: "New OLUNAA sign-in", body: "Your account was just signed in on " + device, data: { type: "security_login", userId: user.id } });
    return NextResponse.json({ user: { id: user.id, email: user.email, username: user.username, displayName: user.displayName, avatarUrl: user.avatarUrl, verified: user.verified } });
  } catch { return NextResponse.json({ error: "Sign in is temporarily unavailable." }, { status: 500 }); }
}
