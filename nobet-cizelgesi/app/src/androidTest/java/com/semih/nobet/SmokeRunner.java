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
    Bundle result = new Bundle();
    result.putInt("passed", passed);
    result.putInt("failed", failed);
    result.putString("stream", "Android checks: " + passed + " passed, " + failed + " failed\n");
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
