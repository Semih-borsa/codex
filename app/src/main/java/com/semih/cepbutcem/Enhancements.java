package com.semih.cepbutcem;

import java.time.*;
import java.util.*;
import org.json.*;

/** Small planning entities; actual money remains in the indexed transaction ledger. */
public final class Enhancements {
  public final List<Template> templates = new ArrayList<>();
  public final List<Goal> goals = new ArrayList<>();
  public final List<Purchase> purchases = new ArrayList<>();

  public static final class Template {
    public long id, amount, account, category;
    public String name, kind, note = "";
  }

  public static final class Goal {
    public long id, account, target;
    public String name;
    public LocalDate deadline;
  }

  public static final class Purchase {
    public long id, account, amount;
    public String name;
    public LocalDate first;
    public int count, paid;

    public long total() {
      return Math.multiplyExact(amount, count);
    }

    public long remaining() {
      return Math.multiplyExact(amount, count - paid);
    }

    public LocalDate nextDue() {
      return due(paid);
    }

    public LocalDate due(int slot) {
      return Finance.cycleStart(YearMonth.from(first).plusMonths(slot), first.getDayOfMonth());
    }
  }

  public long nextTemplateId() {
    return templates.stream().mapToLong(x -> x.id).max().orElse(0) + 1;
  }

  public long nextGoalId() {
    return goals.stream().mapToLong(x -> x.id).max().orElse(0) + 1;
  }

  public long nextPurchaseId() {
    return purchases.stream().mapToLong(x -> x.id).max().orElse(0) + 1;
  }

  public JSONObject json() throws JSONException {
    JSONArray ts = new JSONArray(), gs = new JSONArray(), ps = new JSONArray();
    for (Template t : templates)
      ts.put(
          new JSONObject()
              .put("id", t.id)
              .put("name", t.name)
              .put("kind", t.kind)
              .put("amount", t.amount)
              .put("account", t.account)
              .put("category", t.category)
              .put("note", t.note));
    for (Goal g : goals)
      gs.put(
          new JSONObject()
              .put("id", g.id)
              .put("name", g.name)
              .put("account", g.account)
              .put("target", g.target)
              .put("deadline", g.deadline == null ? JSONObject.NULL : g.deadline.toString()));
    for (Purchase p : purchases)
      ps.put(
          new JSONObject()
              .put("id", p.id)
              .put("name", p.name)
              .put("account", p.account)
              .put("amount", p.amount)
              .put("first", p.first.toString())
              .put("count", p.count)
              .put("paid", p.paid));
    return new JSONObject().put("templates", ts).put("goals", gs).put("purchases", ps);
  }

  static long number(JSONObject o, String key, long min, long max) throws JSONException {
    Object v = o.get(key);
    if (!(v instanceof Number) || !v.toString().matches("[0-9]+"))
      throw new JSONException("Tam sayı gerekli: " + key);
    long n = o.getLong(key);
    if (n < min || n > max) throw new JSONException("Değer aralık dışında: " + key);
    return n;
  }

  static String text(JSONObject o, String key, int max, boolean blank) throws JSONException {
    Object v = o.get(key);
    if (!(v instanceof String)
        || ((String) v).length() > max
        || (!blank && ((String) v).trim().isEmpty()))
      throw new JSONException("Geçersiz metin: " + key);
    return ((String) v).trim();
  }

  static LocalDate date(String v) {
    LocalDate d = LocalDate.parse(v);
    if (d.getYear() < 2000 || d.getYear() > 2100 || !d.toString().equals(v))
      throw new IllegalArgumentException("Tarih 2000–2100 arasında olmalı.");
    return d;
  }

  private static JSONArray array(JSONObject o, String key) throws JSONException {
    JSONArray a = o.getJSONArray(key);
    if (a.length() > 1000) throw new JSONException("En fazla 1000 " + key + " kaydı olabilir.");
    return a;
  }

  public static Enhancements parse(
      JSONObject o, Set<Long> accounts, Map<Long, String> kinds, Set<Long> cards)
      throws JSONException {
    Enhancements e = new Enhancements();
    Set<Long> ids = new HashSet<>(), goalAccounts = new HashSet<>();
    JSONArray a = array(o, "templates");
    for (int i = 0; i < a.length(); i++) {
      JSONObject x = a.getJSONObject(i);
      Template t = new Template();
      t.id = number(x, "id", 1, Long.MAX_VALUE - 1);
      t.name = text(x, "name", 60, false);
      t.kind = text(x, "kind", 1, false);
      t.note = text(x, "note", 1000, true);
      t.amount = number(x, "amount", 1, 100_000_000_000L);
      t.account = number(x, "account", 1, Long.MAX_VALUE);
      t.category = number(x, "category", 1, Long.MAX_VALUE);
      if (!ids.add(t.id)
          || !accounts.contains(t.account)
          || !Arrays.asList("I", "E").contains(t.kind)
          || !t.kind.equals(kinds.get(t.category))
          || (t.kind.equals("I") && cards.contains(t.account)))
        throw new JSONException("Şablon hesabı/kategorisi geçersiz.");
      e.templates.add(t);
    }
    ids.clear();
    a = array(o, "goals");
    for (int i = 0; i < a.length(); i++) {
      JSONObject x = a.getJSONObject(i);
      Goal g = new Goal();
      g.id = number(x, "id", 1, Long.MAX_VALUE - 1);
      g.name = text(x, "name", 60, false);
      g.target = number(x, "target", 1, 100_000_000_000L);
      g.account = number(x, "account", 1, Long.MAX_VALUE);
      if (!x.isNull("deadline")) g.deadline = date(text(x, "deadline", 10, false));
      if (!ids.add(g.id)
          || !accounts.contains(g.account)
          || cards.contains(g.account)
          || !goalAccounts.add(g.account))
        throw new JSONException("Her hedef için ayrı bir banka/nakit hesabı seç.");
      e.goals.add(g);
    }
    ids.clear();
    a = array(o, "purchases");
    for (int i = 0; i < a.length(); i++) {
      JSONObject x = a.getJSONObject(i);
      Purchase p = new Purchase();
      p.id = number(x, "id", 1, Long.MAX_VALUE - 1);
      p.name = text(x, "name", 60, false);
      p.account = number(x, "account", 1, Long.MAX_VALUE);
      p.amount = number(x, "amount", 1, 100_000_000_000L);
      p.count = (int) number(x, "count", 1, 360);
      p.paid = (int) number(x, "paid", 0, p.count);
      p.first = date(text(x, "first", 10, false));
      if (!ids.add(p.id) || !cards.contains(p.account) || p.due(p.count - 1).getYear() > 2100)
        throw new JSONException("Kart veya taksit tarihleri geçersiz.");
      e.purchases.add(p);
    }
    return e;
  }
}
