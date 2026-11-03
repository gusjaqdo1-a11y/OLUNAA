import { NextResponse } from "next/server";
import { createHash, randomBytes } from "node:crypto";
import { db } from "../../../../lib/db";

export const runtime = "nodejs";

function hashToken(token: string) {
  return createHash("sha256").update(token).digest("hex");
}

export async function POST(req: Request) {
  try {
    const identifier = String((await req.json()).identifier ?? "").trim().toLowerCase().replace(/^@/, "");
    if (!identifier) return NextResponse.json({ error: "Enter your email or @username." }, { status: 400 });

    const user = await db.user.findFirst({ where: { OR: [{ email: identifier }, { username: identifier }] } });
    const generic = { message: "If the account exists, a password reset link has been sent to its email address." };
    if (!user) return NextResponse.json(generic);

    const apiKey = process.env.RESEND_API_KEY;
    const from = process.env.RESEND_FROM_EMAIL;
    if (!apiKey || !from) return NextResponse.json({ error: "Password recovery email is not configured yet. Add RESEND_API_KEY and RESEND_FROM_EMAIL in Vercel." }, { status: 503 });

    await db.passwordResetToken.deleteMany({ where: { userId: user.id, OR: [{ expiresAt: { lt: new Date() } }, { usedAt: { not: null } }] } });
    const rawToken = randomBytes(32).toString("hex");
    await db.passwordResetToken.create({ data: { tokenHash: hashToken(rawToken), userId: user.id, expiresAt: new Date(Date.now() + 30 * 60 * 1000) } });

    const baseUrl = process.env.NEXT_PUBLIC_APP_URL || "https://olunaa.vercel.app";
    const resetUrl = `${baseUrl}/reset-password?token=${encodeURIComponent(rawToken)}`;
    const response = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        from,
        to: [user.email],
        subject: "Reset your OLUNAA password",
        html: `<div style="font-family:Arial,sans-serif;max-width:520px;margin:auto"><h2>Reset your OLUNAA password</h2><p>We received a request to reset your password.</p><p><a href="${resetUrl}" style="display:inline-block;padding:12px 18px;background:#7047ee;color:#fff;text-decoration:none;border-radius:10px">Reset password</a></p><p>This link expires in 30 minutes. If you did not request this, you can ignore this email.</p></div>`
      })
    });
    if (!response.ok) {
      await db.passwordResetToken.deleteMany({ where: { tokenHash: hashToken(rawToken) } });
      return NextResponse.json({ error: "The reset email could not be sent. Check the email provider configuration." }, { status: 502 });
    }
    return NextResponse.json(generic);
  } catch {
    return NextResponse.json({ error: "Password recovery is temporarily unavailable." }, { status: 500 });
  }
}
