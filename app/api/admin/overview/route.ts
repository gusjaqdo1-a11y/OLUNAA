import { NextResponse } from "next/server";
import { db } from "../../../../lib/db";
import { isAdmin } from "../../../../lib/admin";
export const runtime = "nodejs";

export async function GET() {
  if (!(await isAdmin())) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const since = new Date(Date.now() - 24 * 60 * 60 * 1000);
  const [users, active24h, conversations, messages, stories, groups, channels, notifications, sessions, recentUsers] = await Promise.all([
    db.user.count(),
    db.user.count({ where: { lastActiveAt: { gte: since } } }),
    db.conversation.count(),
    db.message.count(),
    db.story.count(),
    db.group.count(),
    db.channel.count(),
    db.notification.count(),
    db.session.count({ where: { expiresAt: { gt: new Date() } } }),
    db.user.findMany({ orderBy: { createdAt: "desc" }, take: 12, select: { id: true, username: true, displayName: true, email: true, avatarUrl: true, verified: true, lastActiveAt: true, createdAt: true } }),
  ]);
  return NextResponse.json({ stats: { users, active24h, conversations, messages, stories, groups, channels, notifications, sessions }, recentUsers });
}
