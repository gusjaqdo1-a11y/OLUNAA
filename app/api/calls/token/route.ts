import { NextResponse } from "next/server";
import { AccessToken } from "livekit-server-sdk";
import { getCurrentUser } from "../../../../lib/auth";
import { isConversationMember } from "../../../../lib/realtime";

export const runtime = "nodejs";

export async function POST(req: Request) {
  const me = await getCurrentUser();
  if (!me) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const url = process.env.LIVEKIT_URL;
  const apiKey = process.env.LIVEKIT_API_KEY;
  const apiSecret = process.env.LIVEKIT_API_SECRET;
  if (!url || !apiKey || !apiSecret) {
    return NextResponse.json({ error: "Voice and video calling is not configured yet." }, { status: 503 });
  }
  const body = await req.json().catch(() => ({}));
  const conversationId = typeof body.conversationId === "string" ? body.conversationId : "";
  const mode = body.mode === "video" ? "video" : "audio";
  if (!conversationId || !(await isConversationMember(conversationId, me.id))) {
    return NextResponse.json({ error: "Conversation not found." }, { status: 403 });
  }
  try {
    const roomName = `olunaa-${conversationId}`;
    const token = new AccessToken(apiKey, apiSecret, {
      identity: me.id,
      name: me.displayName,
      ttl: "1h",
    });
    token.addGrant({
      roomJoin: true,
      room: roomName,
      canPublish: true,
      canSubscribe: true,
      canPublishData: true,
    });
    return NextResponse.json({ serverUrl: url, participantToken: await token.toJwt(), roomName, mode });
  } catch {
    return NextResponse.json({ error: "Could not create call credentials." }, { status: 502 });
  }
}
