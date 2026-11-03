import { cookies } from "next/headers";
import { randomBytes } from "node:crypto";
import { db } from "./db";

const COOKIE = "olunaa_session";
const DAYS = 30;

export async function createSession(userId: string) {
  const token = randomBytes(32).toString("hex");
  await db.session.create({ data: { token, userId, expiresAt: new Date(Date.now() + DAYS * 86400000) } });
  const jar = await cookies();
  jar.set(COOKIE, token, { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax", path: "/", maxAge: DAYS * 86400 });
  return token;
}

export async function getCurrentUser() {
  const jar = await cookies();
  const token = jar.get(COOKIE)?.value;
  if (!token) return null;
  const session = await db.session.findUnique({ where: { token }, include: { user: { include: { settings: true } } } });
  if (!session || session.expiresAt < new Date()) return null;
  await db.user.update({ where: { id: session.userId }, data: { lastActiveAt: new Date() } });
  return session.user;
}

export async function destroySession() {
  const jar = await cookies();
  const token = jar.get(COOKIE)?.value;
  if (token) await db.session.deleteMany({ where: { token } });
  jar.set(COOKIE, "", { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax", path: "/", maxAge: 0 });
}
