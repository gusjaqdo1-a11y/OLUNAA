import { PrismaClient } from "@prisma/client";

declare global { var __olunaaPrisma: PrismaClient | undefined; }

function normalizeMongoUrl(raw: string | undefined) {
  if (!raw) throw new Error("MONGODB_URI is not configured");
  const match = raw.match(/^(mongodb(?:\+srv)?:\/\/[^/?]+)(\/[^?]*)?(\?.*)?$/);
  if (!match) return raw;
  const [, authority, path, query = ""] = match;
  if (path && path !== "/") return raw;
  return `${authority}/olunaa${query}`;
}

const mongoUrl = normalizeMongoUrl(process.env.MONGODB_URI);

export const db = globalThis.__olunaaPrisma ?? new PrismaClient({
  datasources: { db: { url: mongoUrl } },
});

if (process.env.NODE_ENV !== "production") globalThis.__olunaaPrisma = db;
