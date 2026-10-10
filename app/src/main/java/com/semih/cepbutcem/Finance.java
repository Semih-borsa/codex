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
