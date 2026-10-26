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
    match = args == null ? "" : args.getString("match", "");
    start();
  }

  interface Check {
    void run() throws Exception;
  }

  int passed = 0, failed = 0, skipped = 0;
  String match = "";

  void check(String name, Check check) {
    if (!name.contains(match)) {
      skipped++;
      return;
    }
    Bundle b = new Bundle();
    b.putString("test", name);
    try {
      check.run();
      passed++;
      b.putString("status", "PASS");
    } catch (Throwable e) {
      failed++;
      StringWriter trace = new StringWriter();
      e.printStackTrace(new PrintWriter(trace));
      b.putString("trace", trace.toString());
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

  private void checkStatisticsBounds(android.view.View view) {
    truth(!(view instanceof android.widget.HorizontalScrollView));
    if ("statistics-table".equals(view.getTag())
        || "holiday-statistics-table".equals(view.getTag())) {
      android.view.ViewGroup table = (android.view.ViewGroup) view;
      truth(table.getWidth() > 0);
      for (int i = 0; i < table.getChildCount(); i++) {
        android.view.ViewGroup row = (android.view.ViewGroup) table.getChildAt(i);
        for (int j = 0; j < row.getChildCount(); j++) {
          android.view.View cell = row.getChildAt(j);
          truth(cell.getWidth() > 0 && cell.getLeft() >= 0 && cell.getRight() <= table.getWidth());
        }
      }
    }
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) checkStatisticsBounds(group.getChildAt(i));
    }
  }

  void waitForFrames(Activity activity) throws InterruptedException {
    java.util.concurrent.CountDownLatch frame = new java.util.concurrent.CountDownLatch(1);
    runOnMainSync(
        () ->
            activity
                .getWindow()
                .getDecorView()
                .postOnAnimation(
                    () -> activity.getWindow().getDecorView().postOnAnimation(frame::countDown)));
    truth(frame.await(15, java.util.concurrent.TimeUnit.SECONDS));
    waitForIdleSync();
  }

  Activity launchActivity(Intent intent) throws InterruptedException {
    Activity activity = startActivitySync(intent);
    waitForFrames(activity);
    waitForFocus(activity);
    return activity;
  }

  private void waitForFocus(Activity activity) throws InterruptedException {
    long deadline = SystemClock.uptimeMillis() + 15000;
    boolean[] focused = {false};
    while (SystemClock.uptimeMillis() < deadline) {
      runOnMainSync(() -> focused[0] = activity.hasWindowFocus());
      if (focused[0]) return;
      Thread.sleep(100);
    }
    truth(focused[0]);
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
    CloudChecks.run(this, context);
    DistributionUiChecks.run(this, context, isolated);
    TodayChecks.run(this, target, isolated);
    check(
        "HolidayFull confirmation cancel save clear persistence legacy and separate normal stats",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          State sample = new State();
          sample.people.add(new Person("holiday-full-a", "Semih"));
          sample.people.add(new Person("holiday-full-b", "Serdar"));
          LocalDate day = LocalDate.of(2026, 10, 29);
          Week w = new Week(monday(day));
          w.duties.add(new Duty(day, WORK, "holiday-full-a"));
          sample.weeks.add(w);
          java.lang.reflect.Field stateField = MainActivity.class.getDeclaredField("state");
          stateField.setAccessible(true);
          java.lang.reflect.Field storeField = MainActivity.class.getDeclaredField("store");
          storeField.setAccessible(true);
          java.lang.reflect.Field selectedField = MainActivity.class.getDeclaredField("selected");
          selectedField.setAccessible(true);
          java.lang.reflect.Method render = MainActivity.class.getDeclaredMethod("render");
          render.setAccessible(true);
          java.lang.reflect.Method open =
              MainActivity.class.getDeclaredMethod("holidayWorkDialog", LocalDate.class);
          open.setAccessible(true);
          android.app.AlertDialog[] dialog = {null};
          try {
            runOnMainSync(
                () -> {
                  try {
                    stateField.set(activity, sample);
                    storeField.set(activity, new Store(context));
                    selectedField.set(activity, w.monday);
                    render.invoke(activity);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            truth(
                activity.getWindow().getDecorView().findViewWithTag("holiday-day-" + day) != null);
            truth(
                Holidays.statistics(sample, day, false).stream()
                    .allMatch(r -> java.util.Arrays.stream(r.counts).allMatch(n -> n == 0)));
            runOnMainSync(
                () -> {
                  try {
                    dialog[0] = (android.app.AlertDialog) open.invoke(activity, day);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            runOnMainSync(
                () -> {
                  android.widget.CheckBox checkbox =
                      dialog[0]
                          .getWindow()
                          .getDecorView()
                          .findViewWithTag("holiday-work-" + WORK + "-holiday-full-b");
                  truth(!checkbox.isChecked());
                  checkbox.setChecked(true);
                  dialog[0].getButton(-2).performClick();
                });
            truth(sample.holidayWork.isEmpty());
            runOnMainSync(
                () -> {
                  try {
                    dialog[0] = (android.app.AlertDialog) open.invoke(activity, day);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            runOnMainSync(
                () -> {
                  ((android.widget.CheckBox)
                          dialog[0]
                              .getWindow()
                              .getDecorView()
                              .findViewWithTag("holiday-work-" + WORK + "-holiday-full-b"))
                      .setChecked(true);
                  dialog[0].getButton(-1).performClick();
                });
            truth(
                sample.holidayWork.size() == 1
                    && sample.holidayWork.get(0).person.equals("holiday-full-b"));
            State loaded = new Store(context).load();
            truth(loaded.holidayWork.size() == 1);
            truth(
                Holidays.statistics(loaded, day, false).stream()
                        .filter(r -> r.person.id.equals("holiday-full-b"))
                        .findFirst()
                        .get()
                        .counts[0]
                    == 2);
            truth(
                Roster.statistics(loaded, day, false).stream()
                        .filter(r -> r.person.id.equals("holiday-full-a"))
                        .findFirst()
                        .get()
                        .counts[0]
                    == 1);
            truth(
                Roster.statistics(loaded, day, false).stream()
                        .filter(r -> r.person.id.equals("holiday-full-b"))
                        .findFirst()
                        .get()
                        .counts[0]
                    == 0);
            org.json.JSONObject legacy = new org.json.JSONObject(Store.encode(sample));
            legacy.remove("holidayWork");
            truth(Store.decode(legacy.toString()).holidayWork.isEmpty());
            java.io.File pdf = PdfExport.createHolidayStatistics(context, loaded, day);
            truth(pdf.length() > 0);
            runOnMainSync(
                () -> {
                  try {
                    dialog[0] = (android.app.AlertDialog) open.invoke(activity, day);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            runOnMainSync(
                () -> {
                  android.widget.CheckBox checkbox =
                      dialog[0]
                          .getWindow()
                          .getDecorView()
                          .findViewWithTag("holiday-work-" + WORK + "-holiday-full-b");
                  truth(checkbox.isChecked());
                  checkbox.setChecked(false);
                  dialog[0].getButton(-1).performClick();
                });
            truth(new Store(context).load().holidayWork.isEmpty());
            truth(sample.weeks.get(0).duties.size() == 1);
          } finally {
            runOnMainSync(
                () -> {
                  if (dialog[0] != null) dialog[0].dismiss();
                  activity.finish();
                });
          }
        });

    check(
        "Weekend saved chronological dropdown opens chosen week without writing roster",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          State sample = new State();
          sample.people.add(new Person("weekend-test", "Semih"));
          Week first = new Week(LocalDate.of(2024, 8, 26));
          first.duties.add(new Duty(first.monday.plusDays(5), WORK, "weekend-test"));
          first.duties.add(new Duty(first.monday.plusDays(6), WORK, "weekend-test"));
          sample.weeks.add(first);
          java.lang.reflect.Field field = MainActivity.class.getDeclaredField("state");
          field.setAccessible(true);
          java.lang.reflect.Field selected = MainActivity.class.getDeclaredField("selected");
          selected.setAccessible(true);
          java.lang.reflect.Method render = MainActivity.class.getDeclaredMethod("render");
          render.setAccessible(true);
          java.lang.reflect.Method open = MainActivity.class.getDeclaredMethod("weekendList");
          open.setAccessible(true);
          final android.app.AlertDialog[] dialog = new android.app.AlertDialog[1];
          try {
            runOnMainSync(
                () -> {
                  try {
                    field.set(activity, sample);
                    render.invoke(activity);
                    dialog[0] = (android.app.AlertDialog) open.invoke(activity);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            truth(dialog[0].isShowing());
            truth(dialog[0].getListView().getAdapter().getCount() == 1);
            truth(
                dialog[0]
                    .getListView()
                    .getAdapter()
                    .getItem(0)
                    .toString()
                    .equals("31 Ağustos–1 Eylül 2024 · Semih"));
            runOnMainSync(() -> dialog[0].getListView().performItemClick(null, 0, 0));
            truth(selected.get(activity).equals(first.monday));
            truth(sample.weeks.size() == 1 && sample.weeks.get(0) == first);
          } finally {
            runOnMainSync(activity::finish);
          }
        });
    check(
        "Holiday overrides persist and legacy data enables only automatic calendar",
        () -> {
          State sample = new State();
          Holidays.Holiday automatic = Holidays.on(sample, LocalDate.of(2026, 10, 29)).get(0);
          Holidays.put(
              sample,
              new Holidays.Holiday(
                  automatic.id, "Düzeltilmiş", LocalDate.of(2027, 2, 1), 1, false));
          Holidays.put(sample, new Holidays.Holiday("custom:test", "Kurum Tatili", mon, 1, true));
          Store storage = new Store(context);
          storage.save(sample);
          State read = storage.load();
          truth(read.holidays.size() == 2 && Holidays.units(read, mon) == 1);
          truth(Holidays.units(read, LocalDate.of(2026, 10, 29)) == 0);
          truth(Holidays.units(read, LocalDate.of(2027, 2, 1)) == 0);
          Holidays.Holiday custom =
              read.holidays.stream().filter(h -> h.id.equals("custom:test")).findFirst().get();
          truth(custom.name.equals("Kurum Tatili") && custom.units == 1);
          org.json.JSONObject legacy = new org.json.JSONObject(Store.encode(read));
          legacy.remove("holidays");
          State old = Store.decode(legacy.toString());
          truth(old.holidays.isEmpty() && Holidays.units(old, LocalDate.of(2026, 10, 29)) == 2);
          org.json.JSONObject duplicate = new org.json.JSONObject(Store.encode(read));
          duplicate
              .getJSONArray("holidays")
              .put(duplicate.getJSONArray("holidays").getJSONObject(0));
          boolean rejected = false;
          try {
            Store.decode(duplicate.toString());
          } catch (org.json.JSONException expected) {
            rejected = true;
          }
          truth(rejected);
        });
    check(
        "Holiday PDF renders separate place tables and decimal half days",
        () -> {
          State sample = new State();
          sample.people.add(new Person("holiday", "Deneme"));
          sample.place(WORK).name = "Merkez";
          sample.place(BRANCH).name = "Çarşı";
          Week w = new Week(mon);
          w.duties.add(new Duty(mon, WORK, "holiday"));
          w.duties.add(new Duty(mon, BRANCH, "holiday"));
          sample.weeks.add(w);
          sample.holidayWork.add(new Duty(mon, WORK, "holiday"));
          sample.holidayWork.add(new Duty(mon, BRANCH, "holiday"));
          sample.hiddenPlaces.add(BRANCH);
          Holidays.put(sample, new Holidays.Holiday("custom:pdf", "Kurum Tatili", mon, 1, true));
          truth(Holidays.statistics(sample, mon, false).get(0).counts[0] == 1);
          truth(PdfExport.holidayLabels(sample).equals(java.util.Arrays.asList("Merkez", "Çarşı")));
          File pdf = PdfExport.createHolidayStatistics(context, sample, mon);
          try (ParcelFileDescriptor fd =
                  ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(fd)) {
            truth(renderer.getPageCount() == 1);
            try (PdfRenderer.Page page = renderer.openPage(0)) {
              android.graphics.Bitmap bitmap =
                  android.graphics.Bitmap.createBitmap(
                      page.getWidth(), page.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
              page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
              bitmap.recycle();
            }
          }
          truth(PdfExport.createHolidayStatistics(context, new State(), mon).length() > 0);
        });
    check(
        "Holiday calendar editor validates cancels and persists confirmed half day with live"
            + " statistics",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          android.app.AlertDialog[] calendar = {null}, editor = {null};
          State sample = new State();
          sample.people.add(new Person("holiday-ui", "Deneme Personel"));
          Week w = new Week(mon);
          w.duties.add(new Duty(mon, WORK, "holiday-ui"));
          sample.weeks.add(w);
          try {
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    java.lang.reflect.Field field = MainActivity.class.getDeclaredField("state");
                    field.setAccessible(true);
                    field.set(activity, sample);
                    java.lang.reflect.Field store = MainActivity.class.getDeclaredField("store");
                    store.setAccessible(true);
                    store.set(activity, new Store(context));
                    java.lang.reflect.Method open =
                        MainActivity.class.getDeclaredMethod("holidayCalendar");
                    open.setAccessible(true);
                    calendar[0] = (android.app.AlertDialog) open.invoke(activity);
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            Thread.sleep(1000);
            truth(
                calendar[0].getWindow().getDecorView().findViewWithTag("holiday-add").getHeight()
                    > 0);
            android.graphics.Bitmap image = getUiAutomation().takeScreenshot();
            truth(image != null);
            try (FileOutputStream stream =
                new FileOutputStream(new File(isolated, "holiday-calendar.png"))) {
              image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            }
            image.recycle();
            runOnMainSync(
                () -> {
                  try {
                    calendar[0].dismiss();
                    calendar[0] = null;
                    java.lang.reflect.Method edit =
                        MainActivity.class.getDeclaredMethod(
                            "holidayDialog", Holidays.Holiday.class, int.class, Runnable.class);
                    edit.setAccessible(true);
                    editor[0] =
                        (android.app.AlertDialog)
                            edit.invoke(activity, null, 2026, (Runnable) () -> {});
                    editor[0].getButton(-1).performClick();
                    android.widget.EditText name =
                        editor[0].getWindow().getDecorView().findViewWithTag("holiday-name");
                    truth(name.getError() != null && sample.holidays.isEmpty());
                    editor[0].getButton(-2).performClick();
                    editor[0] = null;
                    truth(sample.holidays.isEmpty());
                    Holidays.Holiday custom =
                        new Holidays.Holiday("custom:ui", "Kurum Tatili", mon, 2, true);
                    editor[0] =
                        (android.app.AlertDialog)
                            edit.invoke(activity, custom, 2026, (Runnable) () -> {});
                    android.widget.Spinner duration =
                        editor[0].getWindow().getDecorView().findViewWithTag("holiday-duration");
                    duration.setSelection(1);
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(() -> editor[0].getButton(-1).performClick());
            waitForIdleSync();
            boolean clicked = false;
            long deadline = SystemClock.uptimeMillis() + 10000;
            while (!clicked && SystemClock.uptimeMillis() < deadline) {
              android.view.accessibility.AccessibilityNodeInfo root =
                  getUiAutomation().getRootInActiveWindow();
              if (root != null) {
                for (android.view.accessibility.AccessibilityNodeInfo node :
                    root.findAccessibilityNodeInfosByText("Onayla")) {
                  if ("Onayla"
                      .equalsIgnoreCase(node.getText() == null ? "" : node.getText().toString()))
                    clicked =
                        node.performAction(
                            android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                  node.recycle();
                }
                root.recycle();
              }
              if (!clicked) Thread.sleep(250);
            }
            truth(clicked);
            boolean[] saved = {false};
            deadline = SystemClock.uptimeMillis() + 10000;
            while (!saved[0] && SystemClock.uptimeMillis() < deadline) {
              runOnMainSync(
                  () ->
                      saved[0] = sample.holidays.size() == 1 && sample.holidays.get(0).units == 1);
              if (!saved[0]) Thread.sleep(100);
            }
            truth(saved[0]);
            waitForIdleSync();
            waitForFrames(activity);
            truth(new Store(context).load().holidays.get(0).units == 1);
            truth(Holidays.statistics(sample, mon, false).get(0).counts[0] == 0);
            truth(Roster.statistics(sample, mon, false).get(0).counts[0] == 1);
            runOnMainSync(
                () -> activity.getWindow().getDecorView().findViewWithTag("nav-3").performClick());
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  android.view.View rootView = activity.getWindow().getDecorView();
                  truth(rootView.findViewWithTag("holiday-statistics-table") != null);
                  truth(findText(rootView, "Resmî Tatilleri PDF Olarak Paylaş") != null);
                });
          } finally {
            runOnMainSync(
                () -> {
                  if (calendar[0] != null) calendar[0].dismiss();
                  if (editor[0] != null) editor[0].dismiss();
                  activity.finish();
                });
          }
        });
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
              truth(page.getWidth() == 700 && page.getHeight() == 324);
              android.graphics.Bitmap bitmap =
                  android.graphics.Bitmap.createBitmap(
                      700, 324, android.graphics.Bitmap.Config.ARGB_8888);
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
              launchActivity(
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
                launchActivity(
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
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          try {
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  android.view.View root = activity.getWindow().getDecorView();
                  android.widget.TextView settings = findText(root, "Ayarlar");
                  truth(settings != null && settings.getCompoundDrawables()[1] != null);
                  settings.performClick();
                  android.widget.TextView selectedTab =
                      (android.widget.TextView)
                          activity.getWindow().getDecorView().findViewWithTag("nav-4");
                  truth(
                      selectedTab.isSelected()
                          && selectedTab.getCurrentTextColor() == android.graphics.Color.WHITE);
                  truth(
                      findText(activity.getWindow().getDecorView(), "Hafta Sonu Nöbet Sırası")
                          != null);
                  truth(
                      findText(activity.getWindow().getDecorView(), "+ Sıraya Yeni Personel Ekle")
                          != null);
                  truth(
                      findText(activity.getWindow().getDecorView(), "Ayarlar • Görev Yerleri")
                          != null);
                  findText(activity.getWindow().getDecorView(), "İstatistik").performClick();
                  truth(findText(activity.getWindow().getDecorView(), "Personel") != null);
                  truth(findText(activity.getWindow().getDecorView(), "Hafta İçi") != null);
                  truth(findText(activity.getWindow().getDecorView(), "Hafta Sonu") != null);
                  truth(
                      findText(
                              activity.getWindow().getDecorView(),
                              "İstatistikleri PDF Olarak Paylaş")
                          != null);
                });
          } finally {
            runOnMainSync(activity::finish);
          }
        });
    check(
        "Original place day selections persist and drive generation",
        () -> {
          State custom = Store.decode(Store.encode(s));
          custom.leaves.clear();
          custom.place(WORK).days = 1;
          custom.place(BRANCH).days = 96;
          Store store = new Store(context);
          store.save(custom);
          State restored = store.load();
          Week plan = generate(restored, mon.plusWeeks(1));
          truth(restored.place(WORK).days == 1 && restored.place(BRANCH).days == 96);
          truth(plan.duties.stream().filter(d -> d.place.equals(WORK)).count() == 1);
          truth(
              plan.duties.stream()
                  .filter(d -> d.place.equals(BRANCH))
                  .allMatch(d -> weekend(d.date)));
          truth(validate(restored, plan).isEmpty());
        });
    check(
        "Deleted place history and flags survive reopening",
        () -> {
          State custom = Store.decode(Store.encode(s));
          custom.leaves.clear();
          custom.places.add(new Place("archive", "Arşiv Şube", 31, 1));
          custom.weeks.clear();
          Week saved = new Week(mon);
          saved.duties.add(new Duty(mon, "archive", "p0"));
          saved.duties.add(new Duty(mon.plusDays(1), "archive", "p1"));
          approve(custom, saved);
          deletePlace(custom, "archive", mon);
          Store store = new Store(context);
          store.save(custom);
          State restored = store.load();
          truth(restored.place("archive").deleted && restored.weeks.get(0).duties.size() == 1);
          truth(totals(restored, "p0", "archive", mon)[0] == 1);
          truth(
              generate(restored, mon.plusWeeks(1)).duties.stream()
                  .noneMatch(d -> d.place.equals("archive")));
        });
    check(
        "Hidden places persist without changing statistics or duties",
        () -> {
          State custom = Store.decode(Store.encode(s));
          custom.hiddenPlaces.add(BRANCH);
          Store store = new Store(context);
          store.save(custom);
          State restored = store.load();
          truth(restored.hiddenPlaces.contains(BRANCH));
          truth(displayPlaces(restored, restored.weeks.get(0)).size() == 1);
          truth(restored.weeks.get(0).duties.size() == 12);
          truth(statisticPlaces(restored).size() == 1);
        });
    check(
        "Hidden PDF columns removed while records stay intact",
        () -> {
          State custom = Store.decode(Store.encode(s));
          for (int i = 0; i < 4; i++) {
            Place p = new Place("hidden" + i, "Gizli Şube " + i, 31, 1);
            custom.places.add(p);
            custom.hiddenPlaces.add(p.id);
          }
          custom.hiddenPlaces.add(BRANCH);
          File pdf = PdfExport.create(context, custom, custom.weeks.get(0));
          try (ParcelFileDescriptor fd =
                  ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(fd)) {
            truth(renderer.getPageCount() == 1);
          }
          truth(custom.weeks.get(0).duties.size() == 12);
          truth(custom.places.size() == 6);
        });
    check(
        "Statistics fit narrow screens with twenty places and preserve every branch",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          try {
            waitForFrames(activity);
            waitForFocus(activity);
            runOnMainSync(
                () -> {
                  try {
                    State sample = new State();
                    for (Person p : s.people) sample.people.add(p);
                    for (int i = 2; i < 20; i++)
                      sample.places.add(new Place("fit" + i, "Uzun Şube Adı " + i, 31, 1));
                    java.lang.reflect.Field field = MainActivity.class.getDeclaredField("state");
                    field.setAccessible(true);
                    field.set(activity, sample); // In memory only; never saves over device records.
                    setWeekendOrder(sample, java.util.Arrays.asList("p4", "p2", "p0", "p3", "p1"));
                    findText(activity.getWindow().getDecorView(), "Ayarlar").performClick();
                    truth(
                        findText(
                                activity.getWindow().getDecorView(),
                                "1. Sıra • " + sample.name("p4"))
                            != null);
                    truth(
                        findText(
                                activity.getWindow().getDecorView(),
                                "2. Sıra • " + sample.name("p2"))
                            != null);

                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  findText(activity.getWindow().getDecorView(), "İstatistik").performClick();
                  android.view.View root = activity.getWindow().getDecorView();
                  for (int i = 1; i < 20; i++)
                    truth(
                        findText(root, i == 1 ? s.placeName(BRANCH) : "Uzun Şube Adı " + i)
                            != null);
                  truth(findText(root, "A\nPersonel") == null);
                });
            waitForFrames(activity);
            // Each viewport is a separate UI operation so focus/input can be delivered between
            // synthetic stress layouts on the software-emulated device.
            for (int width : new int[] {280, 320, 360, 600}) {
              runOnMainSync(
                  () -> {
                    android.view.View root = activity.getWindow().getDecorView();
                    float density = activity.getResources().getDisplayMetrics().density;
                    root.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            (int) (width * density), android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            (int) (800 * density), android.view.View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                    checkStatisticsBounds(root);
                    root.requestLayout();
                  });
              waitForFrames(activity);
            }
            android.graphics.Bitmap screenshot = getUiAutomation().takeScreenshot();
            if (screenshot != null) {
              try (FileOutputStream stream =
                  new FileOutputStream(new File(isolated, "statistics-fit.png"))) {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
              }
              screenshot.recycle();
            }
          } finally {
            runOnMainSync(activity::finish);
          }
        });
    check(
        "Statistics PDF renders empty table without explanatory text",
        () -> {
          File pdf = PdfExport.createStatistics(context, new State(), mon);
          try (ParcelFileDescriptor fd =
                  ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(fd)) {
            truth(renderer.getPageCount() == 1);
            try (PdfRenderer.Page page = renderer.openPage(0)) {
              truth(page.getHeight() < 120);
              android.graphics.Bitmap bitmap =
                  android.graphics.Bitmap.createBitmap(
                      page.getWidth(), page.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
              page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
              bitmap.recycle();
            }
          }
        });
    check(
        "Statistics PDF paginates people branches and periods including hidden branches",
        () -> {
          State sample = new State();
          for (int i = 0; i < 19; i++)
            sample.people.add(new Person("pdf" + i, "Deneme Uzun Personel Adı " + i));
          for (int i = 2; i < 7; i++)
            sample.places.add(new Place("pdfplace" + i, "Uzun İsimli Yeni Görev Yeri " + i, 31, 1));
          sample.hiddenPlaces.add(BRANCH);
          Week saved = new Week(mon);
          saved.duties.add(new Duty(mon, WORK, "pdf0"));
          saved.duties.add(new Duty(mon.plusDays(1), BRANCH, "pdf1"));
          sample.weeks.add(saved);
          truth(PdfExport.statisticsLabels(sample).size() == 8);
          truth(statistics(sample, mon, false).stream().mapToLong(row -> row.counts[0]).sum() == 1);
          truth(statistics(sample, mon, true).stream().mapToLong(row -> row.counts[2]).sum() == 1);
          File pdf = PdfExport.createStatistics(context, sample, mon);
          try (ParcelFileDescriptor fd =
                  ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY);
              PdfRenderer renderer = new PdfRenderer(fd)) {
            truth(renderer.getPageCount() == 8);
            for (int i = 0; i < renderer.getPageCount(); i++) {
              try (PdfRenderer.Page page = renderer.openPage(i)) {
                android.graphics.Bitmap bitmap =
                    android.graphics.Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        android.graphics.Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                bitmap.recycle();
              }
            }
          }
        });
    check(
        "Statistics PDF sharing URI is readable and read only",
        () -> {
          File pdf = PdfExport.createStatistics(target, s, mon);
          android.net.Uri uri =
              android.net.Uri.parse("content://com.semih.nobet.pdf/" + pdf.getName());
          try (InputStream stream = target.getContentResolver().openInputStream(uri)) {
            truth(stream.read() == '%');
          }
          boolean rejected = false;
          try {
            target.getContentResolver().openFileDescriptor(uri, "w");
          } catch (FileNotFoundException expected) {
            rejected = true;
          }
          truth(rejected);
        });
    check(
        "Weekend order and revision survive storage including saved cursor",
        () -> {
          State sample = Store.decode(Store.encode(s));
          setWeekendOrder(sample, java.util.Arrays.asList("p4", "p2", "p0", "p3", "p1"));
          Week next = generate(sample, mon.plusWeeks(1));
          approve(sample, next);
          Store isolatedStore = new Store(context);
          isolatedStore.save(sample);
          State restored = isolatedStore.load();
          truth(restored.weekendOrder.equals(sample.weekendOrder));
          truth(restored.rotationRevision == 1);
          truth(restored.weeks.get(restored.weeks.size() - 1).rotationRevision == 1);
          Week following = generate(restored, mon.plusWeeks(2));
          truth(
              PdfExport.person(restored, following, following.monday.plusDays(5), WORK)
                  .equals(restored.name("p2")));
        });
    check(
        "Old records without order fields keep original sequence and cursor",
        () -> {
          org.json.JSONObject legacy = new org.json.JSONObject(Store.encode(s));
          legacy.remove("weekendOrder");
          legacy.remove("rotationRevision");
          org.json.JSONArray weeks = legacy.getJSONArray("weeks");
          for (int i = 0; i < weeks.length(); i++)
            weeks.getJSONObject(i).remove("rotationRevision");
          State restored = Store.decode(legacy.toString());
          truth(restored.weekendOrder.isEmpty() && restored.rotationRevision == 0);
          truth(weekendOrder(restored).get(0).id.equals("p0"));
          Week next = generate(restored, mon.plusWeeks(1));
          truth(
              PdfExport.person(restored, next, next.monday.plusDays(5), WORK)
                  .equals(restored.name("p1")));
        });
    check(
        "Weekend arrow controls move staff without renaming or changing saved records and can"
            + " cancel",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          try {
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  try {
                    State sample = new State();
                    for (int i = 0; i < 5; i++)
                      sample.people.add(new Person("arrow" + i, "Personel " + i));
                    Week recorded = generate(sample, mon);
                    approve(sample, recorded);
                    java.lang.reflect.Field field = MainActivity.class.getDeclaredField("state");
                    field.setAccessible(true);
                    field.set(activity, sample);
                    findText(activity.getWindow().getDecorView(), "Ayarlar").performClick();
                    android.view.View root = activity.getWindow().getDecorView();
                    truth(!root.findViewWithTag("weekend-up-arrow0").isEnabled());
                    truth(!root.findViewWithTag("weekend-down-arrow4").isEnabled());
                    root.findViewWithTag("weekend-down-arrow0").performClick();
                    root = activity.getWindow().getDecorView();
                    truth(findText(root, "1. Sıra • Personel 1") != null);
                    truth(findText(root, "2. Sıra • Personel 0") != null);
                    truth(findText(root, "Sırayı Kaydet") != null);
                    root.findViewWithTag("weekend-up-arrow0").performClick();
                    root = activity.getWindow().getDecorView();
                    truth(findText(root, "1. Sıra • Personel 0") != null);
                    root.findViewWithTag("weekend-up-arrow4").performClick();
                    root = activity.getWindow().getDecorView();
                    truth(findText(root, "4. Sıra • Personel 4") != null);
                    Bundle bundle = new Bundle();
                    ((MainActivity) activity).onSaveInstanceState(bundle);
                    truth(bundle.getStringArrayList("weekendOrderDraft").get(3).equals("arrow4"));
                    truth(sample.weekendOrder.isEmpty() && sample.rotationRevision == 0);
                    truth(
                        sample.people.size() == 5
                            && sample.people.get(4).name.equals("Personel 4"));
                    truth(sample.weeks.get(0) == recorded && recorded.duties.size() == 12);
                    findText(root, "Sıra Değişikliklerini İptal Et").performClick();
                    root = activity.getWindow().getDecorView();
                    truth(findText(root, "5. Sıra • Personel 4") != null);
                    truth(findText(root, "Sırayı Kaydet") == null);
                    root.findViewWithTag("weekend-down-arrow0").performClick();
                    sample.people.get(4).active = false;
                    findText(activity.getWindow().getDecorView(), "Ayarlar").performClick();
                    root = activity.getWindow().getDecorView();
                    truth(root.findViewWithTag("weekend-up-arrow4") == null);
                    truth(findText(root, "Sırayı Kaydet") == null);
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
          } finally {
            runOnMainSync(activity::finish);
          }
        });
    check(
        "Multiple recurring and dated constraints survive storage independently",
        () -> {
          State sample = new State();
          sample.people.add(new Person("weekly", "Deneme"));
          sample.leaves.add(new Leave("weekly", BRANCH, mon, LocalDate.MAX, "Haftalık", 5));
          sample.leaves.add(new Leave("weekly", WORK, mon, mon.plusWeeks(4), "Haftalık", 2));
          sample.leaves.add(new Leave("weekly", ALL, mon.plusDays(4), mon.plusDays(4), "Rapor"));
          Store isolatedStore = new Store(context);
          isolatedStore.save(sample);
          State restored = isolatedStore.load();
          truth(restored.leaves.size() == 3 && restored.leaves.get(0).end.equals(LocalDate.MAX));
          truth(!available(restored, "weekly", BRANCH, mon.plusWeeks(10)));
          truth(available(restored, "weekly", WORK, mon.plusWeeks(10)));
          truth(!available(restored, "weekly", WORK, mon.plusDays(1)));
          truth(!available(restored, "weekly", BRANCH, mon.plusDays(4)));
          restored.leaves.remove(0);
          truth(
              restored.leaves.size() == 2 && !available(restored, "weekly", WORK, mon.plusDays(1)));
        });
    check(
        "Legacy constraints without recurrence retain every date in their range",
        () -> {
          State sample = new State();
          sample.people.add(new Person("legacy", "Deneme"));
          sample.leaves.add(new Leave("legacy", ALL, mon, mon.plusDays(6), "Rapor"));
          org.json.JSONObject json = new org.json.JSONObject(Store.encode(sample));
          json.getJSONArray("leaves").getJSONObject(0).remove("days");
          State restored = Store.decode(json.toString());
          truth(restored.leaves.get(0).days == 0);
          for (int i = 0; i < 7; i++) truth(!available(restored, "legacy", WORK, mon.plusDays(i)));
          truth(available(restored, "legacy", WORK, mon.plusDays(7)));
        });
    check(
        "Weekly restrictions hide dates and add independent rows with plus",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          android.app.AlertDialog[] dialog = new android.app.AlertDialog[1];
          State[] sampleState = new State[1];
          try {
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  try {
                    State sample = new State();
                    sampleState[0] = sample;
                    sample.people.add(new Person("dialog", "Deneme Personel"));
                    java.lang.reflect.Field field = MainActivity.class.getDeclaredField("state");
                    field.setAccessible(true);
                    field.set(activity, sample);
                    java.lang.reflect.Method method =
                        MainActivity.class.getDeclaredMethod(
                            "leaveDialog",
                            Leave.class,
                            String.class,
                            LocalDate.class,
                            LocalDate.class,
                            String.class);
                    method.setAccessible(true);
                    dialog[0] =
                        (android.app.AlertDialog)
                            method.invoke(activity, null, ALL, mon, mon.plusDays(6), "Rapor");
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  android.view.View root = dialog[0].getWindow().getDecorView();
                  android.widget.CheckBox weekly =
                      (android.widget.CheckBox) root.findViewWithTag("leave-weekly");
                  truth(weekly != null && !weekly.isChecked());
                  weekly.performClick();
                  truth(weekly.isChecked());
                  android.widget.CheckBox monday =
                      (android.widget.CheckBox) root.findViewWithTag("leave-day-0");
                  android.widget.CheckBox wednesday =
                      (android.widget.CheckBox) root.findViewWithTag("leave-day-2");
                  truth(
                      ((android.view.View) monday.getParent()).getVisibility()
                          == android.view.View.VISIBLE);
                  monday.performClick();
                  wednesday.performClick();
                  truth(monday.isChecked() && wednesday.isChecked());
                  truth(
                      !((android.widget.CheckBox) root.findViewWithTag("leave-day-6")).isChecked());
                  truth(
                      root.findViewWithTag("leave-start-date").getVisibility()
                          == android.view.View.GONE);
                  truth(
                      root.findViewWithTag("leave-end-date").getVisibility()
                          == android.view.View.GONE);
                  truth(findText(root, "Bitiş Tarihi Olsun") == null);
                  android.view.ViewGroup first =
                      (android.view.ViewGroup) root.findViewWithTag("weekly-restriction-row");
                  android.view.ViewGroup rows = (android.view.ViewGroup) first.getParent();
                  truth(rows.getChildCount() == 1);
                  root.findViewWithTag("weekly-add-row").performClick();
                  truth(rows.getChildCount() == 2);
                  dialog[0]
                      .getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                      .performClick();
                  truth(sampleState[0].leaves.isEmpty());
                  android.view.ViewGroup second = (android.view.ViewGroup) rows.getChildAt(1);
                  android.widget.CheckBox friday =
                      (android.widget.CheckBox) second.findViewWithTag("leave-day-4");
                  friday.performClick();
                  truth(friday.isChecked() && monday.isChecked() && wednesday.isChecked());
                  truth(
                      !((android.widget.CheckBox) second.findViewWithTag("leave-day-0"))
                          .isChecked());
                  second.findViewWithTag("weekly-remove-row").performClick();
                  truth(rows.getChildCount() == 1 && monday.isChecked() && wednesday.isChecked());
                  weekly.performClick();
                  truth(
                      root.findViewWithTag("leave-start-date").getVisibility()
                          == android.view.View.VISIBLE);
                  truth(
                      root.findViewWithTag("leave-end-date").getVisibility()
                          == android.view.View.VISIBLE);
                  weekly.performClick();
                  truth(monday.isChecked() && wednesday.isChecked());
                });
          } finally {
            runOnMainSync(
                () -> {
                  if (dialog[0] != null) dialog[0].dismiss();
                  activity.finish();
                });
          }
        });
    check(
        "Personnel selection retains visible day and offset after rebuilding the roster",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          android.app.AlertDialog[] dialog = new android.app.AlertDialog[1];
          State sample = new State();
          for (int i = 0; i < 5; i++) sample.people.add(new Person("scroll" + i, "Deneme " + i));
          Week saved = generate(sample, mon);
          approve(sample, saved);
          java.lang.reflect.Field stateField = MainActivity.class.getDeclaredField("state");
          stateField.setAccessible(true);
          java.lang.reflect.Field selectedField = MainActivity.class.getDeclaredField("selected");
          selectedField.setAccessible(true);
          java.lang.reflect.Field scrollField = MainActivity.class.getDeclaredField("pageScroll");
          scrollField.setAccessible(true);
          java.lang.reflect.Method render = MainActivity.class.getDeclaredMethod("render");
          render.setAccessible(true);
          java.lang.reflect.Method edit =
              MainActivity.class.getDeclaredMethod("edit", LocalDate.class, String.class);
          edit.setAccessible(true);
          String anchor = "roster-day-" + mon.plusDays(3);
          int[] offset = new int[1];
          try {
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    stateField.set(activity, sample);
                    selectedField.set(activity, mon);
                    render.invoke(activity);
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    android.widget.ScrollView scroll =
                        (android.widget.ScrollView) scrollField.get(activity);
                    android.view.View day = scroll.findViewWithTag(anchor);
                    scroll.scrollTo(0, day.getTop() + 12);
                    truth(scroll.getScrollY() > 0);
                    offset[0] = day.getTop() - scroll.getScrollY();
                    dialog[0] =
                        (android.app.AlertDialog) edit.invoke(activity, mon.plusDays(3), BRANCH);
                    String assigned =
                        saved.duties.stream()
                            .filter(d -> d.date.equals(mon.plusDays(3)) && d.place.equals(BRANCH))
                            .findFirst()
                            .get()
                            .person;
                    int option = 0;
                    for (int i = 0; i < sample.people.size(); i++)
                      if (sample.people.get(i).id.equals(assigned)) option = i + 1;
                    dialog[0].getListView().performItemClick(null, option, option);
                    dialog[0].dismiss();
                    dialog[0] = null;
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    android.widget.ScrollView scroll =
                        (android.widget.ScrollView) scrollField.get(activity);
                    android.view.View day = scroll.findViewWithTag(anchor);
                    truth(scroll.getScrollY() > 0);
                    truth(Math.abs(day.getTop() - scroll.getScrollY() - offset[0]) <= 2);
                    dialog[0] =
                        (android.app.AlertDialog) edit.invoke(activity, mon.plusDays(3), BRANCH);
                    dialog[0].getListView().performItemClick(null, 0, 0);
                    dialog[0].dismiss();
                    dialog[0] = null;
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    android.widget.ScrollView scroll =
                        (android.widget.ScrollView) scrollField.get(activity);
                    android.view.View day = scroll.findViewWithTag(anchor);
                    truth(
                        scroll.getScrollY() > 0
                            && Math.abs(day.getTop() - scroll.getScrollY() - offset[0]) <= 2);
                    truth(
                        ((android.widget.TextView)
                                day.findViewWithTag("assignment-" + mon.plusDays(3) + "-" + BRANCH))
                            .getText()
                            .toString()
                            .equals("—"));
                    findText(activity.getWindow().getDecorView(), "Personel").performClick();
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    truth(
                        ((android.widget.ScrollView) scrollField.get(activity)).getScrollY() == 0);
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
          } finally {
            runOnMainSync(
                () -> {
                  if (dialog[0] != null) dialog[0].dismiss();
                  activity.finish();
                });
          }
        });
    check(
        "Confirmed simultaneous assignments survive storage and absent legacy field grants nothing",
        () -> {
          State sample = new State();
          sample.people.add(new Person("multi", "Deneme"));
          sample.places.add(new Place("multi-place", "Ek Şube", 31, 0));
          Week w = new Week(mon);
          w.duties.add(new Duty(mon, BRANCH, "multi"));
          w.duties.add(new Duty(mon, "multi-place", "multi"));
          w.simultaneousOverrides.add(simultaneousKey(mon, "multi", BRANCH, "multi-place"));
          approve(sample, w);
          Store isolatedStore = new Store(context);
          isolatedStore.save(sample);
          State restored = isolatedStore.load();
          truth(restored.weeks.get(0).simultaneousOverrides.equals(w.simultaneousOverrides));
          truth(validate(restored, restored.weeks.get(0)).isEmpty());
          org.json.JSONObject legacy = new org.json.JSONObject(Store.encode(sample));
          legacy.getJSONArray("weeks").getJSONObject(0).remove("simultaneousOverrides");
          State noGrant = Store.decode(legacy.toString());
          truth(noGrant.weeks.get(0).simultaneousOverrides.isEmpty());
          truth(!validate(noGrant, noGrant.weeks.get(0)).isEmpty());
        });
    check(
        "Manual simultaneous and weekly-limit confirmation can cancel or approve without changing"
            + " saved week",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          android.app.AlertDialog[] dialog = new android.app.AlertDialog[1];
          try {
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  try {
                    State sample = new State();
                    sample.people.add(new Person("multi", "Deneme"));
                    sample.places.add(new Place("multi-place", "Ek Şube", 31, 1));
                    Week original = new Week(mon);
                    original.duties.add(new Duty(mon, BRANCH, "multi"));
                    original.duties.add(new Duty(mon.plusDays(1), "multi-place", "multi"));
                    approve(sample, original);
                    java.lang.reflect.Field stateField =
                        MainActivity.class.getDeclaredField("state");
                    stateField.setAccessible(true);
                    stateField.set(activity, sample);
                    java.lang.reflect.Field selectedField =
                        MainActivity.class.getDeclaredField("selected");
                    selectedField.setAccessible(true);
                    selectedField.set(activity, mon);
                    java.lang.reflect.Field draftField =
                        MainActivity.class.getDeclaredField("draft");
                    draftField.setAccessible(true);
                    java.lang.reflect.Method render =
                        MainActivity.class.getDeclaredMethod("render");
                    render.setAccessible(true);
                    render.invoke(activity);
                    java.lang.reflect.Method apply =
                        MainActivity.class.getDeclaredMethod(
                            "applyManualChange", Week.class, java.util.List.class);
                    apply.setAccessible(true);
                    Week candidate = copy(original);
                    Duty added = new Duty(mon, "multi-place", "multi");
                    candidate.duties.add(added);
                    dialog[0] =
                        (android.app.AlertDialog)
                            apply.invoke(activity, candidate, java.util.Arrays.asList(added));
                    truth(dialog[0] != null && draftField.get(activity) == null);
                    String message =
                        ((android.widget.TextView) dialog[0].findViewById(android.R.id.message))
                            .getText()
                            .toString();
                    truth(
                        message.contains(sample.placeName(BRANCH))
                            && message.contains("Ek Şube")
                            && message.contains("Haftalık kişi sınırı"));
                    dialog[0]
                        .getButton(android.content.DialogInterface.BUTTON_NEGATIVE)
                        .performClick();
                    dialog[0] = null;
                    truth(
                        draftField.get(activity) == null
                            && original.duties.size() == 2
                            && original.simultaneousOverrides.isEmpty());
                    dialog[0] =
                        (android.app.AlertDialog)
                            apply.invoke(activity, candidate, java.util.Arrays.asList(added));
                    dialog[0]
                        .getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                        .performClick();
                    dialog[0] = null;
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  try {
                    java.lang.reflect.Field draftField =
                        MainActivity.class.getDeclaredField("draft");
                    draftField.setAccessible(true);
                    java.lang.reflect.Field stateField =
                        MainActivity.class.getDeclaredField("state");
                    stateField.setAccessible(true);
                    Week approved = (Week) draftField.get(activity);
                    State sample = (State) stateField.get(activity);
                    truth(
                        approved != null
                            && approved.duties.size() == 3
                            && !approved.simultaneousOverrides.isEmpty()
                            && approved.limitOverrides.contains("multi-place|multi"));
                    truth(
                        validate(sample, approved).isEmpty()
                            && sample.weeks.get(0).duties.size() == 2);
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
          } finally {
            runOnMainSync(
                () -> {
                  if (dialog[0] != null) dialog[0].dismiss();
                  activity.finish();
                });
          }
        });
    check(
        "Leave identity and confirmed exceptions survive storage and old files grant nothing",
        () -> {
          State sample = new State();
          sample.people.add(new Person("exception", "Deneme"));
          Leave leave = new Leave("exception", ALL, mon, mon.plusDays(6), "Rapor");
          sample.leaves.add(leave);
          Week w = new Week(mon);
          Duty duty = new Duty(mon, BRANCH, "exception");
          w.duties.add(duty);
          w.leaveOverrides.add(leaveOverrideKey(duty, leave));
          approve(sample, w);
          Store isolatedStore = new Store(context);
          isolatedStore.save(sample);
          State restored = isolatedStore.load();
          truth(
              restored.leaves.get(0).id.equals(leave.id)
                  && validate(restored, restored.weeks.get(0)).isEmpty());
          org.json.JSONObject legacy = new org.json.JSONObject(Store.encode(sample));
          legacy.getJSONArray("weeks").getJSONObject(0).remove("leaveOverrides");
          legacy.getJSONArray("leaves").getJSONObject(0).remove("id");
          State old = Store.decode(legacy.toString());
          truth(!old.leaves.get(0).id.isEmpty());
          truth(old.leaves.get(0).id.equals(Store.decode(legacy.toString()).leaves.get(0).id));
          truth(old.weeks.get(0).leaveOverrides.isEmpty());
          truth(!validate(old, old.weeks.get(0)).isEmpty());
        });
    check(
        "Manual leave exception asks consent and cancel keeps original while approval preserves"
            + " leave",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          android.app.AlertDialog[] dialog = new android.app.AlertDialog[1];
          try {
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  try {
                    State sample = new State();
                    sample.people.add(new Person("exception", "Deneme"));
                    sample.leaves.add(new Leave("exception", ALL, mon, mon.plusDays(6), "Rapor"));
                    sample.leaves.add(
                        new Leave("exception", BRANCH, mon, LocalDate.MAX, "Haftalık Kısıt", 1));
                    Week original = new Week(mon);
                    sample.weeks.add(original);
                    java.lang.reflect.Field stateField =
                        MainActivity.class.getDeclaredField("state");
                    stateField.setAccessible(true);
                    stateField.set(activity, sample);
                    java.lang.reflect.Field selected =
                        MainActivity.class.getDeclaredField("selected");
                    selected.setAccessible(true);
                    selected.set(activity, mon);
                    java.lang.reflect.Field draftField =
                        MainActivity.class.getDeclaredField("draft");
                    draftField.setAccessible(true);
                    java.lang.reflect.Method render =
                        MainActivity.class.getDeclaredMethod("render");
                    render.setAccessible(true);
                    render.invoke(activity);
                    java.lang.reflect.Method apply =
                        MainActivity.class.getDeclaredMethod(
                            "applyManualChange", Week.class, java.util.List.class);
                    apply.setAccessible(true);
                    Week candidate = copy(original);
                    Duty added = new Duty(mon, BRANCH, "exception");
                    candidate.duties.add(added);
                    dialog[0] =
                        (android.app.AlertDialog)
                            apply.invoke(activity, candidate, java.util.Arrays.asList(added));
                    truth(dialog[0] != null);
                    String message =
                        ((android.widget.TextView) dialog[0].findViewById(android.R.id.message))
                            .getText()
                            .toString();
                    truth(
                        message.contains("Rapor")
                            && message.contains("Haftalık Kısıt")
                            && message.contains(
                                mon.format(
                                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
                    dialog[0]
                        .getButton(android.content.DialogInterface.BUTTON_NEGATIVE)
                        .performClick();
                    dialog[0].dismiss();
                    dialog[0] = null;
                    truth(
                        draftField.get(activity) == null
                            && original.duties.isEmpty()
                            && sample.leaves.size() == 2);
                    dialog[0] =
                        (android.app.AlertDialog)
                            apply.invoke(activity, candidate, java.util.Arrays.asList(added));
                    dialog[0]
                        .getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                        .performClick();
                    dialog[0] = null;
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForIdleSync();
            runOnMainSync(
                () -> {
                  try {
                    java.lang.reflect.Field stateField =
                        MainActivity.class.getDeclaredField("state");
                    stateField.setAccessible(true);
                    State sample = (State) stateField.get(activity);
                    java.lang.reflect.Field draftField =
                        MainActivity.class.getDeclaredField("draft");
                    draftField.setAccessible(true);
                    Week approved = (Week) draftField.get(activity);
                    truth(
                        approved != null
                            && approved.duties.size() == 1
                            && approved.leaveOverrides.size() == 2);
                    truth(validate(sample, approved).isEmpty());
                    truth(
                        sample.leaves.size() == 2 && !available(sample, "exception", BRANCH, mon));
                    truth(sample.weeks.get(0).duties.isEmpty());
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
          } finally {
            runOnMainSync(
                () -> {
                  if (dialog[0] != null) dialog[0].dismiss();
                  activity.finish();
                });
          }
        });
    check(
        "Modern light and dark screens retain navigation and render synthetic design previews",
        () -> {
          android.content.SharedPreferences prefs = target.getSharedPreferences("appearance", 0);
          boolean had = prefs.contains("theme");
          int previous = prefs.getInt("theme", 0);
          File previews = new File(isolated, "redesign");
          previews.mkdirs();
          try {
            for (int mode : new int[] {1, 2}) {
              truth(prefs.edit().putInt("theme", mode).commit());
              Activity activity =
                  launchActivity(
                      new Intent(target, MainActivity.class)
                          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
              try {
                waitForFrames(activity);
                runOnMainSync(
                    () -> {
                      try {
                        State sample = new State();
                        String[] names = {
                          "Ahmet Yılmaz", "Elif Demir", "Mehmet Kaya", "Zeynep Arslan", "Can Öztürk"
                        };
                        for (int i = 0; i < names.length; i++)
                          sample.people.add(new Person("preview" + i, names[i]));
                        sample.place(WORK).name = "Merkez";
                        sample.place(BRANCH).name = "Çarşı Şubesi";
                        sample.places.add(new Place("preview-branch", "Destek Şubesi", 31, 1));
                        approve(sample, generate(sample, mon));
                        sample.leaves.add(
                            new Leave(
                                "preview2",
                                ALL,
                                mon.plusWeeks(1),
                                mon.plusWeeks(1).plusDays(4),
                                "Senelik İzin"));
                        sample.leaves.add(
                            new Leave(
                                "preview3", BRANCH, mon, LocalDate.MAX, "Haftalık Kısıt", 64));
                        java.lang.reflect.Field stateField =
                            MainActivity.class.getDeclaredField("state");
                        stateField.setAccessible(true);
                        stateField.set(activity, sample);
                        java.lang.reflect.Field selected =
                            MainActivity.class.getDeclaredField("selected");
                        selected.setAccessible(true);
                        selected.set(activity, mon);
                        java.lang.reflect.Field screen =
                            MainActivity.class.getDeclaredField("screen");
                        screen.setAccessible(true);
                        screen.setInt(activity, 0);
                        java.lang.reflect.Method render =
                            MainActivity.class.getDeclaredMethod("render");
                        render.setAccessible(true);
                        render.invoke(activity);
                      } catch (ReflectiveOperationException e) {
                        throw new AssertionError(e);
                      }
                    });
                String[] pages = {"home", "people", "leave", "statistics", "settings"};
                for (int page = 0; page < 5; page++) {
                  final int index = page;
                  runOnMainSync(
                      () ->
                          activity
                              .getWindow()
                              .getDecorView()
                              .findViewWithTag("nav-" + index)
                              .performClick());
                  waitForFrames(activity);
                  runOnMainSync(
                      () -> {
                        android.view.View root = activity.getWindow().getDecorView();
                        android.view.ViewGroup nav =
                            (android.view.ViewGroup) root.findViewWithTag("bottom-navigation");
                        truth(nav != null && nav.getChildCount() == 5);
                        for (int tab = 0; tab < 5; tab++) {
                          android.widget.Button item = (android.widget.Button) nav.getChildAt(tab);
                          truth(item.isSelected() == (tab == index));
                          truth(
                              item.getCompoundDrawables()[1] != null
                                  && item.getWidth() > 0
                                  && item.getRight() <= nav.getWidth());
                          if (tab == index)
                            truth(item.getCurrentTextColor() == android.graphics.Color.WHITE);
                        }
                        truth(root.findViewWithTag("theme-control") != null);
                        if (index == 0)
                          truth(root.findViewWithTag("schedule-table-header") != null);
                      });
                  // Allow the emulated GPU compositor to present the completed frame.
                  Thread.sleep(1000);
                  android.graphics.Bitmap screenshot = getUiAutomation().takeScreenshot();
                  truth(screenshot != null);
                  try (FileOutputStream stream =
                      new FileOutputStream(
                          new File(
                              previews, pages[page] + (mode == 1 ? "-light.png" : "-dark.png")))) {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                  }
                  screenshot.recycle();
                }
                runOnMainSync(
                    () ->
                        activity
                            .getWindow()
                            .getDecorView()
                            .findViewWithTag("nav-0")
                            .performClick());
                waitForFrames(activity);
                runOnMainSync(
                    () -> {
                      try {
                        java.lang.reflect.Field scrollField =
                            MainActivity.class.getDeclaredField("pageScroll");
                        scrollField.setAccessible(true);
                        android.widget.ScrollView scroll =
                            (android.widget.ScrollView) scrollField.get(activity);
                        scroll.scrollTo(0, scroll.findViewWithTag("roster-day-" + mon).getTop());
                      } catch (ReflectiveOperationException e) {
                        throw new AssertionError(e);
                      }
                    });
                waitForFrames(activity);
                // Allow the emulated GPU compositor to present the completed frame.
                Thread.sleep(1000);
                android.graphics.Bitmap screenshot = getUiAutomation().takeScreenshot();
                truth(screenshot != null);
                try (FileOutputStream stream =
                    new FileOutputStream(
                        new File(previews, mode == 1 ? "roster-light.png" : "roster-dark.png"))) {
                  screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
                }
                screenshot.recycle();
              } finally {
                runOnMainSync(activity::finish);
              }
            }
          } finally {
            if (had) prefs.edit().putInt("theme", previous).commit();
            else prefs.edit().remove("theme").commit();
          }
        });
    check(
        "Table compact six rows fit phone preserve weekend duties and multiple branch widths",
        () -> {
          Activity activity =
              launchActivity(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          State sample = new State();
          for (int i = 0; i < 5; i++) sample.people.add(new Person("table" + i, "Deneme " + i));
          Week w = generate(sample, mon);
          approve(sample, w);
          java.lang.reflect.Field stateField = MainActivity.class.getDeclaredField("state");
          stateField.setAccessible(true);
          java.lang.reflect.Field selectedField = MainActivity.class.getDeclaredField("selected");
          selectedField.setAccessible(true);
          java.lang.reflect.Field scrollField = MainActivity.class.getDeclaredField("pageScroll");
          scrollField.setAccessible(true);
          java.lang.reflect.Method render = MainActivity.class.getDeclaredMethod("render");
          render.setAccessible(true);
          try {
            runOnMainSync(
                () -> {
                  try {
                    stateField.set(activity, sample);
                    selectedField.set(activity, mon);
                    render.invoke(activity);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            runOnMainSync(
                () -> {
                  try {
                    android.view.View root = activity.getWindow().getDecorView();
                    truth(root.findViewWithTag("schedule-table-header") != null);
                    android.view.ViewGroup weekendRow =
                        (android.view.ViewGroup)
                            root.findViewWithTag("roster-day-" + mon.plusDays(5));
                    truth(
                        ((android.widget.TextView) weekendRow.getChildAt(0))
                            .getText()
                            .toString()
                            .contains("Hafta Sonu\nNöbetçisi"));
                    truth(findText(root, "Cumartesi") == null && findText(root, "Pazar") == null);
                    truth(root.findViewWithTag("roster-day-" + mon.plusDays(6)) == null);
                    android.widget.ScrollView scroll =
                        (android.widget.ScrollView) scrollField.get(activity);
                    android.view.View weekend =
                        root.findViewWithTag("roster-day-" + mon.plusDays(5));
                    truth(weekend.getBottom() <= scroll.getHeight());
                    truth(
                        w.duties.stream()
                                .filter(d -> weekend(d.date) && d.place.equals(WORK))
                                .count()
                            == 2);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            Thread.sleep(1000);
            android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
            truth(bitmap != null);
            try (FileOutputStream stream =
                new FileOutputStream(new File(isolated, "compact-table-2.2.png"))) {
              bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            }
            bitmap.recycle();
            runOnMainSync(
                () -> {
                  try {
                    java.lang.reflect.Field cloudField =
                        MainActivity.class.getDeclaredField("cloud");
                    cloudField.setAccessible(true);
                    ((CloudController) cloudField.get(activity)).open();
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            Thread.sleep(1000);
            bitmap = getUiAutomation().takeScreenshot();
            truth(bitmap != null);
            try (FileOutputStream stream =
                new FileOutputStream(new File(isolated, "cloud-setup-2.2.png"))) {
              bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
            }
            bitmap.recycle();
            runOnMainSync(
                () -> {
                  try {
                    java.lang.reflect.Field cloudField =
                        MainActivity.class.getDeclaredField("cloud");
                    cloudField.setAccessible(true);
                    ((CloudController) cloudField.get(activity)).panel.dismiss();
                    for (int i = 0; i < 3; i++)
                      sample.places.add(new Place("table-extra" + i, "Ek Şube " + i, 127, 1));
                    render.invoke(activity);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            waitForFrames(activity);
            for (int width : new int[] {280, 320, 360, 600}) {
              runOnMainSync(
                  () -> {
                    android.view.View root = activity.getWindow().getDecorView();
                    float density = activity.getResources().getDisplayMetrics().density;
                    root.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            (int) (width * density), android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(
                            (int) (800 * density), android.view.View.MeasureSpec.EXACTLY));
                    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                    for (int part : new int[] {0, 2, 4})
                      for (int day = 0; day < 6; day++) {
                        android.view.ViewGroup row =
                            root.findViewWithTag(
                                "roster-day-"
                                    + mon.plusDays(day)
                                    + (part == 0 ? "" : "-part" + part));
                        truth(row != null);
                        for (int cell = 0; cell < row.getChildCount(); cell++)
                          truth(
                              row.getChildAt(cell).getWidth() > 0
                                  && row.getChildAt(cell).getRight() <= row.getWidth());
                      }
                    root.requestLayout();
                  });
              waitForFrames(activity);
            }
          } finally {
            runOnMainSync(activity::finish);
          }
        });
    Bundle result = new Bundle();
    result.putInt("passed", passed);
    result.putInt("failed", failed);
    result.putInt("skipped", skipped);
    result.putString(
        "stream",
        "Android checks: " + passed + " passed, " + failed + " failed, " + skipped + " skipped\n");
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
