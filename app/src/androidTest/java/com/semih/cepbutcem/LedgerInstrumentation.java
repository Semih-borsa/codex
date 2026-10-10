package com.semih.cepbutcem;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import java.time.LocalDate;
import java.util.*;
import org.json.*;

/**
 * Tests on real Android SQLite, isolated from the user's database. No test-framework dependency.
 */
public class LedgerInstrumentation extends Instrumentation {
  private static final String DATABASE = "ledger-validation.db";
  private Context context;
  private Ledger db;
  private int failures;

  private interface Check {
    void run() throws Exception;
  }

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  private void equal(long expected, long actual) {
    if (expected != actual) throw new AssertionError("expected " + expected + ", got " + actual);
  }

  private void truth(boolean value) {
    if (!value) throw new AssertionError("condition false");
  }

  private Finance.Entry expense(long amount, String date) {
    return new Finance.Entry(
        0,
        "E",
        amount,
        LocalDate.parse(date),
        db.accounts().get(0).id,
        0,
        db.categories("E").get(0).id,
        "Test note");
  }

  private void rejects(Check check) throws Exception {
    boolean rejected = false;
    try {
      check.run();
    } catch (Exception e) {
      rejected = true;
    }
    truth(rejected);
  }

  @Override
  public void onStart() {
    context = getTargetContext();
    LinkedHashMap<String, Check> tests = new LinkedHashMap<>();
    tests.put(
        "freshDatabaseHasDefaults",
        () -> {
          equal(2, db.accounts().size());
          equal(15, db.categories(null).size());
          equal(0, db.entries().size());
        });
    tests.put(
        "entryPersistsEditsAndDeletes",
        () -> {
          Finance.Entry e = expense(12345, "2026-10-01");
          db.saveEntry(e);
          equal(1, db.entries().size());
          e.amount = 54321;
          db.saveEntry(e);
          equal(54321, db.entries().get(0).amount);
          db.deleteEntry(e.id);
          equal(0, db.entries().size());
        });
    tests.put(
        "accountAndCategoryRoundTrip",
        () -> {
          db.saveAccount(0, "Kart", -5000);
          equal(3, db.accounts().size());
          db.saveCategory(0, "Spor", "E");
          equal(16, db.categories(null).size());
        });
    tests.put(
        "transferReferencesAndAccounting",
        () -> {
          Finance.Entry e =
              new Finance.Entry(
                  0,
                  "T",
                  1000,
                  LocalDate.of(2026, 10, 1),
                  db.accounts().get(0).id,
                  db.accounts().get(1).id,
                  0,
                  "");
          db.saveEntry(e);
          equal(
              -1000,
              Finance.balance(db.accounts().get(0), db.entries(), LocalDate.of(2026, 10, 1)));
          equal(
              1000, Finance.balance(db.accounts().get(1), db.entries(), LocalDate.of(2026, 10, 1)));
          equal(0, Finance.total(db.entries(), "E"));
        });
    tests.put(
        "rejectsWrongCategoryAndMissingAccount",
        () -> {
          Finance.Entry e = expense(100, "2026-10-01");
          e.categoryId = db.categories("I").get(0).id;
          rejects(() -> db.saveEntry(e));
          e.categoryId = db.categories("E").get(0).id;
          e.accountId = 999;
          rejects(() -> db.saveEntry(e));
          equal(0, db.entries().size());
        });
    tests.put(
        "budgetsUpdateAndRemove",
        () -> {
          long cat = db.categories("E").get(0).id;
          db.budget(cat, 10000);
          db.budget(cat, 20000);
          equal(20000, db.budgets().get(cat));
          db.budget(cat, 0);
          equal(0, db.budgets().size());
        });
    tests.put(
        "recurringIsIdempotentAndClampsMonthEnd",
        () -> {
          db.addRecurring(expense(100, "2024-01-31"));
          equal(3, db.processRecurring(LocalDate.of(2024, 3, 31)));
          equal(0, db.processRecurring(LocalDate.of(2024, 3, 31)));
          truth(db.entries().stream().anyMatch(e -> e.date.equals(LocalDate.of(2024, 2, 29))));
          truth(db.entries().stream().anyMatch(e -> e.date.equals(LocalDate.of(2024, 3, 31))));
        });
    tests.put(
        "deletedOccurrenceDoesNotReappear",
        () -> {
          db.addRecurring(expense(100, "2026-10-01"));
          db.processRecurring(LocalDate.of(2026, 10, 10));
          db.deleteEntry(db.entries().get(0).id);
          equal(0, db.processRecurring(LocalDate.of(2026, 10, 10)));
          equal(0, db.entries().size());
        });
    tests.put(
        "stoppingRecurringPreservesHistory",
        () -> {
          db.addRecurring(expense(100, "2026-10-01"));
          db.processRecurring(LocalDate.of(2026, 10, 10));
          db.deleteRecurring(db.recurring().getJSONObject(0).getLong("id"));
          equal(1, db.entries().size());
          equal(0, db.processRecurring(LocalDate.of(2026, 11, 10)));
        });
    tests.put(
        "backupRestoreRoundTrip",
        () -> {
          db.saveEntry(expense(12345, "2026-10-01"));
          db.budget(db.categories("E").get(0).id, 50000);
          db.addRecurring(expense(100, "2026-11-01"));
          String backup = db.backup();
          db.saveEntry(expense(900, "2026-10-02"));
          db.restore(backup);
          equal(1, db.entries().size());
          equal(12345, db.entries().get(0).amount);
          equal(1, db.budgets().size());
          equal(1, db.recurring().length());
        });
    tests.put(
        "invalidBackupCannotEraseExistingData",
        () -> {
          db.saveEntry(expense(777, "2026-10-01"));
          JSONObject backup = new JSONObject(db.backup());
          backup.getJSONArray("entries").getJSONObject(0).put("account_id", 999);
          rejects(() -> db.restore(backup.toString()));
          equal(1, db.entries().size());
          equal(777, db.entries().get(0).amount);
        });
    tests.put(
        "duplicateOccurrenceRollsBackRestore",
        () -> {
          db.addRecurring(expense(100, "2026-10-01"));
          db.processRecurring(LocalDate.of(2026, 10, 10));
          JSONObject backup = new JSONObject(db.backup());
          JSONObject duplicate =
              new JSONObject(backup.getJSONArray("entries").getJSONObject(0).toString());
          duplicate.put("id", 999);
          backup.getJSONArray("entries").put(duplicate);
          rejects(() -> db.restore(backup.toString()));
          equal(1, db.entries().size());
        });
    tests.put(
        "csvPreservesNotesAndNeutralizesFormulas",
        () -> {
          Finance.Entry e = expense(12345, "2026-10-01");
          e.note = "=SUM(A1)\nquoted \"note\"";
          db.saveEntry(e);
          String csv = db.csv();
          truth(csv.contains("123,45"));
          truth(csv.contains("'=SUM(A1)"));
          truth(csv.contains("\"\"note\"\""));
          e.note = "  =SUM(A1)";
          db.saveEntry(e);
          truth(db.csv().contains("'  =SUM(A1)"));
        });
    tests.put(
        "futureRecurringDoesNotGenerateEarly",
        () -> {
          db.addRecurring(expense(100, "2026-11-01"));
          equal(0, db.processRecurring(LocalDate.of(2026, 10, 31)));
          equal(1, db.processRecurring(LocalDate.of(2026, 11, 1)));
        });
    tests.put(
        "legacySchemaUpgradePreservesEntries",
        () -> {
          Finance.Entry e = expense(12345, "2026-10-01");
          db.saveEntry(e);
          db.budget(e.categoryId, 50000);
          android.database.sqlite.SQLiteDatabase raw = db.getWritableDatabase();
          raw.execSQL("DROP TABLE planning");
          raw.setVersion(1);
          db.close();
          db = new Ledger(context, DATABASE);
          equal(2, db.getWritableDatabase().getVersion());
          equal(12345, db.entries().get(0).amount);
          equal(50000, db.budgets().get(e.categoryId));
          equal(1, db.planning().payday);
        });
    tests.put(
        "planningRoundTripAndLegacyBackup",
        () -> {
          Planning p = db.planning();
          p.payday = 15;
          p.target = 30;
          p.light = true;
          p.cards.add(new Planning.Card(db.accounts().get(1).id, 25, 5));
          db.savePlanning(p);
          String backup = db.backup();
          db.restore(backup);
          equal(15, db.planning().payday);
          truth(db.planning().light);
          equal(1, db.planning().cards.size());
          JSONObject old = new JSONObject(backup);
          old.put("version", 1);
          old.remove("planning");
          db.restore(old.toString());
          equal(1, db.planning().payday);
          equal(0, db.planning().cards.size());
        });
    tests.put(
        "billPaymentAtomicIdempotentAndUndo",
        () -> {
          Planning p = db.planning();
          Planning.Bill b = new Planning.Bill();
          b.id = 1;
          b.name = "Elektrik";
          b.amount = 50000;
          b.account = db.accounts().get(0).id;
          b.category = db.categories("E").get(0).id;
          b.due = LocalDate.of(2024, 1, 31);
          b.day = 31;
          b.monthly = true;
          p.bills.add(b);
          db.savePlanning(p);
          equal(0, db.entries().size());
          db.payBill(1, LocalDate.of(2024, 1, 31));
          equal(1, db.entries().size());
          equal(2, db.planning().bills.size());
          truth(db.planning().bills.get(1).due.equals(LocalDate.of(2024, 2, 29)));
          rejects(() -> db.payBill(1, LocalDate.of(2024, 1, 31)));
          equal(1, db.entries().size());
          long paid = db.planning().bills.get(0).paid;
          rejects(() -> db.deleteEntry(paid));
          db.restore(db.backup());
          equal(paid, db.planning().bills.get(0).paid);
          db.undoBill(1);
          equal(0, db.entries().size());
          equal(0, db.planning().bills.get(0).paid);
          equal(2, db.planning().bills.size());
          db.payBill(1, LocalDate.of(2024, 2, 1));
          equal(2, db.planning().bills.size());
          db.payBill(2, LocalDate.of(2024, 2, 29));
          truth(db.planning().bills.get(2).due.equals(LocalDate.of(2024, 3, 31)));
        });
    tests.put(
        "invalidPlanningRestoreCannotEraseData",
        () -> {
          db.saveEntry(expense(321, "2026-10-01"));
          JSONObject root = new JSONObject(db.backup());
          root.getJSONObject("planning").put("payday", 32);
          rejects(() -> db.restore(root.toString()));
          equal(321, db.entries().get(0).amount);
          Planning p = db.planning();
          p.cards.add(new Planning.Card(999, 10, 20));
          rejects(() -> db.savePlanning(p));
          equal(0, db.planning().cards.size());
        });
    tests.put(
        "cardPaymentDoesNotDoubleCountExpense",
        () -> {
          long bank = db.accounts().get(0).id, card = db.accounts().get(1).id;
          Finance.Entry shopping = expense(10000, "2026-10-01");
          shopping.accountId = card;
          db.saveEntry(shopping);
          db.saveEntry(
              new Finance.Entry(
                  0, "T", 10000, LocalDate.of(2026, 10, 2), bank, card, 0, "Kart ödemesi"));
          equal(10000, Finance.total(db.entries(), "E"));
          equal(0, Finance.total(db.entries(), "I"));
          equal(0, Finance.balance(db.accounts().get(1), db.entries(), LocalDate.of(2026, 10, 2)));
        });
    tests.put(
        "bankCardSnapshotRoundTripAndV2Compatibility",
        () -> {
          Planning.Card c = new Planning.Card(0, 10, 20);
          c.statement = 10000;
          c.current = 2500;
          c.pending = 999;
          c.updated = LocalDate.of(2026, 10, 10);
          db.saveCard(c, "Bonus", -12500);
          equal(3, db.accounts().size());
          equal(1, db.planning().cards.size());
          equal(13499, db.planning().cards.get(0).bankTotal());
          equal(0, db.entries().size());
          String backup = db.backup();
          equal(3, new JSONObject(backup).getLong("version"));
          db.restore(backup);
          equal(999, db.planning().cards.get(0).pending);
          truth(db.planning().cards.get(0).updated.equals(LocalDate.of(2026, 10, 10)));
          JSONObject old = new JSONObject(backup);
          old.put("version", 2);
          JSONObject card = old.getJSONObject("planning").getJSONArray("cards").getJSONObject(0);
          for (String key : Arrays.asList("statement", "current", "pending", "updated"))
            card.remove(key);
          db.restore(old.toString());
          truth(db.planning().cards.get(0).updated == null);
          equal(3, db.accounts().size());
        });
    tests.put(
        "invalidCardCreationRollsBackAccount",
        () -> {
          rejects(() -> db.saveCard(new Planning.Card(0, 32, 20), "Yanlış kart", 0));
          equal(2, db.accounts().size());
          equal(0, db.planning().cards.size());
          Planning.Card c = new Planning.Card(0, 10, 20);
          c.updated = LocalDate.now();
          c.pending = -1;
          rejects(() -> db.saveCard(c, "Yanlış borç", 0));
          equal(2, db.accounts().size());
        });
    tests.put(
        "malformedBankSnapshotRestorePreservesData",
        () -> {
          Planning.Card c = new Planning.Card(0, 10, 20);
          c.updated = LocalDate.now();
          c.statement = 100;
          db.saveCard(c, "Kart", 0);
          db.saveEntry(expense(321, "2026-10-01"));
          JSONObject root = new JSONObject(db.backup());
          root.getJSONObject("planning").getJSONArray("cards").getJSONObject(0).put("pending", -1);
          rejects(() -> db.restore(root.toString()));
          equal(321, db.entries().get(0).amount);
          equal(100, db.planning().cards.get(0).statement);
        });
    tests.put(
        "removeCardKeepsFinancialAccountAndTransactions",
        () -> {
          long id = db.accounts().get(1).id;
          db.saveCard(new Planning.Card(id, 10, 20), null, 0);
          Finance.Entry e = expense(789, "2026-10-01");
          e.accountId = id;
          db.saveEntry(e);
          db.removeCard(id);
          equal(0, db.planning().cards.size());
          equal(2, db.accounts().size());
          equal(789, db.entries().get(0).amount);
        });
    tests.put(
        "deleteAccountRemovesAllDependenciesWithoutDanglingReferences",
        () -> {
          long id = db.accounts().get(0).id, other = db.accounts().get(1).id;
          Finance.Entry e = expense(100, "2026-10-01");
          db.saveEntry(e);
          db.saveEntry(
              new Finance.Entry(0, "T", 200, LocalDate.of(2026, 10, 2), other, id, 0, "Transfer"));
          Finance.Entry kept = expense(300, "2026-10-01");
          kept.accountId = other;
          db.saveEntry(kept);
          e.date = LocalDate.of(2026, 11, 1);
          db.addRecurring(e);
          Planning p = db.planning();
          p.cards.add(new Planning.Card(id, 10, 20));
          Planning.Bill b = new Planning.Bill();
          b.id = 1;
          b.name = "Fatura";
          b.amount = 100;
          b.account = id;
          b.category = e.categoryId;
          b.due = e.date;
          b.day = 1;
          b.paid = e.id;
          p.bills.add(b);
          db.savePlanning(p);
          db.deleteAccount(id);
          equal(1, db.accounts().size());
          equal(1, db.entries().size());
          equal(300, db.entries().get(0).amount);
          equal(0, db.recurring().length());
          equal(0, db.planning().cards.size());
          equal(0, db.planning().bills.size());
          db.restore(db.backup());
          equal(300, db.entries().get(0).amount);
          rejects(() -> db.deleteAccount(other));
          equal(1, db.accounts().size());
          rejects(() -> db.deleteAccount(999));
          equal(300, db.entries().get(0).amount);
        });
    tests.put(
        "riskAnimationMovesThroughIntermediateValuesToExactTarget",
        () -> {
          java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
          java.util.concurrent.atomic.AtomicBoolean zero =
              new java.util.concurrent.atomic.AtomicBoolean();
          java.util.concurrent.atomic.AtomicBoolean middle =
              new java.util.concurrent.atomic.AtomicBoolean();
          java.util.concurrent.atomic.AtomicBoolean enabled =
              new java.util.concurrent.atomic.AtomicBoolean();
          runOnMainSync(
              () -> {
                enabled.set(android.animation.ValueAnimator.areAnimatorsEnabled());
                if (!enabled.get()) return;
                android.animation.ValueAnimator animation =
                    Design.ratioAnimator(
                        40,
                        true,
                        value -> {
                          if (value == 0) zero.set(true);
                          if (value > 0 && value < 40) middle.set(true);
                          if (value == 40) done.countDown();
                        });
                animation.start();
              });
          truth(enabled.get());
          truth(done.await(15, java.util.concurrent.TimeUnit.SECONDS));
          truth(zero.get());
          truth(middle.get());
        });
    tests.put(
        "riskAnimationDisabledUsesFinalValueWithoutAnimator",
        () -> {
          java.util.concurrent.atomic.AtomicReference<android.animation.ValueAnimator> animation =
              new java.util.concurrent.atomic.AtomicReference<>();
          double[] value = {-1};
          runOnMainSync(() -> animation.set(Design.ratioAnimator(150, false, x -> value[0] = x)));
          truth(animation.get() == null);
          truth(value[0] == 150);
        });
    int current = 0;
    StringBuilder summary = new StringBuilder();
    for (Map.Entry<String, Check> test : tests.entrySet()) {
      Bundle status = new Bundle();
      status.putString("class", getClass().getName());
      status.putString("test", test.getKey());
      status.putInt("numtests", tests.size());
      status.putInt("current", ++current);
      sendStatus(1, status);
      context.deleteDatabase(DATABASE);
      db = new Ledger(context, DATABASE);
      try {
        test.getValue().run();
        status.putString("stream", "PASS " + test.getKey() + "\n");
        sendStatus(0, status);
        summary.append("PASS ").append(test.getKey()).append('\n');
      } catch (Throwable e) {
        failures++;
        status.putString("stack", e.toString());
        status.putString("stream", "FAIL " + test.getKey() + ": " + e + "\n");
        sendStatus(-2, status);
        summary.append("FAIL ").append(test.getKey()).append(": ").append(e).append('\n');
      } finally {
        db.close();
        context.deleteDatabase(DATABASE);
      }
    }
    Bundle result = new Bundle();
    result.putString(
        "stream", summary + "\nTests: " + tests.size() + ", failures: " + failures + "\n");
    finish(failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
