import { PrismaClient } from "@prisma/client";

declare global { var __olunaaPrisma: PrismaClient | undefined }
export const db = global.__olunaaPrisma ?? new PrismaClient();
if (process.env.NODE_ENV !== "production") global.__olunaaPrisma = db;
