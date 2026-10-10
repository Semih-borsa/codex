import test from "node:test";
import assert from "node:assert/strict";
import {
  parseMinor,
  money,
  salaryPeriod,
  summarize,
  band,
  isDate,
} from "./finance.js";
test("exact Turkish cents", () => {
  assert.equal(parseMinor("1.234,56"), "123456");
  assert.equal(parseMinor("0.01"), "1");
  assert.throws(() => parseMinor("1,001"));
  assert.throws(() => parseMinor("-1"));
  assert.throws(() => parseMinor("0"));
});
test("money keeps large integer precision", () => {
  assert.equal(money("9007199254740993"), "₺90.071.992.547.409,93");
  assert.equal(money(-101n), "−₺1,01");
});
test("payday spans months and short leap month", () => {
  assert.deepEqual(salaryPeriod("2024-03-01", 31), {
    from: "2024-02-29",
    to: "2024-03-30",
  });
  assert.deepEqual(salaryPeriod("2026-10-10", 15), {
    from: "2026-09-15",
    to: "2026-10-14",
  });
});
test("strict dates and payday", () => {
  assert.equal(isDate("2026-02-30"), false);
  assert.equal(isDate("2024-02-29"), true);
  assert.throws(() => salaryPeriod("2026-10-10", 32));
});
test("transfers and future transactions excluded", () => {
  const data = [
    { kind: "income", amountMinor: "10000", occurredOn: "2026-10-01" },
    { kind: "expense", amountMinor: "15000", occurredOn: "2026-10-02" },
    { kind: "transfer", amountMinor: "9999", occurredOn: "2026-10-02" },
    { kind: "expense", amountMinor: "9999", occurredOn: "2026-10-30" },
  ];
  const result = summarize(data, "2026-10-01", "2026-10-31", "2026-10-10");
  assert.equal(result.expense, 15000n);
  assert.equal(result.ratio, 150);
});
test("bands use exact thresholds and unknown income", () => {
  assert.equal(band(49.9), "safe");
  assert.equal(band(50), "watch");
  assert.equal(band(80), "high");
  assert.equal(band(null), "unknown");
  assert.equal(
    summarize([], "2026-10-01", "2026-10-31", "2026-10-10").ratio,
    null,
  );
});
