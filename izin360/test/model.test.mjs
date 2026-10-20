import test from "node:test";
import assert from "node:assert/strict";
import {
  emptyData,
  blankBase,
  parseNumber,
  countDays,
  balance,
  applyChanges,
  validateData,
  diff,
  receiveSync,
  resolveConflict,
  csv,
  backup,
  readBackup,
} from "../src/model.mjs";
export const employee = (id = "person-1") => ({
  id,
  name: "Ayşe Yılmaz",
  startDate: "2026-10-01",
  entitlement: 0,
  carry: 0,
  openingUsed: 0,
  archived: false,
  department: "Muhasebe",
  position: "Uzman",
  phone: "",
  note: "",
});
test("new employee starts with zero entitlement, no seniority calculation", () => {
  const data = emptyData(),
    e = employee();
  e.startDate = "2000-01-01";
  data.employees[e.id] = e;
  assert.equal(balance(data, e).remaining, 0);
});
test("exact fractional leave quantities and signed manual corrections", () => {
  assert.equal(parseNumber("0,5"), 500);
  assert.equal(parseNumber("-0.125", 1000, { signed: true }), -125);
  assert.equal(parseNumber("123,45", 100), 12345);
  for (const s of ["NaN", "1e5", "1.0001", "-2"])
    assert.throws(() => parseNumber(s));
});
test("leave dates include both endpoints, optional weekends, leap year", () => {
  assert.equal(countDays("2026-10-23", "2026-10-26"), 4000);
  assert.equal(countDays("2026-10-23", "2026-10-26", true), 2000);
  assert.equal(countDays("2028-02-28", "2028-03-01"), 3000);
  assert.throws(() => countDays("2026-02-30", "2026-03-02"));
  assert.throws(() => countDays("2026-11-02", "2026-11-01"));
});
test("edit and deletion recalculate balance without double deduction", () => {
  let d = emptyData(),
    e = { ...employee(), entitlement: 20000, carry: 4000, openingUsed: 2000 };
  d.employees[e.id] = e;
  const leave = {
    id: "l1",
    employeeId: e.id,
    kind: "Yıllık izin",
    start: "2026-10-20",
    end: "2026-10-21",
    days: 2000,
    deduct: true,
    note: "",
  };
  d = applyChanges(d, [{ table: "leaves", id: leave.id, value: leave }]);
  assert.equal(balance(d, e).remaining, 20000);
  d = applyChanges(d, [
    { table: "leaves", id: leave.id, value: { ...leave, days: 500 } },
  ]);
  assert.equal(balance(d, e).remaining, 21500);
  d = applyChanges(d, [{ table: "leaves", id: leave.id, value: null }]);
  assert.equal(balance(d, e).remaining, 22000);
  assert.equal(Object.keys(d.audit).length, 3);
});
test("non-deducting leave, adjustment and manual used total stay independent", () => {
  const d = emptyData(),
    e = { ...employee(), entitlement: 10000 };
  d.employees[e.id] = e;
  d.leaves.l = {
    id: "l",
    employeeId: e.id,
    kind: "Rapor",
    start: "2026-10-20",
    end: "2026-10-20",
    days: 1000,
    deduct: false,
    note: "",
  };
  d.adjustments.a = {
    id: "a",
    employeeId: e.id,
    amount: -500,
    date: "2026-10-20",
    reason: "Eski kayıt düzeltmesi",
  };
  assert.equal(balance(d, e).remaining, 9500);
  e.openingUsed = 2000;
  assert.equal(balance(d, e).remaining, 7500);
  validateData(d);
});
test("payroll and attendance duplicate rows are rejected", () => {
  const d = emptyData(),
    e = employee();
  d.employees[e.id] = e;
  d.attendance.a = {
    id: "a",
    employeeId: e.id,
    date: "2026-10-20",
    status: "Çalıştı",
    minutes: 480,
    overtime: 60,
    note: "",
  };
  d.attendance.b = { ...d.attendance.a, id: "b" };
  assert.throws(() => validateData(d), /ikinci puantaj/);
  delete d.attendance.b;
  d.payroll.p = {
    id: "p",
    employeeId: e.id,
    month: "2026-10",
    base: 100000,
    extra: 10000,
    deduction: 5000,
    paid: 0,
    note: "",
  };
  d.payroll.q = { ...d.payroll.p, id: "q" };
  assert.throws(() => validateData(d), /ikinci maaş/);
});
test("bad backup never mutates original, round trip preserves audit and values", () => {
  let d = emptyData();
  d = applyChanges(d, [
    { table: "employees", id: "person-1", value: employee() },
  ]);
  const saved = backup(d);
  assert.deepEqual(readBackup(saved), d);
  const invalid = JSON.parse(saved);
  invalid.data.employees["person-1"].entitlement = -1000;
  assert.throws(() => readBackup(JSON.stringify(invalid)));
  assert.equal(d.employees["person-1"].entitlement, 0);
  assert.throws(() => readBackup('{"app":"other","version":1}'));
});
test("orphan employee references are rejected", () => {
  const d = emptyData();
  d.adjustments.a = {
    id: "a",
    employeeId: "constructor",
    amount: 1000,
    reason: "Test",
    date: "2026-10-20",
  };
  assert.throws(() => validateData(d));
});
test("pending changes survive local edit during successful sync response", () => {
  const e = employee();
  const s = {
    data: emptyData(),
    base: blankBase(),
    conflicts: [],
    lastSync: null,
  };
  s.data.employees[e.id] = { ...e, name: "Son düzenleme" };
  const response = {
    ok: true,
    records: [{ table: "employees", id: e.id, version: 1, value: e }],
    conflicts: [],
  };
  const next = receiveSync(s, response);
  assert.equal(next.data.employees[e.id].name, "Son düzenleme");
  assert.equal(diff(next.data, next.base)[0].baseVersion, 1);
});
test("conflict cannot silently overwrite remote on next sync", () => {
  const original = employee(),
    local = { ...original, name: "Telefondaki" },
    remote = { ...original, name: "Windows’taki" };
  const s = {
    data: emptyData(),
    base: blankBase(),
    conflicts: [],
    lastSync: null,
  };
  s.data.employees[local.id] = local;
  s.base.employees[original.id] = { version: 1, value: original };
  const c = { table: "employees", id: local.id, version: 2, remote };
  const next = receiveSync(s, {
    ok: false,
    records: [{ table: "employees", id: remote.id, version: 2, value: remote }],
    conflicts: [c],
  });
  assert.equal(next.base.employees[local.id].version, 1);
  assert.equal(next.conflicts.length, 1);
  const localChoice = resolveConflict(next, next.conflicts[0], true);
  assert.equal(diff(localChoice.data, localChoice.base)[0].baseVersion, 2);
  const remoteChoice = resolveConflict(next, next.conflicts[0], false);
  assert.equal(remoteChoice.data.employees[local.id].name, remote.name);
  assert.equal(diff(remoteChoice.data, remoteChoice.base).length, 0);
});
test("CSV quotes text and neutralizes Excel formulas", () => {
  const out = csv([
    ["Ad", "Not"],
    ['=HYPERLINK("bad")', "a;b"],
  ]);
  assert.ok(out.startsWith("\uFEFF"));
  assert.match(out, /'=HYPERLINK/);
  assert.match(out, /"a;b"/);
});
