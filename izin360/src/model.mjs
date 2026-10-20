export const TABLES = [
  "employees",
  "leaves",
  "adjustments",
  "attendance",
  "payroll",
  "settings",
  "audit",
];
export const emptyData = () => Object.fromEntries(TABLES.map((k) => [k, {}]));
export const clone = (value) => JSON.parse(JSON.stringify(value));
export const id = () => {
  const b = crypto.getRandomValues(new Uint8Array(16));
  b[6] = (b[6] & 15) | 64;
  b[8] = (b[8] & 63) | 128;
  const h = Array.from(b, (n) => n.toString(16).padStart(2, "0")).join("");
  return [
    h.slice(0, 8),
    h.slice(8, 12),
    h.slice(12, 16),
    h.slice(16, 20),
    h.slice(20),
  ].join("-");
};
export function parseNumber(value, scale = 1000, { signed = false } = {}) {
  const s = String(value).trim().replace(",", ".");
  const precision = scale === 100 ? 2 : 3;
  if (
    !new RegExp(`^${signed ? "-?" : ""}\\d+(\\.\\d{1,${precision}})?$`).test(s)
  )
    throw Error(`Geçerli bir sayı gir (en fazla ${precision} ondalık).`);
  const [whole, fraction = ""] = s.split(".");
  const negative = s.startsWith("-");
  const n =
    (Math.abs(Number(whole)) * scale +
      Number(fraction.padEnd(precision, "0"))) *
    (negative ? -1 : 1);
  if (!Number.isSafeInteger(n) || Math.abs(n) > 1_000_000_000)
    throw Error("Miktar izin verilen aralığın dışında.");
  return n;
}
export const formatDays = (n) =>
  new Intl.NumberFormat("tr-TR", { maximumFractionDigits: 3 }).format(n / 1000);
export const formatMoney = (n) =>
  new Intl.NumberFormat("tr-TR", { style: "currency", currency: "TRY" }).format(
    n / 100,
  );
export const inputNumber = (n, scale = 1000) => String((n || 0) / scale);
export function date(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) throw Error("Tarihi kontrol et.");
  const d = new Date(value + "T12:00:00Z");
  if (!Number.isFinite(+d) || d.toISOString().slice(0, 10) !== value)
    throw Error("Tarihi kontrol et.");
  return d;
}
export const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
export const monthNow = () => today().slice(0, 7);
export const niceDate = (s) => {
  if (!s) return "—";
  const [y, m, d] = s.split("-");
  return `${d}.${m}.${y}`;
};
export function countDays(start, end, weekdaysOnly = false) {
  const a = date(start),
    b = date(end);
  if (b < a) throw Error("Bitiş tarihi başlangıçtan önce olamaz.");
  const days = (b - a) / 86400000 + 1;
  if (days > 3660) throw Error("İzin aralığı en fazla 10 yıl olabilir.");
  let count = 0;
  for (let d = new Date(a); d <= b; d.setUTCDate(d.getUTCDate() + 1))
    if (!weekdaysOnly || ![0, 6].includes(d.getUTCDay())) count++;
  return count * 1000;
}
export function balance(data, employee) {
  const recorded = Object.values(data.leaves)
    .filter((l) => l.employeeId === employee.id && l.deduct)
    .reduce((a, l) => a + l.days, 0);
  const adjustment = Object.values(data.adjustments)
    .filter((a) => a.employeeId === employee.id)
    .reduce((a, r) => a + r.amount, 0);
  const used = employee.openingUsed + recorded;
  return {
    recorded,
    used,
    adjustment,
    remaining: employee.entitlement + employee.carry - used + adjustment,
  };
}
export function validateData(d) {
  if (
    !d ||
    TABLES.some((k) => !d[k] || typeof d[k] !== "object" || Array.isArray(d[k]))
  )
    throw Error("Yedek yapısı geçersiz.");
  let count = 0;
  const string = (s, max = 3000) => typeof s === "string" && s.length <= max;
  const integer = (n, signed = false) =>
    Number.isSafeInteger(n) && Math.abs(n) <= 1e9 && (signed || n >= 0);
  for (const table of TABLES)
    for (const [key, r] of Object.entries(d[table])) {
      if (
        ++count > 50000 ||
        !r ||
        r.id !== key ||
        !string(key, 100) ||
        !key ||
        ["__proto__", "constructor", "prototype"].includes(key)
      )
        throw Error("Yedekteki kayıt kimliği geçersiz.");
      if (table === "employees") {
        if (
          !string(r.name, 160) ||
          !r.name.trim() ||
          !integer(r.entitlement) ||
          !integer(r.carry, true) ||
          !integer(r.openingUsed, true) ||
          typeof r.archived !== "boolean"
        )
          throw Error("Personel bilgileri geçersiz.");
        date(r.startDate);
        for (const k of ["department", "position", "phone", "note"])
          if (!string(r[k] || "")) throw Error("Personel alanı geçersiz.");
      } else if (table === "settings") {
        if (
          key !== "organization" ||
          !string(r.name, 200) ||
          !string(r.manager, 160)
        )
          throw Error("İşyeri ayarı geçersiz.");
      } else if (table === "audit") {
        if (
          !TABLES.includes(r.table) ||
          r.table === "audit" ||
          !string(r.entityId, 100) ||
          !string(r.title, 200) ||
          !string(r.at, 60) ||
          !Number.isFinite(Date.parse(r.at))
        )
          throw Error("Geçmiş kaydı geçersiz.");
      } else {
        if (!Object.prototype.hasOwnProperty.call(d.employees, r.employeeId))
          throw Error("Kayıt bir personele bağlı olmalı.");
        if (table === "leaves") {
          date(r.start);
          date(r.end);
          if (
            r.end < r.start ||
            !integer(r.days) ||
            r.days === 0 ||
            !string(r.kind, 100) ||
            !r.kind.trim() ||
            typeof r.deduct !== "boolean"
          )
            throw Error("İzin kaydı geçersiz.");
        }
        if (table === "adjustments") {
          if (
            !integer(r.amount, true) ||
            !string(r.reason, 1000) ||
            !r.reason.trim()
          )
            throw Error("Düzeltme açıklaması ve miktarı gerekli.");
          date(r.date);
        }
        if (table === "attendance") {
          date(r.date);
          if (
            !integer(r.minutes) ||
            !integer(r.overtime) ||
            ![
              "Çalıştı",
              "İzinli",
              "Raporlu",
              "Devamsız",
              "Hafta tatili",
              "Diğer",
            ].includes(r.status)
          )
            throw Error("Puantaj kaydı geçersiz.");
        }
        if (table === "payroll") {
          if (
            !/^\d{4}-(0[1-9]|1[0-2])$/.test(r.month) ||
            !integer(r.base) ||
            !integer(r.extra) ||
            !integer(r.deduction) ||
            !integer(r.paid)
          )
            throw Error("Maaş kaydı geçersiz.");
        }
        if (!string(r.note || "")) throw Error("Not çok uzun.");
      }
    }
  const seen = new Set();
  for (const [table, fields] of [
    ["attendance", ["employeeId", "date"]],
    ["payroll", ["employeeId", "month"]],
  ])
    for (const r of Object.values(d[table])) {
      const key = table + ":" + fields.map((f) => r[f]).join("|");
      if (seen.has(key))
        throw Error(
          table === "attendance"
            ? "Aynı personele aynı gün için ikinci puantaj kaydı eklenemez."
            : "Aynı personel ve ay için ikinci maaş kaydı eklenemez.",
        );
      seen.add(key);
    }
  return d;
}
export function applyChanges(
  data,
  changes,
  { title = "Kayıt düzenlendi", audit = true, device = "" } = {},
) {
  const next = clone(data);
  for (const c of changes) {
    if (!TABLES.includes(c.table) || c.table === "audit")
      throw Error("Geçersiz işlem.");
    const before = next[c.table][c.id] || null;
    if (c.value) next[c.table][c.id] = c.value;
    else delete next[c.table][c.id];
    if (audit) {
      const key = id();
      next.audit[key] = {
        id: key,
        table: c.table,
        entityId: c.id,
        before,
        after: c.value || null,
        title,
        at: new Date().toISOString(),
        device,
      };
    }
  }
  return validateData(next);
}
export const identical = (a, b) => stable(a) === stable(b);
function stable(v) {
  if (v === undefined || v === null) return "null";
  if (Array.isArray(v)) return "[" + v.map(stable).join(",") + "]";
  if (typeof v === "object")
    return (
      "{" +
      Object.keys(v)
        .sort()
        .map((k) => JSON.stringify(k) + ":" + stable(v[k]))
        .join(",") +
      "}"
    );
  return JSON.stringify(v);
}
export function diff(data, base) {
  const changes = [];
  for (const table of TABLES) {
    const ids = new Set([
      ...Object.keys(data[table]),
      ...Object.keys(base[table] || {}),
    ]);
    for (const id of ids) {
      const b = base[table]?.[id];
      const value = data[table][id] || null;
      if (!identical(value, b?.value || null))
        changes.push({ table, id, baseVersion: b?.version || 0, value });
    }
  }
  return changes;
}
export const blankBase = () => Object.fromEntries(TABLES.map((k) => [k, {}]));
export function receiveSync(state, response) {
  const pending = diff(state.data, state.base),
    base = blankBase(),
    data = emptyData();
  for (const r of response.records) {
    if (!TABLES.includes(r.table)) continue;
    base[r.table][r.id] = { version: r.version, value: r.value };
    if (r.value) data[r.table][r.id] = r.value;
  }
  for (const change of pending) {
    const remote = base[change.table][change.id]?.value || null;
    if (identical(change.value, remote)) continue;
    if (change.value) data[change.table][change.id] = change.value;
    else delete data[change.table][change.id];
  }
  const conflicts = (response.conflicts || []).map((c) => ({
    ...c,
    local: state.data[c.table][c.id] || null,
  }));
  // A rejected transaction is never retried with an advanced conflict version without a user choice.
  for (const c of conflicts)
    base[c.table][c.id] = state.base[c.table][c.id] || {
      version: 0,
      value: null,
    };
  return {
    ...state,
    data,
    base,
    conflicts,
    lastSync: response.ok ? new Date().toISOString() : state.lastSync,
  };
}
export function resolveConflict(state, conflict, useLocal) {
  const next = clone(state);
  next.base[conflict.table][conflict.id] = {
    version: conflict.version,
    value: conflict.remote,
  };
  if (!useLocal) {
    if (conflict.remote)
      next.data[conflict.table][conflict.id] = conflict.remote;
    else delete next.data[conflict.table][conflict.id];
  }
  next.conflicts = next.conflicts.filter(
    (c) => c.table !== conflict.table || c.id !== conflict.id,
  );
  return next;
}
export function csv(rows) {
  const quote = (v) =>
    '"' +
    (typeof v === "number"
      ? String(v).replace(".", ",")
      : String(v ?? "").replace(/^[\s]*[=+@\-\t\r]/, "'$&")
    ).replace(/"/g, '""') +
    '"';
  return "\uFEFF" + rows.map((row) => row.map(quote).join(";")).join("\r\n");
}
export function backup(data) {
  return JSON.stringify(
    { app: "izin360", version: 1, exportedAt: new Date().toISOString(), data },
    null,
    2,
  );
}
export function readBackup(text) {
  if (text.length > 10 * 1024 * 1024)
    throw Error("Yedek 10 MB sınırını aşıyor.");
  const b = JSON.parse(text);
  if (b.app !== "izin360" || b.version !== 1)
    throw Error("Bu dosya desteklenen bir İzin360 yedeği değil.");
  return validateData(b.data);
}
