import { NextResponse } from "next/server";
import { db } from "../../../lib/db";
import { getCurrentUser } from "../../../lib/auth";
export const runtime = "nodejs";
export async function GET(req: Request) {
  const me = await getCurrentUser();
  const handle = new URL(req.url).searchParams.get("handle");
  const channels = await db.channel.findMany({ orderBy: { updatedAt: "desc" }, take: 50 });
  if (!handle) return NextResponse.json({ channels });
  const channel = await db.channel.findUnique({ where: { handle }, include: { messages: { orderBy: { createdAt: "desc" }, take: 100, include: { author: { select: { id: true, username: true, displayName: true, avatarUrl: true, verified: true } } } } } });
  if (!channel) return NextResponse.json({ error: "Channel not found" }, { status: 404 });
  return NextResponse.json({ channel, authenticated: !!me });
}
