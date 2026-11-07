import { GoogleAuth } from "google-auth-library";
import { db } from "./db";

type PushPayload = { title: string; body: string; data?: Record<string, string> };

function limitUtf8(value: string, maxBytes: number) {
  const encoder = new TextEncoder();
  let output = "";
  let used = 0;
  for (const character of value) {
    const size = encoder.encode(character).length;
    if (used + size > maxBytes) break;
    output += character;
    used += size;
  }
  return output;
}

function buildFcmData(payload: PushPayload) {
  const data: Record<string, string> = {};
  for (const [key, value] of Object.entries(payload.data || {})) {
    if (typeof value !== "string") continue;
    const limit = key === "senderAvatarUrl" ? 900 : 500;
    data[key] = limitUtf8(value, limit);
  }
  data.title = limitUtf8(payload.title || "OLUNAA", 180);
  data.body = limitUtf8(payload.body || "You have a new notification", 700);
  return data;
}

export async function sendPushToUsers(userIds: string[], payload: PushPayload) {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw || !userIds.length) return { sent: 0, configured: false };
  try {
    const credentials = JSON.parse(raw);
    const projectId = process.env.FIREBASE_PROJECT_ID || credentials.project_id;
    if (!projectId) return { sent: 0, configured: false };
    const auth = new GoogleAuth({ credentials, scopes: ["https://www.googleapis.com/auth/firebase.messaging"] });
    const client = await auth.getClient();
    const access = await client.getAccessToken();
    const accessToken = typeof access === "string" ? access : access?.token;
    if (!accessToken) return { sent: 0, configured: false };
    const devices = await db.deviceToken.findMany({ where: { userId: { in: userIds } }, select: { id: true, token: true } });
    const results = await Promise.allSettled(devices.map(async device => {
      const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ message: {
          token: device.token,
          // Data-only messages let Android build a rich notification with the sender's avatar.
          data: buildFcmData(payload),
          android: { priority: "high" },
        } }),
      });
      if (!response.ok) throw new Error(`FCM ${response.status}: ${(await response.text()).slice(0, 250)}`);
      return true;
    }));
    return { sent: results.filter(x => x.status === "fulfilled").length, configured: true };
  } catch (error) {
    console.error("FCM delivery failed", error);
    return { sent: 0, configured: true };
  }
}
