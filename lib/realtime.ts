import Ably from "ably";
import { db } from "./db";

export async function publishConversationEvent(conversationId: string, name: string, data: unknown) {
  const key = process.env.ABLY_API_KEY;
  if (!key) return false;
  const client = new Ably.Rest({ key });
  await client.channels.get(`conversation:${conversationId}`).publish(name, data);
  return true;
}

export async function publishUserEvent(userId: string, name: string, data: unknown) {
  const key = process.env.ABLY_API_KEY;
  if (!key) return false;
  const client = new Ably.Rest({ key });
  await client.channels.get(`user:${userId}`).publish(name, data);
  return true;
}

export async function isConversationMember(conversationId: string, userId: string) {
  return !!(await db.conversationMember.findUnique({
    where: { conversationId_userId: { conversationId, userId } },
    select: { id: true },
  }));
}
