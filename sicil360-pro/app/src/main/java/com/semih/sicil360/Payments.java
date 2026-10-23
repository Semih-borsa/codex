package com.semih.sicil360;

import java.math.*;
import java.util.*;

/** Immutable original debts; payments are integer kuruş, never floating point. */
public final class Payments {
  public static final long MAX = 99999999999999L;

  public static final class Entry {
    public String id = UUID.randomUUID().toString(), note = "";
    public long amount, time = System.currentTimeMillis();
    public boolean cancelled;
  }

  public static final class Account {
    public String code = "", title = "", first = "", last = "";
    public long original;
    public final List<Entry> payments = new ArrayList<>();

    public String name() {
      return (first + " " + last).trim();
    }

    public long paid() {
      long n = 0;
      for (Entry e : payments) if (!e.cancelled) n = Math.addExact(n, e.amount);
      return n;
    }

    public long remaining() {
      return Math.max(0, original - paid());
    }

    public long excess() {
      return Math.max(0, paid() - original);
    }
  }

  public static final class Ledger {
    public String id = UUID.randomUUID().toString(), name = "", fingerprint = "";
    public String period = java.time.YearMonth.now().toString();
    public long created = System.currentTimeMillis();
    public final LinkedHashMap<String, Account> accounts = new LinkedHashMap<>();
  }

  public static final class Trend {
    public int appearances, unpaidStreak, percent;
    public long original, covered;
  }

  public static Trend trend(Model.State state, String code, java.time.YearMonth end) {
    Trend t = new Trend();
    java.util.TreeMap<java.time.YearMonth, Account> months = new java.util.TreeMap<>();
    for (Ledger l : state.debts) {
      java.time.YearMonth month = java.time.YearMonth.parse(l.period);
      if (month.isAfter(end)) continue;
      Account a = l.accounts.get(code);
      if (a == null) continue;
      months.put(month, a);
      t.original = Math.addExact(t.original, a.original);
      t.covered = Math.addExact(t.covered, Math.min(a.original, a.paid()));
    }
    for (java.time.YearMonth m : months.keySet())
      if (!m.isBefore(end.minusMonths(2))) t.appearances++;
    java.time.YearMonth cursor = end;
    while (months.containsKey(cursor)
        && months.get(cursor).original > 0
        && months.get(cursor).paid() == 0) {
      t.unpaidStreak++;
      cursor = cursor.minusMonths(1);
    }
    if (t.original > 0)
      t.percent =
          java.math.BigDecimal.valueOf(t.covered)
              .multiply(java.math.BigDecimal.valueOf(100))
              .divide(java.math.BigDecimal.valueOf(t.original), 0, java.math.RoundingMode.HALF_UP)
              .intValueExact();
    return t;
  }

  public static final class Totals {
    public long original, paid, remaining, excess;
    public int closed;
  }

  public static Totals totals(Ledger ledger) {
    Totals t = new Totals();
    for (Account a : ledger.accounts.values()) {
      t.original = Math.addExact(t.original, a.original);
      t.paid = Math.addExact(t.paid, a.paid());
      t.remaining = Math.addExact(t.remaining, a.remaining());
      t.excess = Math.addExact(t.excess, a.excess());
      if (a.remaining() == 0) t.closed++;
    }
    return t;
  }

  public static long money(String value, boolean numeric) {
    String s =
        value
            .trim()
            .replace("₺", "")
            .replaceAll("(?i)\\s*(TL|TRY)$", "")
            .trim()
            .replace("\u00a0", "");
    if (!numeric) {
      if (s.contains(",")) {
        if (!s.matches("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+),[0-9]{1,2}"))
          throw new IllegalArgumentException("Tutarı 1.234,56 Biçiminde Girin.");
        s = s.replace(".", "").replace(',', '.');
      } else if (s.matches("[0-9]{1,3}(?:\\.[0-9]{3})+")) s = s.replace(".", "");
      else if (!s.matches("[0-9]+(?:\\.[0-9]{1,2})?"))
        throw new IllegalArgumentException(
            "Geçerli Bir Tutar Girin. En Fazla İki Ondalık Basamak Kullanılabilir.");
    }
    try {
      if (s.length() > 32) throw new ArithmeticException();
      BigDecimal n = new BigDecimal(s);
      if (n.precision() > 16 || Math.abs((long) n.scale()) > 16) throw new ArithmeticException();
      long cents = n.movePointRight(2).longValueExact();
      if (cents < 0 || cents > MAX) throw new ArithmeticException();
      return cents;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Tutar Geçersiz, Negatif Veya Desteklenen Sınırın Üzerinde.");
    }
  }

  public static String format(long cents) {
    java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
    return f.format(BigDecimal.valueOf(cents, 2));
  }

  public static void add(Account a, long amount, String note) {
    if (amount <= 0 || amount > MAX || a.paid() > MAX - amount)
      throw new IllegalArgumentException("Ödeme Pozitif Olmalı Ve Tutar Sınırını Aşmamalı.");
    if (a.payments.size() >= 10000)
      throw new IllegalArgumentException("Kişi Başına 10.000 Ödeme Kaydı Sınırı.");
    if (note.length() > 500)
      throw new IllegalArgumentException("Ödeme Notu En Fazla 500 Karakter Olabilir.");
    Entry e = new Entry();
    e.amount = amount;
    e.note = note;
    a.payments.add(e);
  }

  public static Ledger find(Model.State state, String id) {
    for (Ledger l : state.debts) if (l.id.equals(id)) return l;
    throw new IllegalArgumentException("Borç Listesi Bulunamadı.");
  }

  static String hash(String s) throws Exception {
    byte[] b =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder();
    for (byte x : b) out.append(String.format(Locale.ROOT, "%02x", x));
    return out.toString();
  }

  public static boolean looksLike(Excel.Table table) {
    if (table.rows.isEmpty()) return false;
    int row = table.rows.firstKey();
    return Model.fold(table.value(row, 0)).contains("SIRA")
        && Model.fold(table.value(row, 1)).contains("CARI")
        && Model.fold(table.value(row, 5)).contains("BAKIYE");
  }

  public static Ledger read(Excel.Table table) throws Exception {
    if (table.rows.isEmpty()) throw new IllegalArgumentException("Excel Sayfası Boş.");
    int header = table.rows.firstKey();
    String[] expected = {"SIRA", "CARI", "TICARI", "AD", "SOYAD", "BAKIYE"};
    for (int c = 0; c < 6; c++)
      if (!Model.fold(table.value(header, c)).contains(expected[c]))
        throw new IllegalArgumentException(
            "İlk Satır A:Sıra No, B:Cari Kodu, C:Ticari Unvanı, D:Adı, E:Soyadı, F:Bakiye Olmalı.");
    Ledger ledger = new Ledger();
    Long suppliedTotal = null;
    List<String> errors = new ArrayList<>();
    for (Map.Entry<Integer, TreeMap<Integer, Excel.Cell>> row : table.rows.entrySet()) {
      int r = row.getKey();
      if (r <= header) continue;
      if (row.getValue().values().stream().allMatch(c -> c.value.trim().isEmpty())) continue;
      boolean total = false;
      for (int c = 0; c < 5; c++) {
        String label = Model.fold(table.value(r, c));
        if (label.equals("TOPLAM") || label.equals("GENEL TOPLAM")) total = true;
      }
      Excel.Cell balance = row.getValue().get(5);
      if (total && table.value(r, 3).trim().isEmpty() && table.value(r, 4).trim().isEmpty()) {
        if (balance != null && !balance.formula && !balance.value.isEmpty()) {
          long amount = money(balance.value, balance.numeric);
          if (suppliedTotal != null && suppliedTotal != amount)
            throw new IllegalArgumentException("Birden Fazla Farklı Genel Toplam Var.");
          suppliedTotal = amount;
        }
        continue;
      }
      try {
        Account a = new Account();
        a.code = table.value(r, 1).trim();
        a.title = table.value(r, 2).trim();
        a.first = table.value(r, 3).trim();
        a.last = table.value(r, 4).trim();
        if (a.code.isEmpty() || !a.code.matches("[\\p{L}\\p{N}._/-]{1,64}"))
          throw new IllegalArgumentException("Cari Kodu Eksik Veya Geçersiz.");
        if (a.name().isEmpty()) throw new IllegalArgumentException("Ad Soyad Eksik.");
        Excel.Cell code = row.getValue().get(1);
        if (code != null && (code.formula || code.unsafe))
          throw new IllegalArgumentException(
              "Cari Kodu Formül Veya Hassasiyet Kaybı İçeriyor; Metin Olarak Kaydedin.");
        if (balance == null || balance.formula || balance.unsafe)
          throw new IllegalArgumentException(
              "Bakiye Eksik, Formül Veya Güvensiz Sayı; Değer Olarak Kaydedin.");
        a.original = money(balance.value, balance.numeric);
        if (ledger.accounts.putIfAbsent(a.code, a) != null)
          throw new IllegalArgumentException(
              "Mükerrer Cari Kodu. Ayrı Satırlar Sessizce Birleştirilmez.");
      } catch (IllegalArgumentException e) {
        if (errors.size() < 20) errors.add("Satır " + r + ": " + e.getMessage());
      }
    }
    if (!errors.isEmpty())
      throw new IllegalArgumentException(
          String.join("\n", errors) + "\nDosya Kaydedilmedi. Hataları Düzeltip Yeniden Yükleyin.");
    if (ledger.accounts.isEmpty()) throw new IllegalArgumentException("Borç Kaydı Bulunamadı.");
    Totals t = totals(ledger);
    if (suppliedTotal != null && suppliedTotal != t.original)
      throw new IllegalArgumentException(
          "Excel Genel Toplamı İle Satırların Toplamı Farklı. Dosyayı Kontrol Edin.");
    StringBuilder canonical = new StringBuilder();
    for (String code : new TreeSet<>(ledger.accounts.keySet())) {
      Account a = ledger.accounts.get(code);
      canonical
          .append(code.length())
          .append(':')
          .append(code)
          .append(':')
          .append(a.original)
          .append(';');
    }
    ledger.fingerprint = hash(canonical.toString());
    return ledger;
  }
}
