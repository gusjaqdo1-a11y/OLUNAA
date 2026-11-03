const { spawnSync } = require("node:child_process");

function normalizeMongoUrl(raw) {
  if (!raw) throw new Error("MONGODB_URI is not configured");
  const match = raw.match(/^(mongodb(?:\+srv)?:\/\/[^/?]+)(\/[^?]*)?(\?.*)?$/);
  if (!match) return raw;
  const [, authority, path, query = ""] = match;
  if (path && path !== "/") return raw;
  return `${authority}/olunaa${query}`;
}

process.env.MONGODB_URI = normalizeMongoUrl(process.env.MONGODB_URI);

for (const args of [
  ["prisma", "db", "push", "--skip-generate"],
  ["prisma", "generate"],
]) {
  const result = spawnSync("npx", args, { stdio: "inherit", env: process.env });
  if (result.status !== 0) process.exit(result.status || 1);
}
