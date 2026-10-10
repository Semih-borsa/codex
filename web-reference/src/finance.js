export function todayISO() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}
export function parseMinor(input) {
  const trimmed = input.trim().replaceAll(" ", "");
  const text = trimmed.includes(",")
    ? trimmed.replaceAll(".", "").replace(",", ".")
    : trimmed;
  const match = /^(\d+)(?:\.(\d{1,2}))?$/.exec(text);
  if (!match) throw new Error("Tutarı 1234,56 biçiminde gir.");
  const result =
    BigInt(match[1]) * 100n + BigInt((match[2] ?? "").padEnd(2, "0"));
  if (result <= 0n || result > 100_000_000_000n)
    throw new Error("Tutar 0’dan büyük, en fazla 1 milyar TL olmalı.");
  return result.toString();
}
export function money(value) {
  let minor = BigInt(value),
    prefix = minor < 0n ? "−" : "";
  if (minor < 0n) minor = -minor;
  return `${prefix}₺${(minor / 100n).toLocaleString("tr-TR")},${(minor % 100n).toString().padStart(2, "0")}`;
}
export function isDate(value) {
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(value) ||
    value < "2000-01-01" ||
    value > "2100-12-31"
  )
    return false;
  const date = new Date(`${value}T12:00:00Z`);
  return (
    !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
  );
}
function dayAt(year, month, day) {
  const end = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  return new Date(Date.UTC(year, month, Math.min(day, end)));
}
export function salaryPeriod(date, payday = 1) {
  if (!isDate(date) || !Number.isInteger(payday) || payday < 1 || payday > 31)
    throw new Error("Geçersiz dönem.");
  const [year, month] = date.split("-").map(Number);
  let start = dayAt(year, month - 1, payday);
  if (start.toISOString().slice(0, 10) > date)
    start = dayAt(year, month - 2, payday);
  const next = dayAt(start.getUTCFullYear(), start.getUTCMonth() + 1, payday);
  next.setUTCDate(next.getUTCDate() - 1);
  return {
    from: start.toISOString().slice(0, 10),
    to: next.toISOString().slice(0, 10),
  };
}
export function summarize(transactions, from, to, today) {
  let income = 0n,
    expense = 0n;
  const end = to < today ? to : today;
  for (const t of transactions) {
    if (t.occurredOn < from || t.occurredOn > end) continue;
    if (t.kind === "income") income += BigInt(t.amountMinor);
    else if (t.kind === "expense") expense += BigInt(t.amountMinor);
  }
  const ratio =
    income > 0n ? Number((expense * 1000n + income / 2n) / income) / 10 : null;
  return { income, expense, net: income - expense, ratio };
}
export function band(ratio) {
  return ratio === null || !Number.isFinite(ratio)
    ? "unknown"
    : ratio < 50
      ? "safe"
      : ratio < 80
        ? "watch"
        : "high";
}
