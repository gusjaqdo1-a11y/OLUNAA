import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";

export const runtime = "nodejs";

export async function GET(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const q = new URL(req.url).searchParams.get("q")?.trim().toLowerCase() ?? "";
  if (!q) return NextResponse.json({ users: [], groups: [] });
  const [users, groups] = await Promise.all([
    db.user.findMany({ where: { OR: [{ username: { contains: q, mode: "insensitive" } }, { displayName: { contains: q, mode: "insensitive" } }] }, select: { id: true, username: true, displayName: true, avatarUrl: true, lastActiveAt: true, settings: true }, take: 25 }),
    db.group.findMany({ where: { OR: [{ handle: { contains: q, mode: "insensitive" } }, { name: { contains: q, mode: "insensitive" } }] }, select: { id: true, handle: true, name: true, description: true, avatarUrl: true }, take: 25 })
  ]);
  return NextResponse.json({ users: users.filter(u => u.id !== me.id).map(u => ({ ...u, online: !!u.settings?.showOnlineStatus && Date.now() - u.lastActiveAt.getTime() < 45000 })), groups });
}
