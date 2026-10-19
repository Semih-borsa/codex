package com.semih.havapusulasi;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.json.*;

public final class WeatherInstrumentation extends Instrumentation {
  private int passed;
  private Bundle options;

  public void onCreate(Bundle b) {
    super.onCreate(b);
    options = b == null ? new Bundle() : b;
    start();
  }

  private void check(boolean ok, String name) {
    if (!ok) throw new AssertionError(name);
    passed++;
    Bundle b = new Bundle();
    b.putString("stream", "PASS " + name + "\n");
    sendStatus(0, b);
  }

  private static JSONObject array(JSONObject target, String key, int count, Object value)
      throws Exception {
    JSONArray a = new JSONArray();
    for (int i = 0; i < count; i++) a.put(value);
    target.put(key, a);
    return target;
  }

  static String fixture(long now) throws Exception {
    long hour = now / 3600_000 * 3600;
    JSONObject root = new JSONObject().put("timezone", "Europe/Istanbul");
    JSONObject h = new JSONObject(), d = new JSONObject(), units = new JSONObject();
    JSONArray times = new JSONArray();
    for (int i = 0; i < 48; i++) times.put(hour - 3600 + i * 3600);
    h.put("time", times);
    array(h, "temperature_2m", 48, 22.0);
    array(h, "apparent_temperature", 48, 21.0);
    array(h, "precipitation_probability", 48, 65.0);
    array(h, "wind_speed_10m", 48, 16.0);
    array(h, "relative_humidity_2m", 48, 55.0);
    array(h, "weather_code", 48, 61);
    JSONArray days = new JSONArray();
    LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.of("Europe/Istanbul")).toLocalDate();
    for (int i = 0; i < 7; i++)
      days.put(today.plusDays(i).atStartOfDay(ZoneId.of("Europe/Istanbul")).toEpochSecond());
    d.put("time", days);
    array(d, "temperature_2m_min", 7, 14.0);
    array(d, "temperature_2m_max", 7, 24.0);
    array(d, "weather_code", 7, 2);
    units.put("time", "unixtime").put("temperature_2m", "°C").put("wind_speed_10m", "km/h");
    return root.put("hourly", h).put("daily", d).put("hourly_units", units).toString();
  }

  private static boolean text(View v, String contains) {
    if (v instanceof TextView && ((TextView) v).getText().toString().contains(contains))
      return true;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) if (text(g.getChildAt(i), contains)) return true;
    }
    return false;
  }

  public void onStart() {
    Bundle result = new Bundle();
    try {
      Context c = getTargetContext();
      long now = System.currentTimeMillis();
      String raw = fixture(now);
      if (options.getString("phase", "").equals("alarm-smoke")) {
        if (Build.VERSION.SDK_INT != 30)
          throw new IllegalStateException("Run timer smoke on dedicated API 30 test emulator");
        SharedPreferences p = DailyReminder.prefs(c);
        Map<String, ?> original = p.getAll();
        long due = System.currentTimeMillis() + 7000;
        try {
          p.edit().putBoolean("reminder", true).remove("lastReminder").commit();
          c.getSystemService(AlarmManager.class)
              .setExactAndAllowWhileIdle(
                  AlarmManager.RTC_WAKEUP, due, DailyReminder.pending(c, due));
          check(
              due > System.currentTimeMillis(), "Real timer scheduled while Activity is not open");
          android.service.notification.StatusBarNotification delivered = null;
          for (int i = 0; i < 120 && delivered == null; i++) {
            Thread.sleep(250);
            for (android.service.notification.StatusBarNotification n :
                c.getSystemService(NotificationManager.class).getActiveNotifications())
              if (n.getId() == 21 && n.getPostTime() >= due) delivered = n;
          }
          check(delivered != null, "Actual AlarmManager broadcast delivers weather notification");
          String day =
              Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate().toString();
          // Posting the notification is asynchronous with the receiver's final reschedule.
          for (int i = 0;
              i < 40
                  && (p.getLong("nextAlarm", 0) <= System.currentTimeMillis()
                      || !day.equals(p.getString("lastReminder", "")));
              i++) Thread.sleep(100);
          check(
              day.equals(p.getString("lastReminder", "")), "Receiver durably marks delivered day");
          check(
              p.getLong("nextAlarm", 0) > System.currentTimeMillis(),
              "Receiver schedules next daily alarm");
          long posted = delivered.getPostTime();
          new DailyReminder().onReceive(c, new Intent(DailyReminder.ACTION).putExtra("due", due));
          boolean same = false;
          for (android.service.notification.StatusBarNotification n :
              c.getSystemService(NotificationManager.class).getActiveNotifications())
            if (n.getId() == 21) same = n.getPostTime() == posted;
          check(same, "Duplicate same-day alarm does not post a second notification");
        } finally {
          SharedPreferences.Editor edit = p.edit();
          if (original.containsKey("reminder"))
            edit.putBoolean("reminder", (Boolean) original.get("reminder"));
          else edit.remove("reminder");
          if (original.containsKey("lastReminder"))
            edit.putString("lastReminder", (String) original.get("lastReminder"));
          else edit.remove("lastReminder");
          edit.commit();
          DailyReminder.schedule(c);
        }
        result.putString(
            "stream", "OK (" + passed + " actual timer checks). Previous app settings restored.\n");
        finish(Activity.RESULT_OK, result);
        return;
      }
      if (options.getString("phase", "").equals("seed-reboot")) {
        try (WeatherStore real = new WeatherStore(c)) {
          check(real.cities().isEmpty(), "Reboot fixture only seeds empty test installation");
          long id =
              real.add(
                  new City(
                      0, "Ankara • TEST VERİSİ", "", "Türkiye", 39.93, 32.85, "Europe/Istanbul"));
          real.save(id, raw, now);
          ZonedDateTime later = ZonedDateTime.now().plusHours(2);
          DailyReminder.prefs(c)
              .edit()
              .putBoolean("reminder", true)
              .putInt("hour", later.getHour())
              .putInt("minute", later.getMinute())
              .putLong("selected", id)
              .putString("theme", "dark")
              .commit();
          check(DailyReminder.schedule(c) > now, "Test notification scheduled before real reboot");
        }
        result.putString(
            "stream",
            "OK (" + passed + " reboot preparation checks). Fixture labelled TEST VERİSİ.\n");
        finish(Activity.RESULT_OK, result);
        return;
      }
      if (options.getString("phase", "").equals("verify-reboot")) {
        try (WeatherStore real = new WeatherStore(c)) {
          check(
              real.cities().size() == 1 && real.cities().get(0).name.contains("TEST VERİSİ"),
              "City survives actual emulator reboot");
          check(
              real.cached(real.cities().get(0).id) != null,
              "Cached forecast survives actual emulator reboot");
          check(
              DailyReminder.prefs(c).getBoolean("reminder", false),
              "Daily reminder choice survives actual reboot");
          check(
              DailyReminder.prefs(c).getLong("nextAlarm", 0) > now,
              "Boot broadcast restores alarm before reopening Activity");
        }
        MainActivity a =
            (MainActivity)
                startActivitySync(
                    new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runOnMainSync(
            () ->
                check(
                    text(a.getWindow().getDecorView(), "TEST VERİSİ")
                        && text(a.getWindow().getDecorView(), "Kaydedilmiş tahmin"),
                    "Offline cold launch renders persisted cached forecast"));
        waitForIdleSync();
        runOnMainSync(a::finish);
        result.putString(
            "stream", "OK (" + passed + " actual reboot checks). No network required.\n");
        finish(Activity.RESULT_OK, result);
        return;
      }
      Forecast f = WeatherApi.parse(raw, now);
      check(f.upcoming(now, 24).size() == 24, "24 hourly slots from fixture");
      check(f.days.size() == 7, "Seven daily forecasts");
      check(f.hours.get(0).temperature == 22.0, "Temperature and units parsed");
      JSONObject nullable = new JSONObject(raw);
      nullable.getJSONObject("hourly").getJSONArray("temperature_2m").put(0, JSONObject.NULL);
      check(
          WeatherApi.parse(nullable.toString(), now).hours.get(0).temperature == null,
          "Missing value stays null");
      JSONObject invalid = new JSONObject(raw);
      invalid.getJSONObject("hourly_units").put("time", "iso8601");
      try {
        WeatherApi.parse(invalid.toString(), now);
        throw new AssertionError("Invalid units accepted");
      } catch (IOException expected) {
        check(true, "Incompatible time units rejected");
      }
      JSONObject bad = new JSONObject(raw);
      bad.getJSONObject("hourly").getJSONArray("precipitation_probability").put(0, 120);
      try {
        WeatherApi.parse(bad.toString(), now);
        throw new AssertionError("Bad rain accepted");
      } catch (IOException expected) {
        check(true, "Out of range probability rejected");
      }
      check(WeatherApi.search("Xx", url -> "{}").isEmpty(), "No geocoding results handled");
      List<City> found =
          WeatherApi.search(
              "Ankara",
              url ->
                  "{\"results\":[{\"name\":\"Ankara\",\"latitude\":39.93,\"longitude\":32.85,\"timezone\":\"Europe/Istanbul\",\"country\":\"Türkiye\"}]}");
      check(
          found.size() == 1 && found.get(0).name.equals("Ankara"),
          "Geocoding candidate retains city timezone");
      try {
        WeatherApi.fetch("https://example.org/");
        throw new AssertionError("Unknown host accepted");
      } catch (IOException expected) {
        check(true, "Networking limited to forecast and geocoding hosts");
      }
      String database = "readiness-" + now + ".db";
      long cityId;
      try (WeatherStore store = new WeatherStore(c, database)) {
        cityId =
            store.add(
                new City(
                    0,
                    "ÖRNEK ŞEHİR • TEST VERİSİ",
                    "",
                    "Türkiye",
                    39.93,
                    32.85,
                    "Europe/Istanbul"));
        store.save(cityId, raw, now);
        check(
            store.add(new City(0, "Duplicate", "", "", 39.93, 32.85, "Europe/Istanbul")) == cityId,
            "Duplicate city is not added");
        check(store.cities().size() == 1, "Saved city bounded and unique");
        try {
          store.save(cityId, bad.toString(), now);
          throw new AssertionError("Invalid cache saved");
        } catch (IOException expected) {
          check(
              store.cached(cityId).hours.get(0).rain == 65.0,
              "Failed update preserves last good forecast");
        }
        for (int i = 0; i < 11; i++) store.add(new City(0, "Şehir " + i, "", "", i, i, "UTC"));
        try {
          store.add(new City(0, "Fazla", "", "", 20, 20, "UTC"));
          throw new AssertionError("City limit missing");
        } catch (IllegalStateException expected) {
          check(true, "Twelve city storage limit");
        }
        check(
            DailyReminder.summary(store.cities(), store, now).contains("Kaydedilmiş tahmin"),
            "Notification labels cached data");
        check(
            DailyReminder.summary(store.cities(), store, now + 10 * 86400_000L)
                .contains("süresi doldu"),
            "Expired forecasts never appear current in notification");
      }
      WeatherStore reopened = new WeatherStore(c, database);
      check(
          reopened.cities().size() == 12 && reopened.cached(cityId) != null,
          "Offline cache and cities survive database reopen");
      Context isolated =
          new ContextWrapper(c) {
            public SharedPreferences getSharedPreferences(String name, int mode) {
              return c.getSharedPreferences("test-" + now + "-" + name, mode);
            }
          };
      DailyReminder.prefs(isolated)
          .edit()
          .putBoolean("reminder", true)
          .putInt("hour", 8)
          .putInt("minute", 15)
          .commit();
      long scheduled = DailyReminder.schedule(isolated);
      check(scheduled > now, "Custom daily alarm scheduled in future");
      new DailyReminder().onReceive(isolated, new Intent(Intent.ACTION_BOOT_COMPLETED));
      check(
          DailyReminder.prefs(isolated).getLong("nextAlarm", 0) > now,
          "Boot receiver restores future alarm");
      new DailyReminder().onReceive(isolated, new Intent(Intent.ACTION_TIMEZONE_CHANGED));
      check(
          DailyReminder.prefs(isolated).getLong("nextAlarm", 0) > now,
          "Timezone change replans using device clock");
      DailyReminder.prefs(isolated).edit().remove("background").commit();
      check(
          RefreshJob.enabled(isolated) == (WeatherWidget.ids(isolated).length > 0),
          "Installed widgets enable automatic updates unless explicitly opted out");
      DailyReminder.prefs(isolated).edit().putBoolean("background", true).commit();
      RefreshJob.configure(isolated);
      JobInfo job = c.getSystemService(JobScheduler.class).getPendingJob(RefreshJob.ID);
      check(
          job != null
              && job.getNetworkType() == JobInfo.NETWORK_TYPE_UNMETERED
              && job.isPersisted(),
          "Background job is opt-in, persisted and unmetered only");
      check(job.getIntervalMillis() == 1800000L, "Default background policy is thirty minutes");
      DailyReminder.prefs(isolated)
          .edit()
          .putInt("refreshMinutes", 60)
          .putBoolean("wifiOnly", false)
          .commit();
      RefreshJob.configure(isolated);
      JobInfo changed = c.getSystemService(JobScheduler.class).getPendingJob(RefreshJob.ID);
      check(
          changed.getIntervalMillis() == 3600000L
              && changed.getNetworkType() == JobInfo.NETWORK_TYPE_ANY,
          "Changing frequency and data preference replaces existing job constraints");
      runOnMainSync(
          () -> {
            City sampleCity =
                new City(1, "Widget • TEST VERİSİ", "", "", 39.93, 32.85, "Europe/Istanbul");
            android.widget.RemoteViews fresh =
                WeatherWidget.views(c, 991, sampleCity, f, now, "", false);
            View freshView = fresh.apply(c, new FrameLayout(c));
            check(
                text(freshView, "22°")
                    && text(freshView, f.lastUpdated(now, java.time.ZoneId.systemDefault())),
                "Widget RemoteViews shows exact last successful refresh time");
            check(
                text(freshView, "Hissedilen\n21°C")
                    && text(freshView, "Rüzgâr\n16 km/sa")
                    && text(freshView, "Nem\n55%"),
                "Widget exposes feels-like wind and humidity with units");
            check(
                text(freshView, "24° / 14°") && text(freshView, "Parçalı bulutlu"),
                "Widget daily table shows condition and high before low");
            freshView.measure(
                View.MeasureSpec.makeMeasureSpec(
                    (int) (300 * c.getResources().getDisplayMetrics().density),
                    View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                    (int) (500 * c.getResources().getDisplayMetrics().density),
                    View.MeasureSpec.EXACTLY));
            freshView.layout(0, 0, freshView.getMeasuredWidth(), freshView.getMeasuredHeight());
            check(
                freshView.findViewById(R.id.widget_forecast_rows).getHeight()
                    >= (int) (90 * c.getResources().getDisplayMetrics().density),
                "Default widget size leaves readable space for three forecast rows");
            try {
              android.graphics.Bitmap preview =
                  android.graphics.Bitmap.createBitmap(
                      freshView.getMeasuredWidth(),
                      freshView.getMeasuredHeight(),
                      android.graphics.Bitmap.Config.ARGB_8888);
              freshView.draw(new android.graphics.Canvas(preview));
              try (FileOutputStream output =
                  new FileOutputStream(
                      new File(
                          c.getFilesDir(),
                          "widget-expanded-api-" + Build.VERSION.SDK_INT + ".png"))) {
                preview.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            WeatherWidget.prefs(isolated).edit().putInt("page_991", 1).commit();
            View secondPage =
                WeatherWidget.views(isolated, 991, sampleCity, f, now, "", false)
                    .apply(c, new FrameLayout(c));
            String fourthDate =
                f.futureDays(now)
                    .get(3)
                    .date
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMM EEE", Forecast.TR));
            check(
                ((TextView) secondPage.findViewById(R.id.widget_day_date_0))
                    .getText()
                    .toString()
                    .equals(fourthDate),
                "Widget second page advances to fourth future day");
            View emptyWidget =
                WeatherWidget.views(c, 991, sampleCity, null, now, "", false)
                    .apply(c, new FrameLayout(c));
            check(
                text(emptyWidget, "Hissedilen\n—")
                    && text(emptyWidget, "Rüzgâr\n—")
                    && text(emptyWidget, "Gelecek günlere ait kayıt yok"),
                "Missing forecast has no fabricated measurements or daily predictions");
            android.widget.RemoteViews expired =
                WeatherWidget.views(c, 991, sampleCity, f, now + 10 * 86400_000L, "", true);
            View expiredView = expired.apply(c, new FrameLayout(c));
            check(
                text(expiredView, "—")
                    && text(expiredView, "süresi doldu")
                    && !text(expiredView, "22°"),
                "Expired widget clears old current temperature");
            check(
                !WeatherWidget.owns(c, Integer.MAX_VALUE),
                "Unknown widget ID cannot request private widget actions");
          });
      DailyReminder.prefs(isolated)
          .edit()
          .putBoolean("background", false)
          .putBoolean("reminder", false)
          .commit();
      RefreshJob.configure(isolated);
      DailyReminder.schedule(isolated);
      check(
          c.getSystemService(JobScheduler.class).getPendingJob(RefreshJob.ID) == null,
          "Opt-out cancels background job");
      DailyReminder.schedule(c);
      RefreshJob.configure(c);
      boolean allowed = DailyReminder.allowed(c);
      boolean delivered =
          DailyReminder.deliver(c, DailyReminder.summary(reopened.cities(), reopened, now), "TEST");
      check(delivered == allowed, "Notification permission is respected on this Android version");
      if (delivered) {
        boolean exists = false;
        for (android.service.notification.StatusBarNotification n :
            c.getSystemService(NotificationManager.class).getActiveNotifications())
          if (n.getId() == 21)
            exists = n.getNotification().visibility == Notification.VISIBILITY_PRIVATE;
        check(exists, "Notification has private lock screen visibility");
      } else
        check(
            Build.VERSION.SDK_INT >= 33
                || !c.getSystemService(NotificationManager.class).areNotificationsEnabled(),
            "Notification denied state handled without changing settings");
      String[] permissions =
          c.getPackageManager()
              .getPackageInfo(c.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS)
              .requestedPermissions;
      check(
          !Arrays.asList(permissions).contains("android.permission.ACCESS_FINE_LOCATION")
              && !Arrays.asList(permissions).contains("android.permission.SEND_SMS"),
          "No GPS, messaging or payment permission");
      MainActivity activity =
          (MainActivity)
              startActivitySync(
                  new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      runOnMainSync(
          () -> {
            try {
              java.lang.reflect.Field store = MainActivity.class.getDeclaredField("store");
              store.setAccessible(true);
              WeatherStore old = (WeatherStore) store.get(activity);
              store.set(activity, reopened);
              old.close();
              java.lang.reflect.Field preferences = MainActivity.class.getDeclaredField("prefs");
              preferences.setAccessible(true);
              preferences.set(
                  activity, isolated.getSharedPreferences("weather", Context.MODE_PRIVATE));
              java.lang.reflect.Field selected = MainActivity.class.getDeclaredField("selected");
              selected.setAccessible(true);
              selected.setLong(activity, cityId);
              java.lang.reflect.Method render = MainActivity.class.getDeclaredMethod("render");
              render.setAccessible(true);
              render.invoke(activity);
              check(
                  text(activity.getWindow().getDecorView(), "TEST VERİSİ"),
                  "Activity renders isolated fixture city");
              check(
                  text(activity.getWindow().getDecorView(), "Bu saatin model tahmini"),
                  "UI identifies hourly forecast instead of observation");
              check(
                  text(activity.getWindow().getDecorView(), "Önümüzdeki 24 saat"),
                  "Hourly forecast UI renders");
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      waitForIdleSync();
      if ("true".equals(options.getString("offline"))) {
        check(
            c.getSystemService(android.net.ConnectivityManager.class).getActiveNetwork() == null,
            "Test device has no active network");
        runOnMainSync(
            () -> {
              try {
                java.lang.reflect.Method refresh =
                    MainActivity.class.getDeclaredMethod("refresh", City.class);
                refresh.setAccessible(true);
                refresh.invoke(activity, reopened.cities().get(0));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
        java.lang.reflect.Field busy = MainActivity.class.getDeclaredField("busy");
        busy.setAccessible(true);
        boolean[] finished = {false};
        for (int i = 0; i < 160 && !finished[0]; i++) {
          Thread.sleep(250);
          runOnMainSync(
              () -> {
                try {
                  finished[0] = !busy.getBoolean(activity);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
        }
        check(finished[0], "Offline refresh finishes with bounded network timeout");
        runOnMainSync(
            () ->
                check(
                    text(activity.getWindow().getDecorView(), "Yenilenemedi")
                        && text(activity.getWindow().getDecorView(), "Bu saatin model tahmini"),
                    "Offline refresh error keeps cached forecast visible"));
        check(
            reopened.cached(cityId).hours.get(0).temperature == 22.0,
            "Offline refresh preserves stored forecast");
      }
      android.graphics.Bitmap shot = getUiAutomation().takeScreenshot();
      if (shot != null) {
        File path = new File(c.getFilesDir(), "fixture-api-" + Build.VERSION.SDK_INT + ".png");
        try (FileOutputStream out = new FileOutputStream(path)) {
          shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
      }
      runOnMainSync(activity::finish);
      result.putString(
          "stream",
          "OK ("
              + passed
              + " Android checks), API "
              + Build.VERSION.SDK_INT
              + ". All forecast data is test fixture; no live API claim.\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString(
          "stream", "FAILED after " + passed + ": " + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
