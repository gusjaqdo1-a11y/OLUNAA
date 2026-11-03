import { NextResponse } from "next/server";
import { getCurrentUser } from "../../../lib/auth";

export const runtime = "nodejs";

export async function POST() {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  await fetch(new URL("/api/me", "http://localhost"), { cache: "no-store" }).catch(() => null);
  return NextResponse.json({ ok: true, online: true, at: new Date().toISOString() });
}
