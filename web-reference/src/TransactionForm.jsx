import React, { useId, useRef, useState } from "react";
import { isDate, parseMinor, todayISO } from "./finance.js";
export function TransactionForm({
  categories,
  onSave,
  onCancel,
  initialKind = "expense",
}) {
  const [kind, setKind] = useState(initialKind),
    [amount, setAmount] = useState("");
  const [categoryId, setCategory] = useState(
    categories.find((c) => c.kind === initialKind)?.id ?? "",
  );
  const [occurredOn, setDate] = useState(todayISO()),
    [note, setNote] = useState("");
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const retry = useRef(null);
  const errorId = useId();
  const control =
    "w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-base dark:border-slate-700 dark:bg-slate-900 focus:outline-2 focus:outline-teal-500";
  async function submit(event) {
    event.preventDefault();
    setError("");
    try {
      if (
        !categoryId ||
        !categories.some((c) => c.id === categoryId && c.kind === kind)
      )
        throw new Error("Uygun bir kategori seç.");
      if (!isDate(occurredOn))
        throw new Error("2000–2100 arasında geçerli bir tarih seç.");
      const payload = {
        kind,
        amountMinor: parseMinor(amount),
        categoryId,
        occurredOn,
        note: note.trim(),
      };
      const fingerprint = JSON.stringify(payload);
      if (retry.current?.fingerprint !== fingerprint)
        retry.current = { fingerprint, key: crypto.randomUUID() };
      setBusy(true);
      await onSave(payload, retry.current.key);
      retry.current = null;
      setAmount("");
      setNote("");
      setKind(initialKind);
      setCategory(categories.find((c) => c.kind === initialKind)?.id ?? "");
    } catch (failure) {
      setError(
        failure instanceof Error
          ? failure.message
          : "Kaydedilemedi. Tekrar dene.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4 disabled:opacity-70">
        <legend className="mb-4 text-xl font-bold">Hızlı işlem ekle</legend>
        <div className="grid grid-cols-2 gap-2" aria-label="İşlem türü">
          {[
            ["expense", "Gider"],
            ["income", "Gelir"],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              aria-pressed={kind === value}
              onClick={() => {
                setKind(value);
                setCategory(categories.find((c) => c.kind === value)?.id ?? "");
              }}
              className={`min-h-12 rounded-xl font-semibold ${kind === value ? "bg-teal-700 text-white" : "bg-slate-100 dark:bg-slate-800"}`}
            >
              {label}
            </button>
          ))}
        </div>
        <label className="block text-sm font-medium">
          Tutar · TL
          <input
            autoFocus
            inputMode="decimal"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0,00"
            className={`${control} mt-2 text-2xl font-bold`}
            aria-describedby={error ? errorId : undefined}
          />
        </label>
        <label className="block text-sm font-medium">
          Kategori
          <select
            required
            value={categoryId}
            onChange={(e) => setCategory(e.target.value)}
            className={`${control} mt-2`}
          >
            <option value="" disabled>
              Seç
            </option>
            {categories
              .filter((c) => c.kind === kind)
              .map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
          </select>
        </label>
        <details className="rounded-xl bg-slate-50 p-3 dark:bg-slate-800">
          <summary className="min-h-8 cursor-pointer text-sm">
            Tarih ve açıklama · isteğe bağlı
          </summary>
          <div className="mt-3 space-y-3">
            <label className="block text-sm">
              Tarih
              <input
                required
                type="date"
                min="2000-01-01"
                max="2100-12-31"
                value={occurredOn}
                onChange={(e) => setDate(e.target.value)}
                className={`${control} mt-1`}
              />
            </label>
            <label className="block text-sm">
              Açıklama
              <textarea
                maxLength={1000}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className={`${control} mt-1`}
                rows={2}
              />
            </label>
          </div>
        </details>
        {error && (
          <p
            id={errorId}
            role="alert"
            className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800 dark:bg-rose-950 dark:text-rose-200"
          >
            {error}
          </p>
        )}
        <button
          type="submit"
          className="min-h-12 w-full rounded-xl bg-teal-700 px-5 py-3 font-bold text-white hover:bg-teal-800"
        >
          {busy ? "Kaydediliyor…" : "Kaydet"}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="min-h-12 w-full rounded-xl border border-slate-300 dark:border-slate-700"
          >
            Vazgeç
          </button>
        )}
      </fieldset>
    </form>
  );
}
