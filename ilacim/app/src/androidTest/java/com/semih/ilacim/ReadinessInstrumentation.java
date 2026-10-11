package com.semih.ilacim;

import android.app.*;
import android.content.*;
import android.database.sqlite.*;
import android.os.Bundle;
import java.io.File;
import java.time.*;
import java.util.*;
import org.json.*;

public class ReadinessInstrumentation extends Instrumentation {
  interface Check {
    void run() throws Exception;
  }

  Context isolated;
  Store s;

  public void onCreate(Bundle b) {
    super.onCreate(b);
    start();
  }

  void truth(boolean value) {
    if (!value) throw new AssertionError("Expected condition");
  }

  JSONObject medicine() throws Exception {
    return new JSONObject()
        .put("id", "box-1")
        .put("name", "Test product")
        .put("expiry", "12/2028")
        .put("stock", Rules.quantity("20"))
        .put("low", 5000)
        .put("unit", "adet")
        .put("category", "Test");
  }

  void rejects(JSONObject d) throws Exception {
    try {
      Store.validate(d);
    } catch (Exception e) {
      return;
    }
    throw new AssertionError("Invalid backup accepted");
  }

  public void onStart() {
    Context target = getTargetContext();
    isolated =
        new ContextWrapper(target) {
          public File getDatabasePath(String name) {
            return target.getDatabasePath("readiness-test.db");
          }

          public SQLiteDatabase openOrCreateDatabase(
              String n, int m, SQLiteDatabase.CursorFactory f) {
            return target.openOrCreateDatabase("readiness-test.db", m, f);
          }

          public SQLiteDatabase openOrCreateDatabase(
              String n,
              int m,
              SQLiteDatabase.CursorFactory f,
              android.database.DatabaseErrorHandler h) {
            return target.openOrCreateDatabase("readiness-test.db", m, f, h);
          }

          public SharedPreferences getSharedPreferences(String n, int mode) {
            return target.getSharedPreferences("readiness-" + n, mode);
          }
        };
    LinkedHashMap<String, Check> tests = new LinkedHashMap<>();
    tests.put(
        "sqlitePersistsExactHalfDose",
        () -> {
          s.list("meds").put(medicine());
          s.find("meds", "box-1").put("stock", Rules.quantity("20") - Rules.quantity("0,5"));
          s.save();
          s.close();
          s = new Store(isolated);
          truth(s.find("meds", "box-1").getInt("stock") == 19500);
        });
    tests.put(
        "backupRoundTripWithPlanAppointmentAndLog",
        () -> {
          s.list("meds").put(medicine());
          s.list("plans")
              .put(
                  new JSONObject()
                      .put("id", "plan-1")
                      .put("med", "box-1")
                      .put("dose", 500)
                      .put("times", "08:00, 20:00")
                      .put("start", "2026-10-01")
                      .put("end", "2028-12-01")
                      .put("active", true));
          s.list("appointments")
              .put(
                  new JSONObject()
                      .put("id", "appointment-1")
                      .put("doctor", "Test doctor")
                      .put("when", "2026-12-15T10:30"));
          s.list("logs")
              .put(
                  new JSONObject()
                      .put("id", Rules.slot("plan-1", LocalDate.of(2026, 10, 11), "08:00"))
                      .put("med", "box-1")
                      .put("date", "2026-10-11")
                      .put("status", "taken")
                      .put("amount", 500));
          JSONObject restored = new JSONObject(s.data.toString());
          Store.validate(restored);
          s.data = restored;
          s.save();
          s.close();
          s = new Store(isolated);
          truth(
              s.list("meds").length() == 1
                  && s.list("plans").length() == 1
                  && s.list("appointments").length() == 1
                  && s.list("logs").length() == 1);
        });
    tests.put(
        "rejectsInvalidExpiryWithoutReplacingExistingData",
        () -> {
          s.list("meds").put(medicine());
          s.save();
          JSONObject invalid = new JSONObject(s.data.toString());
          invalid.getJSONArray("meds").getJSONObject(0).put("expiry", "13/2026");
          rejects(invalid);
          truth(s.find("meds", "box-1").getString("expiry").equals("12/2028"));
        });
    tests.put(
        "rejectsOrphanPlan",
        () -> {
          JSONObject d = Store.empty();
          d.getJSONArray("plans")
              .put(
                  new JSONObject()
                      .put("id", "p")
                      .put("med", "absent")
                      .put("dose", 500)
                      .put("times", "08:00")
                      .put("start", "2026-01-01")
                      .put("end", "2026-12-31"));
          rejects(d);
        });
    tests.put(
        "rejectsDuplicateBoxes",
        () -> {
          JSONObject d = Store.empty();
          d.getJSONArray("meds").put(medicine()).put(medicine());
          rejects(d);
        });
    tests.put(
        "archivePreservesStockAndHistory",
        () -> {
          s.list("meds").put(medicine());
          s.find("meds", "box-1").put("archived", true);
          s.save();
          s.close();
          s = new Store(isolated);
          truth(s.find("meds", "box-1").getBoolean("archived"));
          truth(s.find("meds", "box-1").getInt("stock") == 20000);
        });
    tests.put(
        "keystoreEncryptsTokenAndBackupExcludesIt",
        () -> {
          Vault.save(isolated, "synthetic-test-key");
          truth(Vault.read(isolated).equals("synthetic-test-key"));
          String saved = isolated.getSharedPreferences("api", 0).getString("token", "");
          truth(!saved.contains("synthetic-test-key"));
          truth(!s.data.toString().contains("synthetic-test-key"));
          isolated.getSharedPreferences("api", 0).edit().clear().commit();
        });
    tests.put(
        "nestedApiMappingDoesNotInventCategory",
        () -> {
          JSONObject api = new JSONObject("{\"data\":{\"name\":\"Test medicine\"}}");
          truth(MainActivity.jsonPath(api, "data.name").equals("Test medicine"));
          truth(MainActivity.jsonPath(api, "data.category").isEmpty());
        });
    int current = 0, failed = 0;
    StringBuilder summary = new StringBuilder();
    for (Map.Entry<String, Check> t : tests.entrySet()) {
      Bundle status = new Bundle();
      status.putString("class", getClass().getName());
      status.putString("test", t.getKey());
      status.putInt("numtests", tests.size());
      status.putInt("current", ++current);
      sendStatus(1, status);
      target.deleteDatabase("readiness-test.db");
      s = new Store(isolated);
      try {
        t.getValue().run();
        status.putString("stream", "PASS " + t.getKey() + "\n");
        sendStatus(0, status);
        summary.append("PASS ").append(t.getKey()).append('\n');
      } catch (Throwable e) {
        failed++;
        status.putString("stack", e.toString());
        status.putString("stream", "FAIL " + t.getKey() + ": " + e + "\n");
        sendStatus(-2, status);
        summary.append("FAIL ").append(t.getKey()).append(':').append(e).append('\n');
      } finally {
        s.close();
        target.deleteDatabase("readiness-test.db");
      }
    }
    Bundle result = new Bundle();
    result.putString("stream", summary + "Tests: " + tests.size() + ", failures: " + failed + "\n");
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
