import { NextResponse } from "next/server";
import { destroyAdminSession } from "../../../../../lib/admin";
export async function POST() { await destroyAdminSession(); return NextResponse.json({ ok: true }); }
