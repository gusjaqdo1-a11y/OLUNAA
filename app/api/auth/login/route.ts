import { NextResponse } from "next/server";
import { createHash, timingSafeEqual } from "node:crypto";
import { randomBytes, scryptSync } from "node:crypto";
import { db } from "@/lib/db";

function verifyPassword(password: string, stored: string) {
  const [salt, hex] = stored.split(":");
  if (!salt || !hex) return false;
  const actual = scryptSync(password, salt, 64);
  const expected = Buffer.from(hex, "hex");
  return actual.length === expected.length && timingSafeEqual(actual, expected);
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const identifier = String(body.identifier ?? "").trim().toLowerCase().replace(/^@/, "");
    const password = String(body.password ?? "");
    if (!identifier || !password) return NextResponse.json({error:"Enter your username/email and password."},{status:400});

    const user = await db.user.findFirst({where:{OR:[{email:identifier},{username:identifier}]}});
    if (!user || !verifyPassword(password, user.passwordHash)) return NextResponse.json({error:"Invalid OLUNAA credentials."},{status:401});

    const token = createHash("sha256").update(randomBytes(32)).digest("hex");
    await db.session.create({data:{token,userId:user.id,expiresAt:new Date(Date.now()+1000*60*60*24*30)}});
    const response = NextResponse.json({user:{id:user.id,email:user.email,username:user.username,displayName:user.displayName}});
    response.cookies.set("olunaa_session",token,{httpOnly:true,secure:true,sameSite:"lax",path:"/",maxAge:60*60*24*30});
    return response;
  } catch {
    return NextResponse.json({error:"OLUNAA database is not configured yet."},{status:503});
  }
}
