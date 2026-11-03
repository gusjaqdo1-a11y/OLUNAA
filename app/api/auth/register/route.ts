import { NextResponse } from "next/server";
import { randomBytes, scryptSync } from "node:crypto";
import { db } from "@/lib/db";

function hashPassword(password: string) {
  const salt = randomBytes(16).toString("hex");
  const hash = scryptSync(password, salt, 64).toString("hex");
  return `${salt}:${hash}`;
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const email = String(body.email ?? "").trim().toLowerCase();
    const username = String(body.username ?? "").trim().toLowerCase().replace(/^@/, "");
    const displayName = String(body.displayName ?? username).trim().slice(0, 60);
    const password = String(body.password ?? "");

    if (!/^\S+@\S+\.\S+$/.test(email)) return NextResponse.json({error:"Enter a valid email."},{status:400});
    if (!/^[a-z0-9_]{3,24}$/.test(username)) return NextResponse.json({error:"Username must be 3–24 letters, numbers or underscores."},{status:400});
    if (password.length < 8) return NextResponse.json({error:"Password must be at least 8 characters."},{status:400});

    const existing = await db.user.findFirst({where:{OR:[{email},{username}]},select:{email:true,username:true}});
    if (existing?.email === email) return NextResponse.json({error:"Email is already registered."},{status:409});
    if (existing?.username === username) return NextResponse.json({error:"Username is already taken."},{status:409});

    const user = await db.user.create({data:{email,username,displayName,passwordHash:hashPassword(password)},select:{id:true,email:true,username:true,displayName:true}});
    return NextResponse.json({user},{status:201});
  } catch {
    return NextResponse.json({error:"OLUNAA database is not configured yet."},{status:503});
  }
}
