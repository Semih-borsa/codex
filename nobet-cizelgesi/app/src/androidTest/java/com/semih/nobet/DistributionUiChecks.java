package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;

final class DistributionUiChecks {
  static void set(Activity activity, String name, Object value) throws Exception {
    java.lang.reflect.Field f = MainActivity.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(activity, value);
  }

  static void render(Activity activity) throws Exception {
    java.lang.reflect.Method m = MainActivity.class.getDeclaredMethod("render");
    m.setAccessible(true);
    m.invoke(activity);
  }

  static void headers(View v, List<String> ids) {
    if (v.getTag() instanceof String
        && ((String) v.getTag()).startsWith("leave-person-")
        && !((String) v.getTag()).startsWith("leave-person-details-")) ids.add((String) v.getTag());
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
        headers(((ViewGroup) v).getChildAt(i), ids);
  }

  static State sample(LocalDate mon) {
    State s = new State();
    String[] names = {"Zeynep Arslan", "Ömer Yılmaz", "İpek Demir", "Işık Kaya", "Ayşe Çelik"};
    for (int i = 0; i < names.length; i++) s.people.add(new Person("ui23-" + i, names[i]));
    s.leaves.add(new Leave("ui23-4", ALL, mon, mon.plusDays(2), "Senelik İzin"));
    s.leaves.add(new Leave("ui23-4", ALL, mon.plusWeeks(1), mon.plusWeeks(1).plusDays(1), "Rapor"));
    s.leaves.add(new Leave("ui23-4", BRANCH, mon, LocalDate.MAX, "Haftalık Kısıt", 5));
    s.leaves.add(new Leave("ui23-1", BRANCH, mon, mon.plusDays(2), "Diğer Mazeret"));
    approve(s, generate(s, mon));
    return s;
  }

  static void snapshot(SmokeRunner r, File dir, String name) throws Exception {
    Thread.sleep(1000);
    Bitmap b = r.getUiAutomation().takeScreenshot();
    r.truth(b != null);
    try (FileOutputStream out = new FileOutputStream(new File(dir, name))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      b.recycle();
    }
  }

  static void assertRule(
      SmokeRunner r, android.graphics.drawable.Drawable row, int width, int height) {
    Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Rect original = new Rect(row.getBounds());
    row.setBounds(0, 0, width, height);
    row.draw(new Canvas(b));
    r.truth(b.getPixel(width / 2, height - 1) == Color.BLACK);
    row.setBounds(original);
    b.recycle();
  }

  static void pdfRule(SmokeRunner r, File file, int expectedY) throws Exception {
    try (ParcelFileDescriptor fd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(fd);
        PdfRenderer.Page page = renderer.openPage(0)) {
      Bitmap bitmap =
          Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
      int maximum = 0;
      for (int y = expectedY * 2 - 2; y <= expectedY * 2 + 2; y++) {
        int black = 0;
        for (int x = 50; x < bitmap.getWidth() - 50; x++) {
          int c = bitmap.getPixel(x, y);
          if (Color.red(c) < 32 && Color.green(c) < 32 && Color.blue(c) < 32) black++;
        }
        maximum = Math.max(maximum, black);
      }
      r.truth(maximum > bitmap.getWidth() - 120);
      bitmap.recycle();
    }
  }

  static void run(SmokeRunner r, Context context, File isolated) {
    LocalDate mon = LocalDate.of(2026, 10, 19);
    r.check(
        "Distribution PDF black row rules in weekly statistics and holiday exports",
        () -> {
          State s = sample(mon);
          Week week = s.weeks.get(0);
          pdfRule(r, PdfExport.create(context, s, week), 134);
          pdfRule(r, PdfExport.createStatistics(context, s, mon.plusDays(6)), 90);
          pdfRule(r, PdfExport.createHolidayStatistics(context, s, mon.plusDays(6)), 90);
        });
    r.check(
        "Distribution theme icon grouped alphabetical leaves and table row separators",
        () -> {
          Context target = r.getTargetContext();
          android.content.SharedPreferences prefs = target.getSharedPreferences("appearance", 0);
          boolean had = prefs.contains("theme");
          int old = prefs.getInt("theme", 0);
          try {
            for (int mode : new int[] {1, 2}) {
              r.truth(prefs.edit().putInt("theme", mode).commit());
              Activity activity =
                  r.launchActivity(
                      new Intent(target, MainActivity.class)
                          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
              try {
                r.runOnMainSync(
                    () -> {
                      try {
                        set(activity, "state", sample(mon));
                        set(activity, "selected", mon);
                        set(activity, "screen", 2);
                        render(activity);
                      } catch (Exception e) {
                        throw new AssertionError(e);
                      }
                    });
                r.waitForFrames(activity);
                r.runOnMainSync(
                    () -> {
                      View root = activity.getWindow().getDecorView();
                      ImageButton theme = root.findViewWithTag("theme-control");
                      r.truth(theme != null && theme.getWidth() == theme.getHeight());
                      Visuals.Icon icon = (Visuals.Icon) theme.getDrawable();
                      r.truth(icon.name.equals(mode == 1 ? "moon" : "sun"));
                      r.truth(
                          icon.getIntrinsicWidth()
                              <= theme.getWidth()
                                  - theme.getPaddingLeft()
                                  - theme.getPaddingRight());
                      Bitmap b = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
                      Visuals.Icon probe = new Visuals.Icon(icon.name, icon.color, 24);
                      probe.draw(new Canvas(b));
                      int pixels = 0;
                      for (int y = 0; y < 24; y++)
                        for (int x = 0; x < 24; x++)
                          if (Color.alpha(b.getPixel(x, y)) > 0) pixels++;
                      r.truth(pixels > 30);
                      b.recycle();
                      List<String> ids = new ArrayList<>();
                      headers(root, ids);
                      r.truth(
                          ids.equals(
                              Arrays.asList(
                                  "leave-person-ui23-4",
                                  "leave-person-ui23-3",
                                  "leave-person-ui23-2",
                                  "leave-person-ui23-1",
                                  "leave-person-ui23-0")));
                      View details = root.findViewWithTag("leave-person-details-ui23-4");
                      r.truth(details.getVisibility() == View.GONE);
                      root.findViewWithTag("leave-person-ui23-4").performClick();
                      r.truth(details.getVisibility() == View.VISIBLE);
                      View type = root.findViewWithTag("leave-type-details-ui23-4|Rapor");
                      r.truth(type.getVisibility() == View.GONE);
                      root.findViewWithTag("leave-type-ui23-4|Rapor").performClick();
                      r.truth(type.getVisibility() == View.VISIBLE);
                      root.findViewWithTag("leave-person-ui23-4").performClick();
                      r.truth(details.getVisibility() == View.GONE);
                      root.findViewWithTag("leave-person-ui23-4").performClick();
                      r.truth(
                          details.getVisibility() == View.VISIBLE
                              && type.getVisibility() == View.VISIBLE);
                    });
                r.waitForFrames(activity);
                snapshot(r, isolated, "leaves-2.3-" + mode + ".png");
                r.runOnMainSync(
                    () -> {
                      try {
                        set(activity, "screen", 0);
                        render(activity);
                      } catch (Exception e) {
                        throw new AssertionError(e);
                      }
                    });
                r.waitForFrames(activity);
                r.runOnMainSync(
                    () -> {
                      View root = activity.getWindow().getDecorView();
                      View row = root.findViewWithTag("roster-day-" + mon);
                      r.truth(row != null);
                      assertRule(r, row.getBackground(), 120, 40);
                    });
                snapshot(r, isolated, "schedule-2.3-" + mode + ".png");
                r.runOnMainSync(
                    () -> {
                      try {
                        set(activity, "screen", 3);
                        render(activity);
                      } catch (Exception e) {
                        throw new AssertionError(e);
                      }
                    });
                r.waitForFrames(activity);
                r.runOnMainSync(
                    () -> {
                      ViewGroup table =
                          activity.getWindow().getDecorView().findViewWithTag("statistics-table");
                      r.truth(table != null);
                      ViewGroup row = (ViewGroup) table.getChildAt(1);
                      assertRule(r, row.getChildAt(0).getBackground(), 120, 40);
                    });
                snapshot(r, isolated, "statistics-2.3-" + mode + ".png");
              } finally {
                r.runOnMainSync(activity::finish);
              }
            }
          } finally {
            if (had) prefs.edit().putInt("theme", old).commit();
            else prefs.edit().remove("theme").commit();
          }
        });
  }
}
