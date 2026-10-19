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

  private void checkStatisticsBounds(android.view.View view) {
    truth(!(view instanceof android.widget.HorizontalScrollView));
    if ("statistics-table".equals(view.getTag())) {
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

  private void waitForFrames(Activity activity) throws InterruptedException {
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
              truth(page.getWidth() == 700 && page.getHeight() == 358);
              android.graphics.Bitmap bitmap =
                  android.graphics.Bitmap.createBitmap(
                      700, 358, android.graphics.Bitmap.Config.ARGB_8888);
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
                          == android.graphics.Color.rgb(42, 111, 177));
                  settings.performClick();
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
              startActivitySync(
                  new Intent(target, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          try {
            waitForIdleSync();
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
                    findText(activity.getWindow().getDecorView(), "İstatistik").performClick();
                    android.view.View root = activity.getWindow().getDecorView();
                    for (Place place : statisticPlaces(sample))
                      truth(findText(root, place.name) != null);
                    truth(findText(root, "A\nPersonel") == null);
                    for (int width : new int[] {280, 320, 360, 600}) {
                      float density = activity.getResources().getDisplayMetrics().density;
                      root.measure(
                          android.view.View.MeasureSpec.makeMeasureSpec(
                              (int) (width * density), android.view.View.MeasureSpec.EXACTLY),
                          android.view.View.MeasureSpec.makeMeasureSpec(
                              (int) (800 * density), android.view.View.MeasureSpec.EXACTLY));
                      root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                      checkStatisticsBounds(root);
                    }
                    root.requestLayout();
                  } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                  }
                });
            waitForIdleSync();
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
              startActivitySync(
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
              startActivitySync(
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
              startActivitySync(
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
                    truth(findText(day, sample.placeName(BRANCH) + ": —  ✎") != null);
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
              startActivitySync(
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
    Bundle result = new Bundle();
    result.putInt("passed", passed);
    result.putInt("failed", failed);
    result.putString("stream", "Android checks: " + passed + " passed, " + failed + " failed\n");
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
