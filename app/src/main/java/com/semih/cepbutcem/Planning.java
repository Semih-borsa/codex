package com.semih.cepbutcem;

import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Local preferences and planned obligations; schema validated before restore. */
public final class Planning {
  public int payday = 1, target = 20;
  public boolean light = false;
  public SalarySettings salary = new SalarySettings();
  public final List<Card> cards = new ArrayList<>();
  public final List<Bill> bills = new ArrayList<>();
  public final List<Loan> loans = new ArrayList<>();

  public static final class Loan {
    public long id, principal, installment, account, category;
    public int count;
    public String name;
    public LocalDate first;

    public long total() {
      return Math.multiplyExact(installment, count);
    }
  }

  public static final class Card {
    public long account;
    public int closing, due;
    public long statement, current, pending;
    public LocalDate updated;

    public long bankTotal() {
      return Math.addExact(Math.addExact(statement, current), pending);
    }

    public Card(long account, int closing, int due) {
      this.account = account;
      this.closing = closing;
      this.due = due;
    }
  }

  public static final class Bill {
    public long id, amount, account, category, paid, loan;
    public int installment;
    public int day;
    public String name;
    public LocalDate due, trackedPaid;

    public boolean isPaid() {
      return paid != 0 || trackedPaid != null;
    }

    public boolean monthly;
  }

  public long nextBillId() {
    long max = 0;
    for (Bill b : bills) max = Math.max(max, b.id);
    return Math.addExact(max, 1);
  }

  public long nextLoanId() {
    long max = 0;
    for (Loan l : loans) max = Math.max(max, l.id);
    return Math.addExact(max, 1);
  }

  public JSONObject json() throws JSONException {
    JSONObject o = new JSONObject();
    o.put("payday", payday);
    o.put("target", target);
    o.put("light", light);
    o.put("salary", salary.json());
    JSONArray cs = new JSONArray(), bs = new JSONArray();
    for (Card c : cards) {
      JSONObject card =
          new JSONObject().put("account", c.account).put("closing", c.closing).put("due", c.due);
      if (c.updated != null)
        card.put("statement", c.statement)
            .put("current", c.current)
            .put("pending", c.pending)
            .put("updated", c.updated.toString());
      cs.put(card);
    }
    for (Bill b : bills)
      bs.put(
          new JSONObject()
              .put("id", b.id)
              .put("name", b.name)
              .put("amount", b.amount)
              .put("account", b.account)
              .put("category", b.category)
              .put("due", b.due.toString())
              .put("paid", b.paid)
              .put("monthly", b.monthly)
              .put("day", b.day)
              .put("loan", b.loan)
              .put("installment", b.installment)
              .put(
                  "trackedPaid",
                  b.trackedPaid == null ? JSONObject.NULL : b.trackedPaid.toString()));
    JSONArray ls = new JSONArray();
    for (Loan l : loans)
      ls.put(
          new JSONObject()
              .put("id", l.id)
              .put("name", l.name)
              .put("principal", l.principal)
              .put("installment", l.installment)
              .put("count", l.count)
              .put("first", l.first.toString())
              .put("account", l.account)
              .put("category", l.category));
    return o.put("cards", cs).put("bills", bs).put("loans", ls);
  }

  private static long number(JSONObject o, String key, long min, long max) throws JSONException {
    Object n = o.get(key);
    if (!(n instanceof Number) || !n.toString().matches("[0-9]+"))
      throw new JSONException("Geçersiz sayı: " + key);
    long value = o.getLong(key);
    if (value < min || value > max) throw new JSONException("Sayı aralık dışında: " + key);
    return value;
  }

  private static boolean bool(JSONObject o, String key) throws JSONException {
    Object value = o.get(key);
    if (!(value instanceof Boolean)) throw new JSONException("Geçersiz seçim: " + key);
    return (Boolean) value;
  }

  public static Planning parse(
      JSONObject o, Set<Long> accounts, Map<Long, String> kinds, Map<Long, Finance.Entry> entries)
      throws JSONException {
    Planning p = new Planning();
    p.payday = (int) number(o, "payday", 1, 31);
    p.target = (int) number(o, "target", 0, 100);
    p.light = bool(o, "light");
    if (o.has("salary")) p.salary = SalarySettings.parse(o.getJSONObject("salary"));
    JSONArray cs = o.getJSONArray("cards"), bs = o.getJSONArray("bills");
    if (cs.length() > 100000 || bs.length() > 100000) throw new JSONException("Plan çok büyük.");
    Set<Long> seen = new HashSet<>(), payments = new HashSet<>();
    for (int i = 0; i < cs.length(); i++) {
      JSONObject c = cs.getJSONObject(i);
      long account = number(c, "account", 1, Long.MAX_VALUE);
      if (!accounts.contains(account) || !seen.add(account))
        throw new JSONException("Geçersiz kart hesabı.");
      Card card =
          new Card(account, (int) number(c, "closing", 1, 31), (int) number(c, "due", 1, 31));
      if (c.has("updated") || c.has("statement") || c.has("current") || c.has("pending")) {
        card.statement = number(c, "statement", 0, 100_000_000_000L);
        card.current = number(c, "current", 0, 100_000_000_000L);
        card.pending = number(c, "pending", 0, 100_000_000_000L);
        Object date = c.get("updated");
        if (!(date instanceof String)) throw new JSONException("Geçersiz kart güncelleme tarihi.");
        card.updated = LocalDate.parse((String) date);
        if (card.updated.getYear() < 2000
            || card.updated.getYear() > 2100
            || !card.updated.toString().equals(date))
          throw new JSONException("Geçersiz kart güncelleme tarihi.");
      }
      p.cards.add(card);
    }
    seen.clear();
    JSONArray ls = o.optJSONArray("loans");
    if (o.has("loans") && ls == null) throw new JSONException("Geçersiz krediler.");
    if (ls != null)
      for (int i = 0; i < ls.length(); i++) {
        JSONObject x = ls.getJSONObject(i);
        Loan l = new Loan();
        l.id = number(x, "id", 1, Long.MAX_VALUE - 1);
        if (!seen.add(l.id)) throw new JSONException("Yinelenen kredi.");
        Object loanName = x.get("name");
        if (!(loanName instanceof String)) throw new JSONException("Geçersiz kredi adı.");
        l.name = ((String) loanName).trim();
        if (l.name.isEmpty() || l.name.length() > 60)
          throw new JSONException("Kredi adı 1–60 karakter olmalı.");
        l.principal = number(x, "principal", 1, 100_000_000_000L);
        l.installment = number(x, "installment", 1, 100_000_000_000L);
        l.count = (int) number(x, "count", 1, 360);
        if (l.total() < l.principal)
          throw new JSONException("Toplam geri ödeme anaparadan az olamaz.");
        l.account = number(x, "account", 1, Long.MAX_VALUE);
        l.category = number(x, "category", 1, Long.MAX_VALUE);
        if (!accounts.contains(l.account) || !"E".equals(kinds.get(l.category)))
          throw new JSONException("Kredi hesabı/kategorisi geçersiz.");
        l.first = LocalDate.parse(x.getString("first"));
        if (l.first.getYear() < 2000
            || l.first.plusMonths(l.count - 1).getYear() > 2100
            || !l.first.toString().equals(x.getString("first")))
          throw new JSONException("Kredi tarihleri 2000–2100 arasında olmalı.");
        p.loans.add(l);
      }
    seen.clear();
    Map<Long, Set<Integer>> loanSlots = new HashMap<>();
    for (int i = 0; i < bs.length(); i++) {
      JSONObject oBill = bs.getJSONObject(i);
      Bill b = new Bill();
      b.id = number(oBill, "id", 1, Long.MAX_VALUE - 1);
      b.loan = oBill.has("loan") ? number(oBill, "loan", 0, Long.MAX_VALUE - 1) : 0;
      b.installment = oBill.has("installment") ? (int) number(oBill, "installment", 0, 360) : 0;
      if (!seen.add(b.id)) throw new JSONException("Yinelenen fatura.");
      Object name = oBill.get("name");
      if (!(name instanceof String)
          || ((String) name).trim().isEmpty()
          || ((String) name).length() > 60)
        throw new JSONException("Fatura adı 1–60 karakter olmalı.");
      b.name = (String) name;
      b.amount = number(oBill, "amount", 1, 100_000_000_000L);
      b.account = number(oBill, "account", 1, Long.MAX_VALUE);
      b.category = number(oBill, "category", 1, Long.MAX_VALUE);
      if (!accounts.contains(b.account) || !"E".equals(kinds.get(b.category)))
        throw new JSONException("Fatura hesabı veya kategorisi geçersiz.");
      Object date = oBill.get("due");
      if (!(date instanceof String)) throw new JSONException("Geçersiz tarih.");
      b.due = LocalDate.parse((String) date);
      if (b.due.getYear() < 2000 || b.due.getYear() > 2100 || !b.due.toString().equals(date))
        throw new JSONException("Tarih 2000–2100 aralığında olmalı.");
      b.monthly = bool(oBill, "monthly");
      b.day = (int) number(oBill, "day", 1, 31);
      b.paid = number(oBill, "paid", 0, Long.MAX_VALUE);
      if (oBill.has("trackedPaid") && !oBill.isNull("trackedPaid")) {
        Object paidDate = oBill.get("trackedPaid");
        if (!(paidDate instanceof String) || b.loan == 0 || b.paid != 0)
          throw new JSONException("Geçersiz takip ödemesi.");
        b.trackedPaid = LocalDate.parse((String) paidDate);
        if (b.trackedPaid.getYear() < 2000
            || b.trackedPaid.getYear() > 2100
            || !b.trackedPaid.toString().equals(paidDate))
          throw new JSONException("Geçersiz takip ödeme tarihi.");
      }
      if (b.paid != 0) {
        Finance.Entry e = entries.get(b.paid);
        if (!payments.add(b.paid)
            || e == null
            || !"E".equals(e.kind)
            || e.amount != b.amount
            || e.accountId != b.account
            || e.categoryId != b.category) throw new JSONException("Fatura ödeme kaydı uyumsuz.");
      }
      if (b.loan != 0) {
        Loan l = p.loans.stream().filter(x -> x.id == b.loan).findFirst().orElse(null);
        if (l == null
            || b.installment < 1
            || b.installment > l.count
            || b.amount != l.installment
            || b.account != l.account
            || b.category != l.category
            || b.monthly
            || !b.due.equals(
                Finance.cycleStart(
                    java.time.YearMonth.from(l.first).plusMonths(b.installment - 1),
                    l.first.getDayOfMonth()))
            || !loanSlots.computeIfAbsent(l.id, k -> new HashSet<>()).add(b.installment))
          throw new JSONException("Kredi taksiti uyumsuz.");
      } else if (b.installment != 0) throw new JSONException("Eksik kredi bağı.");
      p.bills.add(b);
    }
    for (Loan l : p.loans)
      if (loanSlots.getOrDefault(l.id, Collections.emptySet()).size() != l.count)
        throw new JSONException("Eksik kredi taksitleri.");
    return p;
  }
}
