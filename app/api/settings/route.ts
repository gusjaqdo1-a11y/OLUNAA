import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";

export const runtime = "nodejs";

export async function GET() {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const settings = await db.userSettings.findUnique({ where: { userId: me.id } });
  return NextResponse.json({ settings: settings ?? {} });
}

export async function PATCH(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const body = await req.json();
  const allowed = ["showOnlineStatus", "showLastSeen", "readReceipts", "typingIndicators", "messageRequests"] as const;
  const data: Record<string, boolean> = {};
  for (const key of allowed) if (typeof body[key] === "boolean") data[key] = body[key];
  const settings = await db.userSettings.upsert({ where: { userId: me.id }, create: { userId: me.id, ...data }, update: data });
  return NextResponse.json({ settings });
}
