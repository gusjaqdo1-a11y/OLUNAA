import { NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function GET(req: Request) {
  const username = new URL(req.url).searchParams.get("username")?.toLowerCase().trim().replace(/^@/,"") ?? "";
  if (!/^[a-z0-9_]{3,24}$/.test(username)) return NextResponse.json({available:false,reason:"Use 3–24 letters, numbers or underscores."});
  const found = await db.user.findUnique({where:{username},select:{id:true}});
  return NextResponse.json({available:!found,username});
}
