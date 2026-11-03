import { cookies } from "next/headers";
import { createHmac, timingSafeEqual } from "node:crypto";

const COOKIE = "olunaa_admin";
const DAYS = 7;

function secret() {
  return process.env.ADMIN_SESSION_SECRET || process.env.ADMIN_PASSWORD || "change-me-in-vercel";
}
function signature(value: string) {
  return createHmac("sha256", secret()).update(value).digest("hex");
}
export async function createAdminSession(username: string) {
  const payload = Buffer.from(JSON.stringify({ u: username, exp: Date.now() + DAYS * 86400000 })).toString("base64url");
  const token = `${payload}.${signature(payload)}`;
  const jar = await cookies();
  jar.set(COOKIE, token, { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax", path: "/", maxAge: DAYS * 86400 });
}
export async function isAdmin() {
  const username = process.env.ADMIN_USERNAME;
  const jar = await cookies();
  const raw = jar.get(COOKIE)?.value;
  if (!username || !raw) return false;
  const [payload, sig] = raw.split(".");
  if (!payload || !sig) return false;
  const expected = signature(payload);
  if (sig.length !== expected.length || !timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) return false;
  try {
    const data = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    return data.u === username && Number(data.exp) > Date.now();
  } catch { return false; }
}
export async function destroyAdminSession() {
  const jar = await cookies();
  jar.set(COOKIE, "", { httpOnly: true, secure: process.env.NODE_ENV === "production", sameSite: "lax", path: "/", maxAge: 0 });
}
