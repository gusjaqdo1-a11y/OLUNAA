import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";

export const runtime = "nodejs";

export async function GET(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const q = new URL(req.url).searchParams.get("q")?.trim().toLowerCase() ?? "";

  const [users, groups] = await Promise.all([
    db.user.findMany({ where: q ? { OR: [{ username: { contains: q, mode: "insensitive" } }, { displayName: { contains: q, mode: "insensitive" } }, ...(q.includes("@") || q.includes(".") ? [{ email: { equals: q, mode: "insensitive" as const } }] : [])] } : { id: { not: me.id } }, orderBy: { lastActiveAt: "desc" }, select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true, lastActiveAt: true, settings: true }, take: q ? 25 : 100 }),
    db.group.findMany({ where: q ? { OR: [{ handle: { contains: q, mode: "insensitive" } }, { name: { contains: q, mode: "insensitive" } }] } : {}, select: { id: true, handle: true, name: true, description: true, avatarUrl: true }, take: 25 })
  ]);
  return NextResponse.json({ users: users.filter(u => u.id !== me.id).map(u => ({ ...u, online: !!u.settings?.showOnlineStatus && Date.now() - u.lastActiveAt.getTime() < 45000 })), groups });
}
