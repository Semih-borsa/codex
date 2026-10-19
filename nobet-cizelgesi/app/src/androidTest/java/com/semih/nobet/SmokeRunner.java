package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.*;
import android.graphics.pdf.PdfRenderer;
import android.os.*;
import java.io.*;
import java.time.*;

/**
 * Android storage/PDF/activity checks. Uses isolated storage; never overwrites the user's roster.
 */
public class SmokeRunner extends Instrumentation {
  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  interface Check {
    void run() throws Exception;
  }

  int passed = 0, failed = 0;

  void check(String name, Check check) {
    Bundle b = new Bundle();
    b.putString("test", name);
    try {
      check.run();
      passed++;
      b.putString("status", "PASS");
    } catch (Throwable e) {
      failed++;
      b.putString("status", "FAIL: " + e.getClass().getSimpleName() + " " + e.getMessage());
    }
    sendStatus(0, b);
  }

  void truth(boolean value) {
    if (!value) throw new AssertionError("condition failed");
  }

  android.widget.TextView findText(android.view.View view, String text) {
    if (view instanceof android.widget.TextView
        && ((android.widget.TextView) view).getText().toString().equals(text))
      return (android.widget.TextView) view;
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        android.widget.TextView found = findText(group.getChildAt(i), text);
        if (found != null) return found;
      }
    }
    return null;
  }

  @Override
  public void onStart() {
    Context target = getTargetContext();
    File isolated = new File(target.getCacheDir(), "instrumentation-isolated");
    isolated.mkdirs();
    Context context =
        new ContextWrapper(target) {
          @Override
          public File getFilesDir() {
            File f = new File(isolated, "files");
            f.mkdirs();
            return f;
          }

          @Override
          public File getCacheDir() {
            File f = new File(isolated, "cache");
            f.mkdirs();
            return f;
          }
        };
    State s = new State();
    for (int i = 0; i < 5; i++) s.people.add(new Person("p" + i, "Deneme " + i));
    LocalDate mon = LocalDate.of(2026, 10, 19);
    Week week = generate(s, mon);
    approve(s, week);
    check(
        "JSON round trip and atomic persistence",
        () -> {
          Store store = new Store(context);
          store.save(s);
          State restored = store.load();
          truth(restored.people.size() == 5 && restored.weeks.get(0).duties.size() == 12);
          store.save(restored);
          truth(store.load().weeks.size() == 1);
        });
    check(
        "PDF page renders",
        () -> {
          File pdf = PdfExport.create(context, s, week);
          truth(pdf.length() > 1000);
          try (ParcelFileDescriptor fd =
                  ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(fd)) {
            truth(renderer.getPageCount() == 1);
            try (PdfRenderer.Page page = renderer.openPage(0)) {
              truth(page.getWidth() == 842 && page.getHeight() == 595);
              android.graphics.Bitmap bitmap =
                  android.graphics.Bitmap.createBitmap(
                      842, 595, android.graphics.Bitmap.Config.ARGB_8888);
              page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
              bitmap.recycle();
            }
          }
        });
    check(
        "Mazeret serialization",
        () -> {
          s.leaves.add(new Leave("p0", ALL, mon, mon.plusDays(2), "Rapor"));
          State restored = Store.decode(Store.encode(s));
          truth(!available(restored, "p0", WORK, mon) && !available(restored, "p0", BRANCH, mon));
        });
    check(
        "PDF provider grants readable URI",
        () -> {
          File pdf = PdfExport.create(target, s, week);
          android.net.Uri uri =
              android.net.Uri.parse("content://com.semih.nobet.pdf/" + pdf.getName());
          try (InputStream in = target.getContentResolver().openInputStream(uri)) {
            byte[] header = new byte[4];
            truth(
                in.read(header) == 4
                    && new String(header, java.nio.charset.StandardCharsets.US_ASCII)
                        .equals("%PDF"));
          }
          try (android.database.Cursor cursor =
              target.getContentResolver().query(uri, null, null, null, null)) {
            truth(cursor != null && cursor.moveToFirst() && cursor.getString(0).endsWith(".pdf"));
          }
        });
    check(
        "PDF provider refuses writes and path traversal",
        () -> {
          File pdf = PdfExport.create(target, s, week);
          boolean writeRejected = false, pathRejected = false;
          try {
            target
                .getContentResolver()
                .openFileDescriptor(
                    android.net.Uri.parse("content://com.semih.nobet.pdf/" + pdf.getName()), "w");
          } catch (FileNotFoundException e) {
            writeRejected = true;
          }
          try {
            target
                .getContentResolver()
                .openInputStream(
                    android.net.Uri.parse("content://com.semih.nobet.pdf/roster-v1.json"));
          } catch (FileNotFoundException e) {
            pathRejected = true;
          }
          truth(writeRejected && pathRejected);
        });
    check(
        "Activity launches",
        () -> {
          Activity activity =
              startActivitySync(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          truth(activity != null);
          waitForIdleSync();
          runOnMainSync(activity::finish);
        });
    check(
        "Dark theme opens and persists",
        () -> {
          android.content.SharedPreferences prefs = target.getSharedPreferences("appearance", 0);
          boolean had = prefs.contains("theme");
          int old = prefs.getInt("theme", 0);
          Activity activity = null;
          try {
            truth(prefs.edit().putInt("theme", 2).commit());
            activity =
                startActivitySync(
                    new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity launched = activity;
            runOnMainSync(
                () -> {
                  android.util.TypedValue value = new android.util.TypedValue();
                  truth(
                      launched
                          .getTheme()
                          .resolveAttribute(android.R.attr.windowLightStatusBar, value, true));
                  truth(value.data == 0);
                });
            truth(prefs.getInt("theme", 0) == 2);
          } finally {
            if (activity != null) {
              Activity closing = activity;
              runOnMainSync(closing::finish);
            }
            if (had) prefs.edit().putInt("theme", old).commit();
            else prefs.edit().remove("theme").commit();
          }
        });
    check(
        "Version 1 records migrate without losing names or duties",
        () -> {
          org.json.JSONObject old = new org.json.JSONObject(Store.encode(s));
          old.put("version", 1);
          old.remove("places");
          State restored = Store.decode(old.toString());
          truth(
              restored.places.size() == 2
                  && restored.people.size() == 5
                  && restored.weeks.get(0).duties.size() == 12);
        });
    check(
        "Renamed and additional places persist",
        () -> {
          State custom = Store.decode(Store.encode(s));
          custom.place(WORK).name = "Merkez Bina";
          Place extra = new Place("extra", "Ek Şube", 5, 2);
          custom.places.add(extra);
          Store store = new Store(context);
          store.save(custom);
          State restored = store.load();
          truth(
              restored.placeName(WORK).equals("Merkez Bina")
                  && restored.place("extra").days == 5
                  && restored.place("extra").limit == 2);
          truth(restored.weeks.get(0).duties.size() == 12);
        });
    check(
        "All locations included in paginated PDF",
        () -> {
          State custom = Store.decode(Store.encode(s));
          for (int i = 0; i < 6; i++)
            custom.places.add(new Place("new" + i, "Ek Şube " + i, 31, 1));
          File pdf = PdfExport.create(context, custom, week);
          try (ParcelFileDescriptor fd =
                  ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(fd)) {
            truth(renderer.getPageCount() == 3);
          }
        });
    check(
        "Saved edit updates persisted statistics without deleting other weeks",
        () -> {
          State custom = Store.decode(Store.encode(s));
          custom.leaves.clear();
          approve(custom, generate(custom, mon.plusWeeks(1)));
          Week original = custom.weeks.get(0);
          Duty removed = original.duties.get(0);
          long[] before = totals(custom, removed.person, removed.place, mon.plusWeeks(3));
          Week revised = copy(original);
          revised.duties.removeIf(
              d ->
                  d.place.equals(removed.place)
                      && d.person.equals(removed.person)
                      && weekend(d.date));
          if (revised.duties.size() == original.duties.size())
            revised.duties.removeIf(
                d -> d.date.equals(removed.date) && d.place.equals(removed.place));
          approve(custom, revised);
          Store store = new Store(context);
          store.save(custom);
          State restored = store.load();
          truth(restored.weeks.size() == 2);
          long[] after = totals(restored, removed.person, removed.place, mon.plusWeeks(3));
          truth(after[0] + after[1] < before[0] + before[1]);
        });
    check(
        "Manual weekly exception persists without dropping other weeks",
        () -> {
          State custom = Store.decode(Store.encode(s));
          custom.leaves.clear();
          Week manual = new Week(mon.plusWeeks(2));
          manual.duties.add(new Duty(manual.monday, BRANCH, "p0"));
          manual.duties.add(new Duty(manual.monday.plusDays(1), BRANCH, "p0"));
          truth(!validate(custom, manual).isEmpty());
          manual.limitOverrides.add(BRANCH + "|p0");
          approve(custom, manual);
          Store store = new Store(context);
          store.save(custom);
          State restored = store.load();
          truth(restored.weeks.size() == 2 && validate(restored, restored.weeks.get(1)).isEmpty());
          truth(totals(restored, "p0", BRANCH, mon.plusWeeks(3))[0] >= 2);
        });
    check(
        "Unassigned location is a dash",
        () -> {
          truth(PdfExport.person(s, new Week(mon), mon, BRANCH).equals("—"));
        });
    check(
        "Settings and alphabetical statistics table open with blue white controls",
        () -> {
          Activity activity =
              startActivitySync(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          try {
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  android.view.View root = activity.getWindow().getDecorView();
                  android.widget.TextView settings = findText(root, "Ayarlar");
                  truth(
                      settings != null
                          && settings.getCurrentTextColor() == android.graphics.Color.WHITE);
                  truth(
                      settings.getBackgroundTintList().getDefaultColor()
                          == android.graphics.Color.rgb(31, 90, 137));
                  settings.performClick();
                  truth(
                      findText(activity.getWindow().getDecorView(), "Ayarlar • Görev yerleri")
                          != null);
                  findText(activity.getWindow().getDecorView(), "İstatistik").performClick();
                  truth(findText(activity.getWindow().getDecorView(), "A\nPersonel") != null);
                  truth(findText(activity.getWindow().getDecorView(), "B\nHafta içi") != null);
                  truth(findText(activity.getWindow().getDecorView(), "C\nHafta sonu") != null);
                });
          } finally {
            runOnMainSync(activity::finish);
          }
        });
    Bundle result = new Bundle();
    result.putInt("passed", passed);
    result.putInt("failed", failed);
    result.putString("stream", "Android checks: " + passed + " passed, " + failed + " failed\n");
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
