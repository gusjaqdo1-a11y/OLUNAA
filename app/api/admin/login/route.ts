import { NextResponse } from "next/server";
import { createAdminSession } from "../../../../../lib/admin";
export const runtime = "nodejs";
export async function POST(req: Request) {
  const { username, password } = await req.json().catch(() => ({}));
  if (!process.env.ADMIN_USERNAME || !process.env.ADMIN_PASSWORD) return NextResponse.json({ error: "Admin credentials are not configured in Vercel." }, { status: 503 });
  if (String(username || "") !== process.env.ADMIN_USERNAME || String(password || "") !== process.env.ADMIN_PASSWORD) return NextResponse.json({ error: "Invalid admin credentials" }, { status: 401 });
  await createAdminSession(process.env.ADMIN_USERNAME);
  return NextResponse.json({ ok: true });
}
