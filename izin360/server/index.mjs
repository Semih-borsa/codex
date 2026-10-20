import http from "node:http";
import { DatabaseSync } from "node:sqlite";
import { randomBytes, timingSafeEqual } from "node:crypto";
import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { resolve, dirname, extname } from "node:path";
import { fileURLToPath } from "node:url";
import { networkInterfaces } from "node:os";
import { TABLES, emptyData, validateData, identical } from "../src/model.mjs";
const here = dirname(fileURLToPath(import.meta.url));
const eq = (a, b) =>
  typeof a === "string" &&
  typeof b === "string" &&
  Buffer.byteLength(a) === Buffer.byteLength(b) &&
  timingSafeEqual(Buffer.from(a), Buffer.from(b));
const loopback = (ip) =>
  ip === "127.0.0.1" || ip === "::1" || ip === "::ffff:127.0.0.1";
export function createApp({
  directory = process.env.IZIN360_DATA_DIR || resolve(here, "../data"),
  dist = resolve(here, "../dist"),
  token: provided,
} = {}) {
  mkdirSync(directory, { recursive: true, mode: 0o700 });
  const tokenFile = resolve(directory, "connection.key");
  const token =
    provided ||
    (existsSync(tokenFile)
      ? readFileSync(tokenFile, "utf8").trim()
      : randomBytes(32).toString("base64url"));
  if (!provided && !/^[A-Za-z0-9_-]{40,100}$/.test(token))
    throw Error("Bağlantı anahtarı dosyası bozuk. Kayıtlar korunuyor; connection.key dosyasını yedekleyip yeniden adlandır ve programı yeniden başlat.");
  if (!provided && !existsSync(tokenFile))
    writeFileSync(tokenFile, token, { mode: 0o600 });
  const session = randomBytes(32).toString("base64url");
  const db = new DatabaseSync(resolve(directory, "izin360.sqlite"));
  db.exec(
    "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000; CREATE TABLE IF NOT EXISTS records (collection TEXT NOT NULL, id TEXT NOT NULL, version INTEGER NOT NULL, value TEXT, PRIMARY KEY(collection,id)); CREATE TABLE IF NOT EXISTS meta (id INTEGER PRIMARY KEY,revision INTEGER NOT NULL); INSERT OR IGNORE INTO meta VALUES(1,0);",
  );
  const all = () =>
    db
      .prepare('SELECT collection AS "table",id,version,value FROM records')
      .all()
      .map((r) => ({
        ...r,
        value: r.value === null ? null : JSON.parse(r.value),
      }));
  const revision = () =>
    db.prepare("SELECT revision FROM meta WHERE id=1").get().revision;
  const rate = new Map();
  const server = http.createServer(async (req, res) => {
    const security = {
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
      "X-Frame-Options": "DENY",
      "Cache-Control": "no-store",
    };
    const reply = (status, body) => {
      res.writeHead(status, {
        ...security,
        "Content-Type": "application/json; charset=utf-8",
      });
      res.end(JSON.stringify(body));
    };
    let url;
    try {
      url = new URL(req.url, "http://localhost");
    } catch {
      return reply(400, { error: "Geçersiz adres." });
    }
    const origin = req.headers.origin;
    const allowed = new Set(
      [
        "https://app.izin360.local",
        `http://${req.headers.host}`,
        `https://${req.headers.host}`,
        process.env.IZIN360_PUBLIC_ORIGIN,
      ].filter(Boolean),
    );
    if (loopback(req.socket.remoteAddress))
      allowed.add("http://localhost:5173");
    if (origin && !allowed.has(origin))
      return reply(403, { error: "Bu kaynaktan erişime izin verilmiyor." });
    if (origin) {
      res.setHeader("Access-Control-Allow-Origin", origin);
      res.setHeader("Vary", "Origin");
      res.setHeader("Access-Control-Allow-Credentials", "true");
    }
    if (req.method === "OPTIONS") {
      res.writeHead(204, {
        "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
        "Access-Control-Allow-Headers": "Authorization,Content-Type",
        "Access-Control-Allow-Private-Network": "true",
      });
      return res.end();
    }
    try {
      if (url.pathname === "/api/health")
        return reply(200, { app: "izin360", version: "1.0.0" });
      if (url.pathname.startsWith("/api/")) {
        const cookie = (req.headers.cookie || "")
          .split(";")
          .map((s) => s.trim())
          .find((s) => s.startsWith("izin360_session="))
          ?.slice(16);
        const local = loopback(req.socket.remoteAddress) && eq(cookie, session);
        const bearer = req.headers.authorization?.startsWith("Bearer ")
          ? req.headers.authorization.slice(7)
          : "";
        if (!local && !eq(bearer, token)) {
          const ip = req.socket.remoteAddress,
            now = Date.now();
          const previous = rate.get(ip) || { at: now, n: 0 };
          if (now - previous.at > 60000) {
            previous.at = now;
            previous.n = 0;
          }
          previous.n++;
          rate.set(ip, previous);
          if (rate.size > 1000) rate.delete(rate.keys().next().value);
          return reply(previous.n > 20 ? 429 : 401, {
            error:
              previous.n > 20
                ? "Çok fazla deneme. Bir dakika sonra tekrar dene."
                : "Bağlantı anahtarı geçersiz. Ayarlardan kontrol et.",
          });
        }
        if (url.pathname === "/api/pairing" && req.method === "GET") {
          if (!local)
            return reply(403, {
              error:
                "Bağlantı anahtarı yalnızca sunucunun çalıştığı bilgisayarda gösterilir.",
            });
          const port = server.address().port;
          const addresses = Object.values(networkInterfaces())
            .flat()
            .filter((a) => a.family === "IPv4" && !a.internal)
            .map((a) => `http://${a.address}:${port}`);
          return reply(200, { addresses, token });
        }
        if (url.pathname === "/api/shutdown" && req.method === "POST") {
          if (!loopback(req.socket.remoteAddress))
            return reply(403, {
              error: "Sunucu yalnızca kendi bilgisayarından kapatılabilir.",
            });
          reply(200, { ok: true });
          setTimeout(() => close(), 100);
          return;
        }
        if (url.pathname !== "/api/sync" || req.method !== "POST")
          return reply(404, { error: "İşlem bulunamadı." });
        if (!(req.headers["content-type"] || "").includes("application/json"))
          return reply(415, { error: "JSON gerekli." });
        let size = 0;
        const chunks = [];
        for await (const chunk of req) {
          size += chunk.length;
          if (size > 10 * 1024 * 1024)
            return reply(413, { error: "Veri boyutu 10 MB sınırını aşıyor." });
          chunks.push(chunk);
        }
        let body;
        try {
          body = JSON.parse(Buffer.concat(chunks).toString("utf8"));
        } catch {
          return reply(400, { error: "JSON okunamadı." });
        }
        if (!Array.isArray(body.changes) || body.changes.length > 50000)
          return reply(400, { error: "Değişiklikler geçersiz." });
        const seen = new Set();
        for (const c of body.changes) {
          if (
            !TABLES.includes(c.table) ||
            typeof c.id !== "string" ||
            !c.id ||
            ["__proto__", "constructor", "prototype"].includes(c.id) ||
            c.id.length > 100 ||
            !Number.isSafeInteger(c.baseVersion) ||
            c.baseVersion < 0 ||
            !Object.hasOwn(c, "value") ||
            (c.value !== null && c.value.id !== c.id)
          )
            return reply(400, { error: "Kayıt biçimi geçersiz." });
          const k = c.table + ":" + c.id;
          if (seen.has(k)) return reply(400, { error: "Yinelenen işlem." });
          seen.add(k);
        }
        db.exec("BEGIN IMMEDIATE");
        try {
          const records = all(),
            lookup = new Map(records.map((r) => [r.table + ":" + r.id, r]));
          const conflicts = [];
          for (const c of body.changes) {
            const r = lookup.get(c.table + ":" + c.id);
            if (
              (r?.version || 0) !== c.baseVersion &&
              !identical(r?.value || null, c.value)
            )
              conflicts.push({
                table: c.table,
                id: c.id,
                version: r?.version || 0,
                remote: r?.value || null,
              });
          }
          if (conflicts.length) {
            db.exec("ROLLBACK");
            return reply(409, {
              ok: false,
              revision: revision(),
              records,
              conflicts,
            });
          }
          const next = emptyData();
          for (const r of records) if (r.value) next[r.table][r.id] = r.value;
          for (const c of body.changes) {
            if (c.value) next[c.table][c.id] = c.value;
            else delete next[c.table][c.id];
          }
          validateData(next);
          const effective = body.changes.filter(
            (c) =>
              !identical(
                lookup.get(c.table + ":" + c.id)?.value || null,
                c.value,
              ),
          );
          if (effective.length) {
            const rev = revision() + 1;
            const set = db.prepare(
              "INSERT INTO records(collection,id,version,value) VALUES(?,?,?,?) ON CONFLICT(collection,id) DO UPDATE SET version=excluded.version,value=excluded.value",
            );
            for (const c of effective)
              set.run(
                c.table,
                c.id,
                rev,
                c.value === null ? null : JSON.stringify(c.value),
              );
            db.prepare("UPDATE meta SET revision=? WHERE id=1").run(rev);
          }
          db.exec("COMMIT");
          return reply(200, {
            ok: true,
            revision: revision(),
            records: all(),
            conflicts: [],
          });
        } catch (e) {
          db.exec("ROLLBACK");
          return reply(422, { error: e.message || "Kayıtlar doğrulanamadı." });
        }
      }
      if (req.method !== "GET" && req.method !== "HEAD")
        return reply(405, { error: "Yöntem desteklenmiyor." });
      let path = decodeURIComponent(url.pathname);
      const target = resolve(dist, "." + path);
      if (!target.startsWith(resolve(dist) + "/") && target !== resolve(dist))
        return reply(403, { error: "Erişim reddedildi." });
      let file = target;
      if (path === "/" || !extname(path)) file = resolve(dist, "index.html");
      if (!existsSync(file))
        return reply(404, {
          error: "Dosya bulunamadı. Önce npm run build çalıştır.",
        });
      const mime =
        {
          ".html": "text/html; charset=utf-8",
          ".js": "text/javascript; charset=utf-8",
          ".css": "text/css; charset=utf-8",
          ".svg": "image/svg+xml",
          ".webmanifest": "application/manifest+json",
          ".png": "image/png",
        }[extname(file)] || "application/octet-stream";
      const headers = {
        ...security,
        "Content-Type": mime,
        "Content-Security-Policy":
          "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' https: http:; object-src 'none'; frame-ancestors 'none'",
      };
      if (
        path === "/" &&
        loopback(req.socket.remoteAddress) &&
        /^(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?$/.test(
          req.headers.host || "",
        )
      )
        headers["Set-Cookie"] =
          `izin360_session=${session}; HttpOnly; SameSite=Strict; Path=/`;
      res.writeHead(200, headers);
      res.end(req.method === "HEAD" ? undefined : readFileSync(file));
    } catch {
      return reply(500, {
        error: "İşlem tamamlanamadı. Sunucu ve kayıt dizinini kontrol et.",
      });
    }
  });
  let closing;
  const close = () =>
    closing ||
    (closing = new Promise((resolve) =>
      server.close(() => {
        db.close();
        resolve();
      }),
    ));
  return { server, close };
}
if (
  process.argv[1] &&
  resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  const app = createApp();
  const port = Number(process.env.PORT || 4310);
  app.server.listen(port, process.env.HOST || "0.0.0.0", () =>
    console.log(
      `İzin360 hazır: http://localhost:${port}\nTelefon bağlantısı: Ayarlar → Windows bağlantısı. Bağlantı anahtarını kimseyle paylaşma.`,
    ),
  );
  app.server.on("error", (e) => {
    console.error(
      e.code === "EADDRINUSE"
        ? "4310 bağlantı noktası kullanımda. Açık İzin360 penceresini kontrol et."
        : "Sunucu başlatılamadı.",
    );
    process.exitCode = 1;
  });
  for (const signal of ["SIGINT", "SIGTERM"])
    process.on(signal, () => app.close().then(() => process.exit(0)));
}
