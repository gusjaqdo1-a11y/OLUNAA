import { NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { createHash } from "node:crypto";
import { db } from "../../../../lib/db";
import { createSession } from "../../../../lib/auth";

export const runtime = "nodejs";

function hashToken(token: string) {
  return createHash("sha256").update(token).digest("hex");
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const token = String(body.token ?? "").trim();
    const password = String(body.password ?? "");
    if (!token || password.length < 8) return NextResponse.json({ error: "Use a valid reset link and an 8+ character password." }, { status: 400 });

    const record = await db.passwordResetToken.findUnique({ where: { tokenHash: hashToken(token) } });
    if (!record || record.usedAt || record.expiresAt < new Date()) return NextResponse.json({ error: "This reset link is invalid or expired. Request a new one." }, { status: 400 });

    const passwordHash = await bcrypt.hash(password, 12);
    await db.$transaction([
      db.user.update({ where: { id: record.userId }, data: { passwordHash, lastActiveAt: new Date() } }),
      db.passwordResetToken.update({ where: { id: record.id }, data: { usedAt: new Date() } }),
      db.session.deleteMany({ where: { userId: record.userId } })
    ]);
    await createSession(record.userId);
    return NextResponse.json({ message: "Password changed successfully." });
  } catch {
    return NextResponse.json({ error: "Password reset is temporarily unavailable." }, { status: 500 });
  }
}
