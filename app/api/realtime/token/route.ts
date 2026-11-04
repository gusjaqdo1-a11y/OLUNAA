import { NextResponse } from "next/server";
import Ably from "ably";
import { getCurrentUser } from "../../../../lib/auth";
import { isConversationMember } from "../../../../lib/realtime";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const key = process.env.ABLY_API_KEY;
  if (!key) return NextResponse.json({ error: "Realtime service is not configured yet." }, { status: 503 });
  const body = await req.json().catch(() => ({}));
  const conversationId = typeof body.conversationId === "string" ? body.conversationId : new URL(req.url).searchParams.get("conversationId") || "";
  if (!conversationId || !(await isConversationMember(conversationId, me.id))) {
    return NextResponse.json({ error: "Conversation not found." }, { status: 403 });
  }
  try {
    const client = new Ably.Rest({ key });
    const capability = {
      [`conversation:${conversationId}`]: ["subscribe", "publish", "presence"],
      [`user:${me.id}`]: ["subscribe"],
    };
    const tokenRequest = await client.auth.createTokenRequest({
      clientId: me.id,
      capability: JSON.stringify(capability),
      ttl: 60 * 60 * 1000,
    });
    return NextResponse.json(tokenRequest);
  } catch {
    return NextResponse.json({ error: "Could not create realtime credentials." }, { status: 502 });
  }
}
