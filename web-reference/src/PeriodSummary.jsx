import React, { useEffect, useState } from "react";
import { band, money, summarize, todayISO } from "./finance.js";
function useAnimatedRatio(target) {
  const [value, setValue] = useState(0);
  useEffect(() => {
    if (
      target === null ||
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      setValue(target);
      return;
    }
    let frame, start;
    setValue(0);
    function tick(time) {
      start ??= time;
      const progress = Math.min(1, (time - start) / 1000);
      setValue(target * (1 - (1 - progress) ** 3));
      if (progress < 1) frame = requestAnimationFrame(tick);
    }
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target]);
  return value;
}
const surfaces = {
  safe: "from-emerald-800 to-teal-700",
  watch: "from-amber-800 to-yellow-700",
  high: "from-rose-900 to-red-700",
  unknown: "from-indigo-800 to-blue-800",
};
const labels = {
  safe: "Dengede",
  watch: "Dikkat",
  high: "Yüksek harcama",
  unknown: "Gelir bekleniyor",
};
export function PeriodSummary({ transactions, categories, from, to }) {
  const today = todayISO(),
    { income, expense, net, ratio } = summarize(transactions, from, to, today);
  const state = band(ratio),
    animated = useAnimatedRatio(ratio);
  const names = new Map(categories.map((c) => [c.id, c.name]));
  const list = transactions
    .filter((t) => t.occurredOn >= from && t.occurredOn <= to)
    .toSorted(
      (a, b) =>
        b.occurredOn.localeCompare(a.occurredOn) ||
        (b.createdAt ?? "").localeCompare(a.createdAt ?? ""),
    )
    .slice(0, 6);
  return (
    <section aria-label="Maaş dönemi özeti" className="space-y-5">
      <div
        className={`rounded-3xl bg-linear-to-br ${surfaces[state]} p-6 text-white shadow-lg`}
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm font-medium">Maaş dönemi dengesi</p>
          <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-bold">
            {labels[state]}
          </span>
        </div>
        <p className="mt-3 break-words text-4xl font-bold tracking-tight">
          {money(net)}
        </p>
        <p className="mt-2 text-xs text-white/85">
          {from} — {to} · Gelir − gider; banka bakiyesi değildir.
        </p>
        <div className="mt-5 flex items-end justify-between gap-2">
          <span className="text-sm">Gelirin harcanan kısmı</span>
          <span aria-hidden="true" className="text-2xl font-bold">
            {animated === null
              ? "—"
              : `%${(animated ?? 0).toLocaleString("tr-TR", { maximumFractionDigits: 1, minimumFractionDigits: 1 })}`}
          </span>
          <span className="sr-only">
            {ratio === null ? "Gelir kaydı gerekli" : `Yüzde ${ratio}`}
          </span>
        </div>
        <div
          aria-hidden="true"
          className="mt-2 h-2 overflow-hidden rounded-full bg-white/20"
        >
          <div
            className="h-full rounded-full bg-white"
            style={{ width: `${Math.min(100, Math.max(0, animated ?? 0))}%` }}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        {[
          ["Gelir", income, "text-teal-700 dark:text-teal-300"],
          ["Gider", expense, "text-rose-700 dark:text-rose-300"],
        ].map(([title, value, color]) => (
          <div
            key={title}
            className="rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-900"
          >
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {title}
            </p>
            <p className={`mt-2 break-words text-xl font-bold ${color}`}>
              {money(value)}
            </p>
          </div>
        ))}
      </div>
      <div className="rounded-2xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
        <h2 className="text-lg font-bold">Son işlemler</h2>
        {!list.length && (
          <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
            İlk gelir veya giderini ekleyerek başla.
          </p>
        )}
        <ul className="mt-2 divide-y divide-slate-100 dark:divide-slate-800">
          {list.map((t) => (
            <li
              key={t.id}
              className="flex items-center justify-between gap-3 py-3"
            >
              <div className="min-w-0">
                <p className="font-medium">
                  {names.get(t.categoryId) ?? "Diğer"}
                </p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {t.occurredOn}
                  {t.occurredOn > today ? " · Planlanan" : ""}
                </p>
                {t.note && (
                  <p className="truncate text-sm text-slate-500 dark:text-slate-400">
                    {t.note}
                  </p>
                )}
              </div>
              <p
                className={`shrink-0 font-semibold ${t.kind === "income" ? "text-teal-700 dark:text-teal-300" : "text-rose-700 dark:text-rose-300"}`}
              >
                {t.kind === "income" ? "+" : "−"}
                {money(t.amountMinor)}
              </p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
