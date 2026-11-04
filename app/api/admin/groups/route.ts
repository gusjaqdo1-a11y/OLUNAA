import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { isAdmin } from "../../../../lib/admin";
export const runtime = "nodejs";
export async function GET() {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const groups = await db.group.findMany({ orderBy: { updatedAt: "desc" }, take: 100, include: { _count: { select: { members: true, conversations: true } } } });
  return NextResponse.json({ groups });
}
export async function POST(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { name, handle, description } = await req.json().catch(() => ({}));
  const clean = String(handle || "").replace(/^@/, "").toLowerCase();
  if (!name || !/^[a-z0-9_]{3,30}$/.test(clean)) return NextResponse.json({ error: "Name and a valid 3-30 character handle are required." }, { status: 400 });
  const exists = await db.group.findUnique({ where: { handle: clean }, select: { id: true } });
  if (exists) return NextResponse.json({ error: "That group handle is already in use." }, { status: 409 });
  const group = await db.group.create({ data: { name: String(name).trim().slice(0, 80), handle: clean, description: String(description || "").slice(0, 500) } });
  return NextResponse.json({ group }, { status: 201 });
}
export async function DELETE(req: Request) {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { groupId } = await req.json().catch(() => ({}));
  if (!groupId) return NextResponse.json({ error: "groupId is required." }, { status: 400 });
  await db.group.delete({ where: { id: String(groupId) } });
  return NextResponse.json({ ok: true });
}