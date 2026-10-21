package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.time.*;

final class TodayChecks {
  static void run(SmokeRunner r, Context target, File isolated) {
    r.check(
        "Today amber weekday weekend other-week highlight and compact light dark rows",
        () -> {
          android.content.SharedPreferences prefs = target.getSharedPreferences("appearance", 0);
          boolean had = prefs.contains("theme");
          int old = prefs.getInt("theme", 0);
          LocalDate mon = LocalDate.of(2026, 10, 19);
          try {
            for (int mode : new int[] {1, 2}) {
              prefs.edit().putInt("theme", mode).commit();
              Activity activity =
                  r.launchActivity(
                      new Intent(target, MainActivity.class)
                          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
              try {
                State sample = DistributionUiChecks.sample(mon);
                Week w = sample.weeks.get(0);
                for (LocalDate today :
                    new LocalDate[] {
                      mon.plusDays(2), mon.plusDays(5), mon.plusDays(6), mon.plusWeeks(1)
                    }) {
                  r.runOnMainSync(
                      () -> {
                        try {
                          DistributionUiChecks.set(activity, "state", sample);
                          DistributionUiChecks.set(activity, "selected", mon);
                          java.lang.reflect.Field f = MainActivity.class.getDeclaredField("body");
                          f.setAccessible(true);
                          LinearLayout body = (LinearLayout) f.get(activity);
                          body.removeAllViews();
                          java.lang.reflect.Method table =
                              MainActivity.class.getDeclaredMethod(
                                  "scheduleTable", Week.class, LocalDate.class);
                          table.setAccessible(true);
                          table.invoke(activity, w, today);
                        } catch (Exception e) {
                          throw new AssertionError(e);
                        }
                      });
                  r.waitForFrames(activity);
                  r.runOnMainSync(
                      () -> {
                        View root = activity.getWindow().getDecorView();
                        int found = 0;
                        for (int row = 0; row < 6; row++) {
                          ViewGroup line = root.findViewWithTag("roster-day-" + mon.plusDays(row));
                          boolean current = ScheduleTable.isToday(w, row, today);
                          r.truth((line.getBackground() instanceof Visuals.TodayRow) == current);
                          TextView date = (TextView) line.getChildAt(0);
                          r.truth(date.getText().toString().contains("Bugün") == current);
                          if (current) {
                            found++;
                            r.truth(
                                date.getLayout() != null && date.getLayout().getLineCount() == 3);
                            r.truth(
                                date.getLayout().getHeight()
                                    <= date.getHeight()
                                        - date.getCompoundPaddingTop()
                                        - date.getCompoundPaddingBottom());
                            Bitmap bitmap =
                                Bitmap.createBitmap(
                                    line.getWidth(), line.getHeight(), Bitmap.Config.ARGB_8888);
                            line.getBackground().draw(new Canvas(bitmap));
                            r.truth(
                                bitmap.getPixel(line.getWidth() / 2, line.getHeight() / 2)
                                    == (mode == 1 ? 0xFFFFF2CC : 0xFF66552F));
                            bitmap.recycle();
                            r.truth(
                                ((TextView) line.getChildAt(1)).getCurrentTextColor()
                                    == (mode == 1 ? 0xFF634616 : 0xFFFFE7AD));
                          }
                        }
                        r.truth(found == (today.isBefore(mon.plusWeeks(1)) ? 1 : 0));
                      });
                  if (today.equals(mon.plusDays(2)) || today.equals(mon.plusDays(5)))
                    DistributionUiChecks.snapshot(
                        r,
                        isolated,
                        "today-2.4-"
                            + mode
                            + "-"
                            + (today.getDayOfWeek().getValue() == 3 ? "weekday" : "weekend")
                            + ".png");
                }
                r.runOnMainSync(
                    () -> {
                      try {
                        java.lang.reflect.Field shown =
                            MainActivity.class.getDeclaredField("displayedDate");
                        shown.setAccessible(true);
                        shown.set(activity, LocalDate.now().minusDays(1));
                        java.lang.reflect.Field task =
                            MainActivity.class.getDeclaredField("refreshDay");
                        task.setAccessible(true);
                        ((Runnable) task.get(activity)).run();
                        r.truth(shown.get(activity).equals(LocalDate.now()));
                      } catch (Exception e) {
                        throw new AssertionError(e);
                      }
                    });
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
