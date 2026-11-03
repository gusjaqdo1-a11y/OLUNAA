import { PrismaClient } from "@prisma/client";

declare global { var __olunaaPrisma: PrismaClient | undefined; }
export const db = globalThis.__olunaaPrisma ?? new PrismaClient();
if (process.env.NODE_ENV !== "production") globalThis.__olunaaPrisma = db;
