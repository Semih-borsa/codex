package com.semih.cepbutcem;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Android-independent accounting rules. Money is always stored as integer kuruş. */
public final class Finance {
  private Finance() {}

  public static final String INCOME = "I", EXPENSE = "E", TRANSFER = "T";
  public static final Locale TR = Locale.forLanguageTag("tr-TR");

  public static final class Entry {
    public long id, accountId, targetId, categoryId, amount;
    public long recordCount = 1, largestAmount;
    public String kind, note;
    public LocalDate date;

    public Entry(
        long id,
        String kind,
        long amount,
        LocalDate date,
        long accountId,
        long targetId,
        long categoryId,
        String note) {
      this.id = id;
      this.kind = kind;
      this.amount = amount;
      this.largestAmount = amount;
      this.date = date;
      this.accountId = accountId;
      this.targetId = targetId;
      this.categoryId = categoryId;
      this.note = note;
    }
  }

  public static final class Account {
    public long id, opening;
    public String name;

    public Account(long id, String name, long opening) {
      this.id = id;
      this.name = name;
      this.opening = opening;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static final class Category {
    public long id;
    public String name, kind;

    public Category(long id, String name, String kind) {
      this.id = id;
      this.name = name;
      this.kind = kind;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  public static long parseMoney(String input) {
    String normalized = input.trim().replace(" ", "").replace("₺", "");
    // Turkish input: 1.234,56; also accept ungrouped 1234.56.
    if (normalized.contains(",")) normalized = normalized.replace(".", "").replace(',', '.');
    if (!normalized.matches("-?\\d+(\\.\\d{1,2})?"))
      throw new IllegalArgumentException("Tutarı 1234,56 biçiminde yazın (en çok 2 ondalık).");
    try {
      long value = new BigDecimal(normalized).movePointRight(2).longValueExact();
      if (Math.abs(value) > 100_000_000_000L || value == Long.MIN_VALUE)
        throw new IllegalArgumentException("Tutar çok büyük.");
      return value;
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("Geçerli bir tutar yazın.");
    }
  }

  public static String money(long cents) {
    NumberFormat f = NumberFormat.getCurrencyInstance(TR);
    f.setCurrency(Currency.getInstance("TRY"));
    return f.format(BigDecimal.valueOf(cents, 2));
  }

  public static String editable(long cents) {
    return BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ',');
  }

  public static boolean within(Entry e, LocalDate from, LocalDate to) {
    return !e.date.isBefore(from) && !e.date.isAfter(to);
  }

  public static List<Entry> period(List<Entry> entries, LocalDate from, LocalDate to) {
    List<Entry> result = new ArrayList<>();
    for (Entry e : entries) if (within(e, from, to)) result.add(e);
    return result;
  }

  public static long total(List<Entry> entries, String kind) {
    long result = 0;
    for (Entry e : entries) if (kind.equals(e.kind)) result = Math.addExact(result, e.amount);
    return result;
  }

  public static long balance(Account a, List<Entry> entries, LocalDate asOf) {
    long result = a.opening;
    for (Entry e : entries) {
      if (e.date.isAfter(asOf)) continue;
      if (e.accountId == a.id)
        result = Math.addExact(result, INCOME.equals(e.kind) ? e.amount : -e.amount);
      if (TRANSFER.equals(e.kind) && e.targetId == a.id) result = Math.addExact(result, e.amount);
    }
    return result;
  }

  public static Map<Long, Long> byCategory(List<Entry> entries, String kind) {
    Map<Long, Long> sums = new HashMap<>();
    for (Entry e : entries)
      if (kind.equals(e.kind)) sums.merge(e.categoryId, e.amount, Math::addExact);
    List<Map.Entry<Long, Long>> sorted = new ArrayList<>(sums.entrySet());
    sorted.sort(
        Map.Entry.<Long, Long>comparingByValue()
            .reversed()
            .thenComparing(Map.Entry.comparingByKey()));
    Map<Long, Long> result = new LinkedHashMap<>();
    for (Map.Entry<Long, Long> e : sorted) result.put(e.getKey(), e.getValue());
    return result;
  }

  /** A cycle is named by the month in which its payday occurs. */
  public static LocalDate cycleStart(YearMonth month, int payday) {
    if (payday < 1 || payday > 31) throw new IllegalArgumentException("Maaş günü 1–31 olmalı.");
    return month.atDay(Math.min(payday, month.lengthOfMonth()));
  }

  public static LocalDate cycleEnd(YearMonth month, int payday) {
    return cycleStart(month.plusMonths(1), payday).minusDays(1);
  }

  public static YearMonth cycleMonth(LocalDate today, int payday) {
    YearMonth m = YearMonth.from(today);
    return today.isBefore(cycleStart(m, payday)) ? m.minusMonths(1) : m;
  }

  public static int cycleDays(YearMonth month, int payday, LocalDate today) {
    LocalDate start = cycleStart(month, payday), end = cycleEnd(month, payday);
    if (today.isBefore(start)) return 0;
    return (int)
            java.time.temporal.ChronoUnit.DAYS.between(start, today.isBefore(end) ? today : end)
        + 1;
  }

  public static long cycleForecast(long expense, YearMonth month, int payday, LocalDate today) {
    int elapsed = cycleDays(month, payday, today);
    int length =
        (int)
                java.time.temporal.ChronoUnit.DAYS.between(
                    cycleStart(month, payday), cycleEnd(month, payday))
            + 1;
    return elapsed == 0 ? 0 : Math.round(expense * (double) length / elapsed);
  }

  /** Forecast observed variable spending; known bill payments remain fixed. */
  public static long obligationForecast(
      long expense,
      long paidBills,
      long pendingBills,
      YearMonth month,
      int payday,
      LocalDate today) {
    if (paidBills < 0 || paidBills > expense || pendingBills < 0)
      throw new IllegalArgumentException("Fatura toplamı giderle uyumlu olmalı.");
    return Math.addExact(
        cycleForecast(expense - paidBills, month, payday, today),
        Math.addExact(paidBills, pendingBills));
  }

  public static double spendingRate(long income, long expense) {
    return income > 0 ? 100d * expense / income : Double.NaN;
  }

  /** Same thresholds for the balance card, period card and gauge. -1 means no income. */
  public static int spendingBand(double ratio) {
    return !Double.isFinite(ratio) ? -1 : ratio < 50 ? 0 : ratio < 80 ? 1 : 2;
  }

  /** Due date is strictly after closing, including equal preferred days. */
  public static LocalDate cardDue(LocalDate closing, int dueDay) {
    if (dueDay < 1 || dueDay > 31) throw new IllegalArgumentException("Ödeme günü 1–31 olmalı.");
    YearMonth m = YearMonth.from(closing);
    LocalDate due = cycleStart(m, dueDay);
    return due.isAfter(closing) ? due : cycleStart(m.plusMonths(1), dueDay);
  }

  public static List<Long> cycles(
      List<Entry> entries, YearMonth end, int count, String kind, int payday) {
    List<Long> result = new ArrayList<>();
    for (int i = count - 1; i >= 0; i--) {
      YearMonth m = end.minusMonths(i);
      result.add(total(period(entries, cycleStart(m, payday), cycleEnd(m, payday)), kind));
    }
    return result;
  }

  public static Entry repeat(Entry source, LocalDate date) {
    return new Entry(
        0,
        source.kind,
        source.amount,
        date,
        source.accountId,
        source.targetId,
        source.categoryId,
        source.note);
  }

  public static Map<LocalDate, Long> dailyExpenses(
      List<Entry> entries, LocalDate from, LocalDate to) {
    Map<LocalDate, Long> result = new TreeMap<>();
    for (Entry e : entries)
      if (EXPENSE.equals(e.kind) && within(e, from, to))
        result.merge(e.date, e.amount, Math::addExact);
    return result;
  }

  public static double savingRate(long income, long expense) {
    return income > 0 ? 100d * (income - expense) / income : Double.NaN;
  }

  public static double change(long current, long previous) {
    return previous > 0 ? 100d * (current - previous) / previous : Double.NaN;
  }

  public static int elapsedDays(YearMonth month, LocalDate today) {
    if (month.isAfter(YearMonth.from(today))) return 0;
    if (month.isBefore(YearMonth.from(today))) return month.lengthOfMonth();
    return today.getDayOfMonth();
  }

  public static long forecast(long expense, YearMonth month, LocalDate today) {
    int days = elapsedDays(month, today);
    return days == 0 ? 0 : Math.round(expense * (double) month.lengthOfMonth() / days);
  }

  public static List<Long> monthly(List<Entry> entries, YearMonth end, int count, String kind) {
    List<Long> result = new ArrayList<>();
    for (int i = count - 1; i >= 0; i--) {
      YearMonth m = end.minusMonths(i);
      result.add(total(period(entries, m.atDay(1), m.atEndOfMonth()), kind));
    }
    return result;
  }

  public static LocalDate nextMonthly(LocalDate current, int preferredDay) {
    YearMonth next = YearMonth.from(current).plusMonths(1);
    return next.atDay(Math.min(preferredDay, next.lengthOfMonth()));
  }

  public static void validate(Entry e) {
    if (!Arrays.asList(INCOME, EXPENSE, TRANSFER).contains(e.kind))
      throw new IllegalArgumentException("İşlem türü geçersiz.");
    if (e.amount <= 0 || e.amount > 100_000_000_000L)
      throw new IllegalArgumentException("Tutar sıfırdan büyük ve en çok 1 milyar TL olmalı.");
    if (e.accountId <= 0 || e.date == null)
      throw new IllegalArgumentException("Hesap ve tarih seçin.");
    if (TRANSFER.equals(e.kind) && (e.targetId <= 0 || e.targetId == e.accountId))
      throw new IllegalArgumentException("Transfer için iki farklı hesap seçin.");
    if (!TRANSFER.equals(e.kind) && e.categoryId <= 0)
      throw new IllegalArgumentException("Kategori seçin.");
    if (e.note != null && e.note.length() > 1000)
      throw new IllegalArgumentException("Not en çok 1000 karakter olabilir.");
  }

  public static String csvCell(String value) {
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
