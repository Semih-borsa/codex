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
