// Reference controllers, not a deployed service. See ARCHITECTURE.md for bootstrap/auth policy.
import { Router } from "express";
import { createHash } from "node:crypto";
import * as argon2 from "argon2";
import { jwtVerify } from "jose";
import { z } from "zod";

const date = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/)
  .refine((value) => {
    const parsed = new Date(`${value}T12:00:00Z`);
    return (
      value >= "2000-01-01" &&
      value <= "2100-12-31" &&
      !Number.isNaN(parsed.getTime()) &&
      parsed.toISOString().slice(0, 10) === value
    );
  });
const amount = z
  .string()
  .regex(/^[1-9]\d*$/)
  .refine((value) => BigInt(value) <= 100_000_000_000n);
const transaction = z
  .object({
    accountId: z.uuid(),
    categoryId: z.uuid(),
    kind: z.enum(["income", "expense"]),
    amountMinor: amount,
    currency: z.string().regex(/^[A-Z]{3}$/),
    occurredOn: date,
    note: z.string().trim().max(1000).default(""),
  })
  .strict();
const authBody = z
  .object({
    email: z.string().trim().toLowerCase().email().max(254),
    password: z.string().min(12).max(128),
  })
  .strict();
const failure = (status, message) =>
  Object.assign(new Error(message), { status });

export function requireAuth(publicKey, issuer, audience) {
  return async (req, res, next) => {
    try {
      const match = /^Bearer ([^ ]+)$/.exec(req.headers.authorization ?? "");
      if (!match) throw failure(401, "Oturum gerekli.");
      const { payload } = await jwtVerify(match[1], publicKey, {
        algorithms: ["RS256"],
        issuer,
        audience,
      });
      req.userId = z.uuid().parse(payload.sub);
      next();
    } catch {
      res.status(401).json({ error: "Oturum geçersiz veya süresi doldu." });
    }
  };
}

// signAccess(userId) must issue a 5–10 minute JWT using a private key from secret storage.
// dummyPasswordHash is one precomputed Argon2id hash for constant-work unknown-email login.
export function createRouter({ pool, auth, signAccess, dummyPasswordHash }) {
  const router = Router();
  router.post("/auth/register", async (req, res) => {
    const body = authBody.parse(req.body);
    const passwordHash = await argon2.hash(body.password, {
      type: argon2.argon2id,
      memoryCost: 65536,
      timeCost: 3,
      parallelism: 1,
    });
    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      const {
        rows: [user],
      } = await client.query(
        "INSERT INTO users(email,password_hash) VALUES($1,$2) RETURNING id",
        [body.email, passwordHash],
      );
      await client.query(
        "INSERT INTO accounts(user_id,name) VALUES($1,'Nakit')",
        [user.id],
      );
      await client.query(
        "INSERT INTO categories(user_id,name,kind) VALUES($1,'Market','expense'),($1,'Maaş','income')",
        [user.id],
      );
      await client.query("COMMIT");
      res.status(201).json({ id: user.id });
    } catch (error) {
      await client.query("ROLLBACK");
      if (error.code === "23505") throw failure(409, "Kayıt tamamlanamadı.");
      throw error;
    } finally {
      client.release();
    }
  });
  router.post("/auth/login", async (req, res) => {
    const body = authBody.parse(req.body);
    const {
      rows: [user],
    } = await pool.query("SELECT id,password_hash FROM users WHERE email=$1", [
      body.email,
    ]);
    const valid = await argon2.verify(
      user?.password_hash ?? dummyPasswordHash,
      body.password,
    );
    if (!user || !valid) throw failure(401, "E-posta veya şifre hatalı.");
    res.json({
      accessToken: await signAccess(user.id),
      tokenType: "Bearer",
      expiresIn: 600,
    });
  });
  router.use(auth);
  router.post("/transactions", async (req, res) => {
    const input = transaction.parse(req.body),
      key = z.uuid().parse(req.get("Idempotency-Key"));
    const hash = createHash("sha256")
      .update(
        JSON.stringify([
          input.accountId,
          input.categoryId,
          input.kind,
          input.amountMinor,
          input.currency,
          input.occurredOn,
          input.note,
        ]),
      )
      .digest("hex");
    const { rows } = await pool.query(
      `INSERT INTO transactions(user_id,account_id,category_id,kind,amount_minor,currency,occurred_on,note,idempotency_key,request_hash)
      SELECT $1,a.id,c.id,$4,$5,$6,$7,$8,$9,$10 FROM accounts a JOIN categories c ON c.user_id=a.user_id
      WHERE a.id=$2 AND c.id=$3 AND a.user_id=$1 AND a.currency=$6 AND c.kind=$4 AND a.archived_at IS NULL AND c.archived_at IS NULL
      ON CONFLICT(user_id,idempotency_key) DO NOTHING RETURNING *`,
      [
        req.userId,
        input.accountId,
        input.categoryId,
        input.kind,
        input.amountMinor,
        input.currency,
        input.occurredOn,
        input.note,
        key,
        hash,
      ],
    );
    if (rows.length) {
      res.status(201).json(rows[0]);
      return;
    }
    const {
      rows: [existing],
    } = await pool.query(
      "SELECT * FROM transactions WHERE user_id=$1 AND idempotency_key=$2",
      [req.userId, key],
    );
    if (!existing)
      throw failure(422, "Hesap/kategori türü veya para birimi uyumsuz.");
    if (existing.request_hash !== hash || existing.deleted_at)
      throw failure(409, "Bu istek anahtarı başka bir işlem için kullanıldı.");
    res.status(200).json(existing);
  });
  router.get("/transactions", async (req, res) => {
    const query = z
      .object({
        from: date,
        to: date,
        categoryId: z.uuid().optional(),
        limit: z.coerce.number().int().min(1).max(100).default(50),
        cursorDate: date.optional(),
        cursorId: z.uuid().optional(),
      })
      .strict()
      .parse(req.query);
    if (
      query.from > query.to ||
      Boolean(query.cursorDate) !== Boolean(query.cursorId)
    )
      throw failure(422, "Geçersiz aralık veya imleç.");
    const { rows } = await pool.query(
      `SELECT * FROM transactions WHERE user_id=$1 AND deleted_at IS NULL AND occurred_on BETWEEN $2 AND $3
      AND ($4::uuid IS NULL OR category_id=$4) AND ($5::date IS NULL OR (occurred_on,id)<($5::date,$6::uuid))
      ORDER BY occurred_on DESC,id DESC LIMIT $7`,
      [
        req.userId,
        query.from,
        query.to,
        query.categoryId ?? null,
        query.cursorDate ?? null,
        query.cursorId ?? null,
        query.limit,
      ],
    );
    res.json({
      items: rows,
      nextCursor:
        rows.length === query.limit
          ? { date: rows.at(-1).occurred_on, id: rows.at(-1).id }
          : null,
    });
  });
  router.put("/transactions/:id", async (req, res) => {
    const id = z.uuid().parse(req.params.id),
      input = transaction
        .extend({ version: z.string().regex(/^[1-9]\d*$/) })
        .parse(req.body);
    const { rows } = await pool.query(
      `UPDATE transactions t SET account_id=$3,category_id=$4,kind=$5,amount_minor=$6,currency=$7,occurred_on=$8,note=$9,version=version+1,updated_at=now()
      WHERE t.id=$1 AND t.user_id=$2 AND t.version=$10 AND t.deleted_at IS NULL
      AND EXISTS(SELECT 1 FROM accounts a WHERE a.id=$3 AND a.user_id=$2 AND a.currency=$7 AND a.archived_at IS NULL)
      AND EXISTS(SELECT 1 FROM categories c WHERE c.id=$4 AND c.user_id=$2 AND c.kind=$5 AND c.archived_at IS NULL) RETURNING t.*`,
      [
        id,
        req.userId,
        input.accountId,
        input.categoryId,
        input.kind,
        input.amountMinor,
        input.currency,
        input.occurredOn,
        input.note,
        input.version,
      ],
    );
    if (!rows.length)
      throw failure(
        409,
        "Kayıt değişti veya referanslar geçersiz; listeyi yenile.",
      );
    res.json(rows[0]);
  });
  router.delete("/transactions/:id", async (req, res) => {
    const id = z.uuid().parse(req.params.id),
      version = z
        .string()
        .regex(/^[1-9]\d*$/)
        .parse(req.get("If-Match"));
    const { rowCount } = await pool.query(
      "UPDATE transactions SET deleted_at=now(),version=version+1,updated_at=now() WHERE id=$1 AND user_id=$2 AND version=$3 AND deleted_at IS NULL",
      [id, req.userId, version],
    );
    if (!rowCount) throw failure(409, "Kayıt değişti; listeyi yenile.");
    res.status(204).end();
  });
  return router;
}
