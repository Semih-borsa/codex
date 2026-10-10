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
          raw.execSQL("DROP TRIGGER entries_daily_insert");
          raw.execSQL("DROP TRIGGER entries_daily_update");
          raw.execSQL("DROP TRIGGER entries_daily_delete");
          raw.execSQL("DROP TABLE account_daily");
          raw.setVersion(1);
          db.close();
          db = new Ledger(context, DATABASE);
          equal(3, db.getWritableDatabase().getVersion());
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
          equal(4, new JSONObject(backup).getLong("version"));
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
    tests.put(
        "indexedPagesReportsAndLifetimeBalances",
        () -> {
          long source = db.accounts().get(0).id, target = db.accounts().get(1).id;
          Finance.Entry e = expense(100, "2026-10-10");
          db.saveEntry(e);
          Finance.Entry e2 = expense(250, "2026-10-10");
          e2.note = "İŞ ÖĞLE";
          db.saveEntry(e2);
          db.saveEntry(
              new Finance.Entry(
                  0,
                  "I",
                  1000,
                  LocalDate.of(2020, 1, 1),
                  source,
                  0,
                  db.categories("I").get(0).id,
                  ""));
          db.saveEntry(
              new Finance.Entry(0, "T", 200, LocalDate.of(2026, 10, 10), source, target, 0, ""));
          db.saveEntry(
              new Finance.Entry(
                  0,
                  "I",
                  9999,
                  LocalDate.of(2099, 1, 1),
                  source,
                  0,
                  db.categories("I").get(0).id,
                  ""));
          List<Finance.Entry> report =
              db.reportEntries(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
          equal(350, Finance.total(report, "E"));
          equal(2, report.stream().filter(x -> x.kind.equals("E")).findFirst().get().recordCount);
          equal(
              250, report.stream().filter(x -> x.kind.equals("E")).findFirst().get().largestAmount);
          equal(450, db.balances(LocalDate.of(2026, 10, 10)).get(source));
          equal(200, db.balances(LocalDate.of(2026, 10, 10)).get(target));
          Ledger.EntryPage page =
              db.entryPage(
                  LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "", 0, 0, "", 1, 0);
          equal(3, page.count);
          equal(1, page.rows.size());
          equal(350, page.expense);
          Ledger.EntryPage second =
              db.entryPage(
                  LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "", 0, 0, "", 1, 1);
          truth(page.rows.get(0).id != second.rows.get(0).id);
          equal(
              1,
              db.entryPage(
                      LocalDate.of(2026, 10, 1),
                      LocalDate.of(2026, 10, 31),
                      "E",
                      0,
                      0,
                      "iş öğle",
                      50,
                      0)
                  .count);
          equal(1, db.dayEntries(LocalDate.of(2026, 10, 10), 1, 0).size());
          equal(5, db.accountEntryCount(source));
          try (android.database.Cursor c =
              db.getReadableDatabase()
                  .rawQuery(
                      "EXPLAIN QUERY PLAN SELECT * FROM entries WHERE date>=? AND date<=? ORDER BY"
                          + " date DESC,id DESC LIMIT 50",
                      new String[] {"2026-10-01", "2026-10-31"})) {
            String plan = "";
            while (c.moveToNext()) plan += c.getString(3);
            truth(plan.contains("INDEX"));
          }
        });
    tests.put(
        "hundredThousandRowsRemainBounded",
        () -> {
          android.database.sqlite.SQLiteDatabase sql = db.getWritableDatabase();
          long account = db.accounts().get(0).id, cat = db.categories("E").get(0).id;
          long started = android.os.SystemClock.elapsedRealtime();
          sql.beginTransaction();
          try {
            sql.execSQL(
                "WITH RECURSIVE seq(x) AS (VALUES(0) UNION ALL SELECT x+1 FROM seq WHERE x<99999)"
                    + " INSERT INTO entries(kind,amount,date,account_id,category_id,note) SELECT"
                    + " 'E',100,date('2026-01-01','+'||(x%100)||' days'),?,?,'load fixture' FROM"
                    + " seq",
                new Object[] {account, cat});
            sql.setTransactionSuccessful();
          } finally {
            sql.endTransaction();
          }
          long queryStart = android.os.SystemClock.elapsedRealtime();
          List<Finance.Entry> report =
              db.reportEntries(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30));
          equal(100, report.size());
          equal(10000000, Finance.total(report, "E"));
          equal(1000, report.get(0).recordCount);
          Ledger.EntryPage page =
              db.entryPage(
                  LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30), "E", 0, 0, "", 50, 0);
          equal(50, page.rows.size());
          equal(100000, page.count);
          equal(-10000000, db.balances(LocalDate.of(2026, 5, 1)).get(account));
          android.util.Log.i(
              "CepBudgetValidation",
              "100000 fixture insertMs="
                  + (queryStart - started)
                  + ", queriesMs="
                  + (android.os.SystemClock.elapsedRealtime() - queryStart)
                  + ", reportRows="
                  + report.size()
                  + ", pageRows="
                  + page.rows.size());
        });
    tests.put(
        "loanSchedulesPreserveMonthEndAndBackup",
        () -> {
          Planning.Loan loan = new Planning.Loan();
          loan.name = "İhtiyaç";
          loan.principal = 30000;
          loan.installment = 12000;
          loan.count = 3;
          loan.first = LocalDate.of(2024, 1, 31);
          loan.account = db.accounts().get(0).id;
          loan.category = db.categories("E").get(0).id;
          db.addLoan(loan);
          Planning p = db.planning();
          equal(1, p.loans.size());
          equal(3, p.bills.size());
          truth(p.bills.get(1).due.equals(LocalDate.of(2024, 2, 29)));
          truth(p.bills.get(2).due.equals(LocalDate.of(2024, 3, 31)));
          db.payLoan(p.bills.get(0).id, LocalDate.of(2024, 1, 31), false);
          truth(db.planning().bills.get(0).isPaid());
          equal(0, db.entries().size());
          equal(0, db.balances(LocalDate.now()).get(loan.account));
          rejects(() -> db.payLoan(p.bills.get(0).id, LocalDate.of(2024, 1, 31), true));
          String backup = db.backup();
          db.restore(backup);
          truth(db.planning().bills.get(0).trackedPaid != null);
          equal(0, db.entries().size());
          db.undoBill(p.bills.get(0).id);
          truth(!db.planning().bills.get(0).isPaid());
          db.payLoan(p.bills.get(0).id, LocalDate.of(2024, 1, 31), true);
          equal(1, db.entries().size());
          equal(-12000, db.balances(LocalDate.now()).get(loan.account));
          backup = db.backup();
          db.restore(backup);
          equal(1, db.entries().size());
          db.undoBill(p.bills.get(0).id);
          equal(0, db.entries().size());
          db.payLoan(p.bills.get(1).id, LocalDate.of(2024, 2, 29), true);
          db.removeLoan(loan.id);
          equal(0, db.planning().loans.size());
          equal(0, db.planning().bills.size());
          equal(1, db.entries().size());
        });
    tests.put(
        "malformedLoanRestoreRollsBackAndLegacyV3StillWorks",
        () -> {
          Finance.Entry e = expense(321, "2026-10-01");
          db.saveEntry(e);
          JSONObject old = new JSONObject(db.backup());
          old.put("version", 3);
          old.getJSONObject("planning").remove("loans");
          db.restore(old.toString());
          equal(321, db.entries().get(0).amount);
          JSONObject broken = new JSONObject(db.backup());
          broken
              .getJSONObject("planning")
              .put(
                  "loans",
                  new JSONArray()
                      .put(
                          new JSONObject()
                              .put("id", 1)
                              .put("name", "Broken")
                              .put("principal", 100)
                              .put("installment", 100)
                              .put("count", 1)
                              .put("first", "2026-10-10")
                              .put("account", db.accounts().get(0).id)
                              .put("category", db.categories("E").get(0).id)));
          rejects(() -> db.restore(broken.toString()));
          equal(321, db.entries().get(0).amount);
        });
    tests.put(
        "v2MigrationPreservesEntriesAndAddsIndexes",
        () -> {
          db.saveEntry(expense(123, "2026-10-10"));
          db.getWritableDatabase().execSQL("DROP INDEX entries_date_id");
          db.getWritableDatabase().execSQL("DROP TRIGGER entries_daily_insert");
          db.getWritableDatabase().execSQL("DROP TRIGGER entries_daily_update");
          db.getWritableDatabase().execSQL("DROP TRIGGER entries_daily_delete");
          db.getWritableDatabase().execSQL("DROP TABLE account_daily");
          db.getWritableDatabase().setVersion(2);
          db.close();
          db = new Ledger(context, DATABASE);
          equal(123, db.entries().get(0).amount);
          equal(3, db.getReadableDatabase().getVersion());
          try (android.database.Cursor c =
              db.getReadableDatabase()
                  .rawQuery(
                      "SELECT name FROM sqlite_master WHERE type='index' AND"
                          + " name='entries_date_id'",
                      null)) {
            truth(c.moveToFirst());
          }
        });
    tests.put(
        "remindersExcludePaidAndUseSnapshotCycle",
        () -> {
          Planning p = new Planning();
          Planning.Bill pending = new Planning.Bill();
          pending.id = 1;
          pending.name = "Fatura";
          pending.amount = 100;
          pending.due = LocalDate.of(2026, 10, 11);
          p.bills.add(pending);
          Planning.Bill paid = new Planning.Bill();
          paid.name = "Ödendi";
          paid.amount = 200;
          paid.due = LocalDate.of(2026, 10, 1);
          paid.trackedPaid = LocalDate.of(2026, 10, 1);
          p.bills.add(paid);
          Planning.Card card = new Planning.Card(1, 5, 15);
          card.statement = 500;
          card.updated = LocalDate.of(2026, 9, 10);
          p.cards.add(card);
          List<Reminders.Due> due =
              Reminders.upcoming(p, db.accounts(), LocalDate.of(2026, 10, 10), 1);
          equal(2, due.size());
          truth(due.get(0).date.equals(LocalDate.of(2026, 9, 15)));
          equal(1, Reminders.upcoming(p, db.accounts(), LocalDate.of(2026, 10, 10), 0).size());
          java.time.ZonedDateTime now =
              java.time.ZonedDateTime.of(
                  2026, 10, 10, 9, 0, 0, 0, java.time.ZoneId.of("Europe/Istanbul"));
          truth(Reminders.nextCheck(now, 9).toLocalDate().equals(LocalDate.of(2026, 10, 11)));
        });

    tests.put(
        "dailyBalanceCacheFollowsEditsTransfersDatesAndRestore",
        () -> {
          Finance.Entry e = expense(100, "2026-10-01");
          db.saveEntry(e);
          e.amount = 200;
          e.date = LocalDate.of(2026, 10, 2);
          db.saveEntry(e);
          long a = db.accounts().get(0).id, b = db.accounts().get(1).id;
          Finance.Entry transfer =
              new Finance.Entry(0, "T", 300, LocalDate.of(2026, 10, 3), a, b, 0, "");
          db.saveEntry(transfer);
          db.saveEntry(
              new Finance.Entry(
                  0, "I", 500, LocalDate.of(2099, 1, 1), a, 0, db.categories("I").get(0).id, ""));
          equal(-200, db.balances(LocalDate.of(2026, 10, 2)).get(a));
          equal(-500, db.balances(LocalDate.of(2026, 10, 3)).get(a));
          equal(300, db.balances(LocalDate.of(2026, 10, 3)).get(b));
          transfer.accountId = b;
          transfer.targetId = a;
          db.saveEntry(transfer);
          equal(100, db.balances(LocalDate.of(2026, 10, 3)).get(a));
          equal(-300, db.balances(LocalDate.of(2026, 10, 3)).get(b));
          String backup = db.backup();
          db.restore(backup);
          for (Finance.Account account : db.accounts())
            equal(
                Finance.balance(account, db.entries(), LocalDate.now()),
                db.balances(LocalDate.now()).get(account.id));
          db.deleteEntry(transfer.id);
          equal(-200, db.balances(LocalDate.now()).get(a));
          equal(0, db.balances(LocalDate.now()).get(b));
          db.deleteEntry(e.id);
          equal(0, db.balances(LocalDate.now()).get(a));
        });
    int current = 0;
    tests.put(
        "salaryYearPeriodAndBackupRoundTrip",
        () -> {
          Planning p = db.planning();
          p.salary.current = "30000";
          p.salary.year = "2026";
          p.salary.firstYear = "2026";
          p.salary.july = true;
          p.salary.raises.put("2026-temmuz", "5");
          p.salary.raises.put("2027-ocak", "20");
          p.salary.inflation.put("2026-6", "-1,25");
          db.savePlanning(p);
          String backup = db.backup();
          db.restore(backup);
          SalarySettings salary = db.planning().salary;
          truth(salary.july);
          truth(salary.raises.get("2027-ocak").equals("20"));
          truth(salary.inflation.get("2026-6").equals("-1,25"));
          equal(2027, SalarySettings.year(salary.year) + 1);
          equal(0, db.entries().size());
        });
    tests.put(
        "invalidSalaryBackupLeavesLedgerUnchanged",
        () -> {
          db.saveEntry(expense(12345, "2026-10-10"));
          JSONObject root = new JSONObject(db.backup());
          root.getJSONObject("planning")
              .getJSONObject("salary")
              .getJSONObject("raises")
              .put("2026-ocak", "-100");
          String invalid = root.toString();
          rejects(() -> db.restore(invalid));
          equal(1, db.entries().size());
          root = new JSONObject(db.backup());
          root.getJSONObject("planning").remove("salary");
          db.restore(root.toString());
          truth(db.planning().salary.current.isEmpty());
        });
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
