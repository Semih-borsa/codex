import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { TransactionForm } from "./TransactionForm.jsx";
import { PeriodSummary } from "./PeriodSummary.jsx";
import { salaryPeriod, todayISO } from "./finance.js";
import "./styles.css";
const categories = [
  { id: "market", name: "Market", kind: "expense" },
  { id: "bills", name: "Faturalar", kind: "expense" },
  { id: "travel", name: "Ulaşım", kind: "expense" },
  { id: "salary", name: "Maaş", kind: "income" },
  { id: "extra", name: "Ek gelir", kind: "income" },
];
function App() {
  const [transactions, setTransactions] = useState([]),
    [payday, setPayday] = useState(1),
    [dark, setDark] = useState(
      () => matchMedia("(prefers-color-scheme: dark)").matches,
    );
  const dialog = useRef(null);
  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
  }, [dark]);
  const period = salaryPeriod(todayISO(), payday);
  async function save(payload, key) {
    setTransactions((previous) =>
      previous.some((t) => t.id === key)
        ? previous
        : [
            { ...payload, id: key, createdAt: new Date().toISOString() },
            ...previous,
          ],
    );
    dialog.current.close();
  }
  return (
    <main className="min-h-screen bg-slate-50 px-4 pb-28 text-slate-950 dark:bg-[#0b1020] dark:text-slate-50">
      <div className="mx-auto max-w-5xl">
        <header className="flex items-center justify-between gap-4 py-6">
          <div>
            <p className="text-xs font-semibold tracking-widest text-slate-500 dark:text-slate-400">
              CEP BÜTÇEM
            </p>
            <h1 className="mt-1 text-2xl font-bold">Finansına genel bakış</h1>
          </div>
          <button
            onClick={() => setDark((v) => !v)}
            aria-pressed={dark}
            className="min-h-12 rounded-xl bg-white px-4 text-sm font-semibold dark:bg-slate-800"
          >
            {dark ? "Açık tema" : "Koyu tema"}
          </button>
        </header>
        <p className="mb-5 rounded-xl bg-indigo-50 p-3 text-sm text-indigo-900 dark:bg-indigo-950 dark:text-indigo-200">
          Çalışan web referansı · kayıtlar bu oturumdadır; sayfa yenilenince
          temizlenir. APK’daki verilerle bağlantısı yoktur.
        </p>
        <label className="mb-5 flex items-center gap-3 text-sm">
          Maaş günüm
          <select
            value={payday}
            onChange={(e) => setPayday(Number(e.target.value))}
            className="min-h-12 rounded-xl border border-slate-200 bg-white px-4 dark:border-slate-700 dark:bg-slate-900"
          >
            {Array.from({ length: 31 }, (_, i) => (
              <option key={i + 1} value={i + 1}>
                {i + 1}
              </option>
            ))}
          </select>
        </label>
        <div className="grid items-start gap-6 lg:grid-cols-[1.2fr_1fr]">
          <PeriodSummary
            transactions={transactions}
            categories={categories}
            {...period}
          />
          <aside className="hidden rounded-3xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900 lg:block">
            <TransactionForm categories={categories} onSave={save} />
          </aside>
        </div>
      </div>
      <div className="fixed inset-x-0 bottom-0 border-t border-slate-200 bg-white/95 p-4 pb-[max(1rem,env(safe-area-inset-bottom))] backdrop-blur dark:border-slate-800 dark:bg-slate-950/95 lg:hidden">
        <button
          onClick={() => dialog.current.showModal()}
          className="min-h-12 w-full rounded-xl bg-teal-700 py-3 font-bold text-white"
        >
          + İşlem ekle
        </button>
      </div>
      <dialog
        ref={dialog}
        aria-label="Yeni işlem"
        className="fixed inset-0 m-auto max-h-[90dvh] w-[calc(100%_-_2rem)] max-w-md overflow-auto rounded-3xl bg-white p-6 text-slate-950 backdrop:bg-slate-950/60 dark:bg-slate-900 dark:text-slate-50"
      >
        <TransactionForm
          categories={categories}
          onSave={save}
          onCancel={() => dialog.current.close()}
        />
      </dialog>
    </main>
  );
}
createRoot(document.getElementById("root")).render(<App />);
