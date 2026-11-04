import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { getCurrentUser } from "../../../../lib/auth";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const token = typeof body.token === "string" ? body.token.trim() : "";
  const platform = body.platform === "ios" ? "ios" : "android";
  if (token.length < 40 || token.length > 4096) return NextResponse.json({ error: "Invalid device token." }, { status: 400 });
  await db.deviceToken.upsert({
    where: { token },
    create: { userId: me.id, token, platform },
    update: { userId: me.id, platform, updatedAt: new Date() },
  });
  return NextResponse.json({ ok: true });
}

export async function DELETE(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => ({}));
  const token = typeof body.token === "string" ? body.token.trim() : "";
  if (!token) return NextResponse.json({ error: "Device token is required." }, { status: 400 });
  await db.deviceToken.deleteMany({ where: { userId: me.id, token } });
  return NextResponse.json({ ok: true });
}
