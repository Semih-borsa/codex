package com.semih.cepbutcem;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.time.*;
import java.util.*;
import org.json.*;

/** Transactional commands for templates, goals, tracking and reversible ledger actions. */
public final class FeatureStore {
  private final Ledger ledger;

  public FeatureStore(Ledger ledger) {
    this.ledger = ledger;
  }

  private interface Command {
    void run() throws JSONException;
  }

  private void atomic(Command c) throws JSONException {
    SQLiteDatabase db = ledger.getWritableDatabase();
    db.beginTransaction();
    try {
      c.run();
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public static final class Snapshot {
    final JSONObject row;

    public Snapshot(String json) throws JSONException {
      row = new JSONObject(json);
    }

    public long id() throws JSONException {
      return row.getLong("id");
    }

    public String json() {
      return row.toString();
    }
  }

  public Snapshot snapshot(long id) throws JSONException {
    try (Cursor c =
        ledger
            .getReadableDatabase()
            .rawQuery("SELECT * FROM entries WHERE id=?", new String[] {Long.toString(id)})) {
      if (!c.moveToFirst()) throw new IllegalArgumentException("İşlem artık bulunamadı.");
      JSONObject row = new JSONObject();
      for (int i = 0; i < c.getColumnCount(); i++)
        row.put(
            c.getColumnName(i),
            c.isNull(i)
                ? JSONObject.NULL
                : c.getType(i) == Cursor.FIELD_TYPE_INTEGER ? c.getLong(i) : c.getString(i));
      return new Snapshot(row.toString());
    }
  }

  public Snapshot deleteWithUndo(long id) throws JSONException {
    Snapshot[] saved = {null};
    atomic(
        () -> {
          saved[0] = snapshot(id);
          ledger.deleteEntry(id);
        });
    return saved[0];
  }

  public void restoreDeleted(Snapshot saved) throws JSONException {
    atomic(
        () -> {
          JSONObject r = saved.row;
          Finance.Entry e =
              new Finance.Entry(
                  r.getLong("id"),
                  r.getString("kind"),
                  r.getLong("amount"),
                  LocalDate.parse(r.getString("date")),
                  r.getLong("account_id"),
                  r.isNull("target_id") ? 0 : r.getLong("target_id"),
                  r.isNull("category_id") ? 0 : r.getLong("category_id"),
                  r.getString("note"));
          ledger.validateReferences(e);
          ContentValues values = new ContentValues();
          for (String k :
              new String[] {
                "id",
                "kind",
                "amount",
                "date",
                "account_id",
                "target_id",
                "category_id",
                "note",
                "recurring_id",
                "occurrence"
              }) {
            if (r.isNull(k)) values.putNull(k);
            else if (Arrays.asList(
                    "id", "amount", "account_id", "target_id", "category_id", "recurring_id")
                .contains(k)) values.put(k, r.getLong(k));
            else values.put(k, r.getString(k));
          }
          ledger.getWritableDatabase().insertOrThrow("entries", null, values);
        });
  }

  public void undoAdded(Snapshot saved) throws JSONException {
    atomic(
        () -> {
          JSONObject current = snapshot(saved.id()).row;
          for (Iterator<String> i = saved.row.keys(); i.hasNext(); ) {
            String k = i.next();
            if (!String.valueOf(saved.row.get(k)).equals(String.valueOf(current.get(k))))
              throw new IllegalArgumentException(
                  "İşlem değiştirildi; geri almak yerine İşlemler'den düzenle.");
          }
          ledger.deleteEntry(saved.id());
        });
  }

  public void saveTemplate(Enhancements.Template t) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          if (t.id == 0) t.id = p.extras.nextTemplateId();
          p.extras.templates.removeIf(x -> x.id == t.id);
          p.extras.templates.add(t);
          ledger.savePlanning(p);
        });
  }

  public Snapshot applyTemplate(long id, LocalDate date) throws JSONException {
    Snapshot[] result = {null};
    atomic(
        () -> {
          Enhancements.Template t =
              ledger.planning().extras.templates.stream()
                  .filter(x -> x.id == id)
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Şablon bulunamadı."));
          Finance.Entry e =
              new Finance.Entry(
                  0,
                  t.kind,
                  t.amount,
                  date,
                  t.account,
                  0,
                  t.category,
                  t.note.isEmpty() ? t.name : t.note);
          ledger.saveEntry(e);
          result[0] = snapshot(e.id);
        });
    return result[0];
  }

  public void saveGoal(Enhancements.Goal g) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          if (g.id == 0) g.id = p.extras.nextGoalId();
          if (g.account == 0)
            g.account =
                ledger.saveAccount(
                    0, "Birikim · " + g.name.substring(0, Math.min(50, g.name.length())), 0);
          p.extras.goals.removeIf(x -> x.id == g.id);
          p.extras.goals.add(g);
          ledger.savePlanning(p);
        });
  }

  public void moveGoal(long id, long otherAccount, long amount, boolean deposit)
      throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          Enhancements.Goal g =
              p.extras.goals.stream()
                  .filter(x -> x.id == id)
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Hedef bulunamadı."));
          if (g.account == otherAccount || p.cardAccounts.contains(otherAccount))
            throw new IllegalArgumentException("Farklı bir banka/nakit hesabı seç.");
          long source = deposit ? otherAccount : g.account,
              target = deposit ? g.account : otherAccount;
          if (amount <= 0 || ledger.balances(LocalDate.now()).getOrDefault(source, 0L) < amount)
            throw new IllegalArgumentException(
                "Tutar pozitif olmalı ve kaynak hesap bakiyesini aşmamalı.");
          ledger.saveEntry(
              new Finance.Entry(
                  0, "T", amount, LocalDate.now(), source, target, 0, "Birikim · " + g.name));
        });
  }

  public void savePurchase(Enhancements.Purchase t) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          if (t.id == 0) t.id = p.extras.nextPurchaseId();
          p.extras.purchases.removeIf(x -> x.id == t.id);
          p.extras.purchases.add(t);
          ledger.savePlanning(p);
        });
  }

  public void markPurchase(long id, int expectedPaid, int delta) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          Enhancements.Purchase t =
              p.extras.purchases.stream()
                  .filter(x -> x.id == id)
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Taksit takibi bulunamadı."));
          if (t.paid != expectedPaid
              || Math.abs(delta) != 1
              || t.paid + delta < 0
              || t.paid + delta > t.count)
            throw new IllegalArgumentException("Taksit durumu değişti; ekranı yenile.");
          if (delta > 0 && t.nextDue().isAfter(LocalDate.now()))
            throw new IllegalArgumentException(
                "Gelecekteki taksit için ödeme tarihi gelmedi. Ödenen sayısını Düzenle'den"
                    + " güncelleyebilirsin.");
          t.paid += delta;
          ledger.savePlanning(p);
        });
  }

  public void remove(String type, long id) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          if (type.equals("template")) p.extras.templates.removeIf(x -> x.id == id);
          else if (type.equals("goal")) p.extras.goals.removeIf(x -> x.id == id);
          else if (type.equals("purchase")) p.extras.purchases.removeIf(x -> x.id == id);
          else throw new IllegalArgumentException("Bilinmeyen kayıt.");
          ledger.savePlanning(p);
        });
  }

  public void saveCardDates(long account, LocalDate close, LocalDate due) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          Planning.Card c =
              p.cards.stream()
                  .filter(x -> x.account == account)
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Kart takibi bulunamadı."));
          if (c.updated == null)
            throw new IllegalArgumentException("Önce bankadaki tutarları güncelle.");
          if (close.isAfter(c.updated))
            throw new IllegalArgumentException(
                "Bu kesim için önce banka tutarlarını güncelle; kesim tarihi özet güncellemesinden"
                    + " sonra olamaz.");
          LocalDate key = Finance.cycleStart(YearMonth.from(close), c.closing);
          for (int offset : new int[] {-1, 1}) {
            LocalDate candidate =
                Finance.cycleStart(YearMonth.from(close).plusMonths(offset), c.closing);
            if (Math.abs(java.time.temporal.ChronoUnit.DAYS.between(candidate, close))
                < Math.abs(java.time.temporal.ChronoUnit.DAYS.between(key, close))) key = candidate;
          }
          if (!c.updated.isBefore(Finance.cycleStart(YearMonth.from(key).plusMonths(1), c.closing)))
            throw new IllegalArgumentException(
                "Kesim tarihi bu banka özetinin dönemine ait olmalı.");
          c.dates.put(key, new LocalDate[] {close, due});
          while (c.dates.size() > 24) c.dates.pollFirstEntry();
          ledger.savePlanning(p);
        });
  }

  public void clearCardDates(long account) throws JSONException {
    atomic(
        () -> {
          Planning p = ledger.planning();
          Planning.Card c =
              p.cards.stream()
                  .filter(x -> x.account == account)
                  .findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("Kart bulunamadı."));
          c.dates.remove(c.nominalClosing());
          ledger.savePlanning(p);
        });
  }

  private long sum(String sql, String... args) {
    try (Cursor c = ledger.getReadableDatabase().rawQuery(sql, args)) {
      c.moveToFirst();
      return c.getLong(0);
    }
  }

  /** SQL sums and known future commitments; statement snapshots are not expenses. */
  public Insights.Forecast forecast(
      Planning p, YearMonth month, LocalDate today, List<Finance.Entry> aggregates)
      throws JSONException {
    LocalDate start = Finance.cycleStart(month, p.payday), end = Finance.cycleEnd(month, p.payday);
    LocalDate actualEnd = today.isBefore(end) ? today : end;
    long fixed =
        sum(
            "SELECT COALESCE(SUM(amount),0) FROM entries WHERE kind='E' AND recurring_id IS NOT"
                + " NULL AND date>=? AND date<=?",
            start.toString(),
            actualEnd.toString());
    long bills = 0;
    for (Planning.Bill b : p.bills) {
      if (!b.isPaid() && !b.due.isAfter(end)) bills = Math.addExact(bills, b.amount);
      if (b.paid != 0)
        fixed =
            Math.addExact(
                fixed,
                sum(
                    "SELECT COALESCE(SUM(amount),0) FROM entries WHERE id=? AND recurring_id IS"
                        + " NULL AND date>=? AND date<=?",
                    Long.toString(b.paid),
                    start.toString(),
                    actualEnd.toString()));
    }
    long futureExpense =
        sum(
            "SELECT COALESCE(SUM(amount),0) FROM entries WHERE kind='E' AND date>? AND date<=?",
            today.toString(),
            end.toString());
    long futureIncome =
        sum(
            "SELECT COALESCE(SUM(amount),0) FROM entries WHERE kind='I' AND date>? AND date<=?",
            today.toString(),
            end.toString());
    JSONArray recurring = ledger.recurring();
    for (int i = 0; i < recurring.length(); i++) {
      JSONObject r = recurring.getJSONObject(i);
      LocalDate next = LocalDate.parse(r.getString("next_date"));
      if (!next.isAfter(today)) {
        next = Finance.cycleStart(YearMonth.from(today), r.getInt("day"));
        if (!next.isAfter(today))
          next = Finance.cycleStart(YearMonth.from(today).plusMonths(1), r.getInt("day"));
      }
      while (!next.isAfter(end)) {
        long exists =
            sum(
                "SELECT COUNT(*) FROM entries WHERE recurring_id=? AND occurrence=?",
                Long.toString(r.getLong("id")),
                next.toString());
        if (exists == 0) {
          if (r.getString("kind").equals("I"))
            futureIncome = Math.addExact(futureIncome, r.getLong("amount"));
          else futureExpense = Math.addExact(futureExpense, r.getLong("amount"));
        }
        next = Finance.cycleStart(YearMonth.from(next).plusMonths(1), r.getInt("day"));
      }
    }
    Insights.Forecast f =
        Insights.forecast(
            aggregates,
            start,
            end,
            today,
            fixed,
            Math.addExact(futureExpense, bills),
            futureIncome);
    long liquid = 0;
    Set<Long> goals = new HashSet<>();
    for (Enhancements.Goal g : p.extras.goals) goals.add(g.account);
    for (Map.Entry<Long, Long> b : ledger.balances(today).entrySet())
      if (!p.cardAccounts.contains(b.getKey()) && !goals.contains(b.getKey()))
        liquid = Math.addExact(liquid, b.getValue());
    long cardReserve = 0;
    for (Planning.Card c : p.cards)
      if (c.updated != null && !c.statementDue().isAfter(end))
        cardReserve = Math.addExact(cardReserve, c.statement);
    f.reservedCards = cardReserve;
    f.liquid = liquid;
    long budget = f.income - f.spent - f.plannedExpense - Math.round(f.income * p.target / 100d);
    long cash = liquid - f.plannedExpense - cardReserve;
    f.dailyAvailable =
        f.remainingDays == 0 ? 0 : Math.max(0, Math.min(budget, cash)) / f.remainingDays;
    return f;
  }
}
