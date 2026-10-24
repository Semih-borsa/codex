import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, rmSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createApp } from "../server/index.mjs";
const emp = (id) => ({
  id,
  name: "Deneme Personeli",
  startDate: "2026-10-01",
  entitlement: 0,
  carry: 0,
  openingUsed: 0,
  archived: false,
});
async function fixture(t) {
  const dir = mkdtempSync(join(tmpdir(), "izin360-server-"));
  writeFileSync(join(dir, "index.html"), "<h1>İzin360</h1>");
  const app = createApp({
    directory: dir,
    dist: dir,
    token: "synthetic-test-token-not-a-real-secret",
  });
  await new Promise((r) => app.server.listen(0, "127.0.0.1", r));
  t.after(async () => {
    await app.close();
    rmSync(dir, { recursive: true, force: true });
  });
  const root = `http://127.0.0.1:${app.server.address().port}`;
  const call = async (changes, auth = true) => {
    const res = await fetch(root + "/api/sync", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(auth
          ? { Authorization: "Bearer synthetic-test-token-not-a-real-secret" }
          : {}),
      },
      body: JSON.stringify({ changes }),
    });
    return { status: res.status, body: await res.json() };
  };
  return { root, call, dir };
}
test("API authentication required, health contains no records", async (t) => {
  const f = await fixture(t);
  assert.equal((await f.call([], false)).status, 401);
  const health = await (await fetch(f.root + "/api/health")).json();
  assert.equal(health.app, "izin360");
  assert.equal(health.token, undefined);
});
test("two devices share data and stale updates are rejected atomically", async (t) => {
  const f = await fixture(t),
    e = emp("p1");
  const first = await f.call([
    { table: "employees", id: e.id, baseVersion: 0, value: e },
  ]);
  assert.equal(first.status, 200);
  assert.equal(first.body.revision, 1);
  assert.equal((await f.call([])).body.records[0].value.entitlement, 0);
  const edit = await f.call([
    {
      table: "employees",
      id: e.id,
      baseVersion: 1,
      value: { ...e, name: "Windows yeni adı" },
    },
  ]);
  assert.equal(edit.status, 200);
  const stale = await f.call([
    {
      table: "employees",
      id: e.id,
      baseVersion: 1,
      value: { ...e, name: "Telefon eski sürümü" },
    },
    { table: "employees", id: "p2", baseVersion: 0, value: emp("p2") },
  ]);
  assert.equal(stale.status, 409);
  assert.equal(stale.body.conflicts.length, 1);
  const latest = await f.call([]);
  assert.equal(latest.body.records.length, 1);
  assert.equal(latest.body.records[0].value.name, "Windows yeni adı");
});
test("identical retries do not create duplicate employee records", async (t) => {
  const f = await fixture(t),
    change = { table: "employees", id: "p1", baseVersion: 0, value: emp("p1") };
  assert.equal((await f.call([change])).status, 200);
  assert.equal((await f.call([change])).status, 200);
  assert.equal((await f.call([])).body.records.length, 1);
});
test("invalid batches are rolled back and archive preserves related records", async (t) => {
  const f = await fixture(t),
    e = emp("p1");
  await f.call([{ table: "employees", id: "p1", baseVersion: 0, value: e }]);
  assert.equal(
    (
      await f.call([
        {
          table: "adjustments",
          id: "a",
          baseVersion: 0,
          value: {
            id: "a",
            employeeId: "missing",
            amount: 1000,
            date: "2026-10-20",
            reason: "Test",
          },
        },
      ])
    ).status,
    422,
  );
  assert.equal((await f.call([])).body.records.length, 1);
  const archive = await f.call([
    {
      table: "employees",
      id: "p1",
      baseVersion: 1,
      value: { ...e, archived: true },
    },
  ]);
  assert.equal(archive.status, 200);
  assert.equal(archive.body.records[0].value.archived, true);
});
test("pairing token requires local browser session and cross-origin writes are rejected", async (t) => {
  const f = await fixture(t);
  const denied = await fetch(f.root + "/api/pairing", {
    headers: { Authorization: "Bearer synthetic-test-token-not-a-real-secret" },
  });
  assert.equal(denied.status, 403);
  const home = await fetch(f.root + "/");
  const cookie = home.headers.get("set-cookie").split(";")[0];
  assert.ok(home.headers.get("content-security-policy"));
  const pairing = await fetch(f.root + "/api/pairing", {
    headers: { Cookie: cookie },
  });
  assert.equal(pairing.status, 200);
  assert.equal(typeof (await pairing.json()).token, "string");
  const csrf = await fetch(f.root + "/api/sync", {
    method: "POST",
    headers: {
      Cookie: cookie,
      Origin: "https://untrusted.example",
      "Content-Type": "application/json",
    },
    body: '{"changes":[]}',
  });
  assert.equal(csrf.status, 403);
});
test("tombstone versions prevent a deleted record from being silently resurrected", async (t) => {
  const f = await fixture(t),
    e = emp("p1");
  await f.call([{ table: "employees", id: "p1", baseVersion: 0, value: e }]);
  const del = await f.call([
    { table: "employees", id: "p1", baseVersion: 1, value: null },
  ]);
  assert.equal(del.body.records[0].value, null);
  const stale = await f.call([
    {
      table: "employees",
      id: "p1",
      baseVersion: 1,
      value: { ...e, name: "Old name" },
    },
  ]);
  assert.equal(stale.status, 409);
});

test('corrupt empty authentication key prevents insecure server startup',()=>{
 const dir=mkdtempSync(join(tmpdir(),'izin360-invalid-key-'));
 try{writeFileSync(join(dir,'connection.key'),'');assert.throws(()=>createApp({directory:dir}),/anahtarı dosyası bozuk/);}
 finally{rmSync(dir,{recursive:true,force:true});}
});


test("startup serves JS/CSS with correct MIME and rejects decoded traversal", async (t) => {
  const { root, dir } = await fixture(t);
  mkdirSync(join(dir, "assets"));
  writeFileSync(join(dir, "assets", "app.js"), "window.ready = true;");
  writeFileSync(join(dir, "assets", "app.css"), "body{color:navy}");
  for (const [url, mime, content] of [
    ["/assets/app.js", "text/javascript", "window.ready = true;"],
    ["/assets/app.css", "text/css", "body{color:navy}"],
  ]) {
    const response = await fetch(root + url);
    assert.equal(response.status, 200);
    assert.ok(response.headers.get("content-type").startsWith(mime));
    assert.equal(await response.text(), content);
  }
  assert.equal((await fetch(root + "/..%2fconnection.key")).status, 403);
});
