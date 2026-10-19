package com.semih.havapusulasi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;
import java.util.*;

public final class DailyReminder extends BroadcastReceiver {
  static final String ACTION = "com.semih.havapusulasi.DAILY", CHANNEL = "daily_weather";

  static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("weather", Context.MODE_PRIVATE);
  }

  static PendingIntent pending(Context c, long due) {
    return PendingIntent.getBroadcast(
        c,
        7,
        new Intent(c, DailyReminder.class).setAction(ACTION).putExtra("due", due),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static boolean exact(Context c) {
    return Build.VERSION.SDK_INT < 31
        || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
  }

  static void channel(Context c) {
    c.getSystemService(NotificationManager.class)
        .createNotificationChannel(
            new NotificationChannel(
                CHANNEL, "Günlük hava özeti", NotificationManager.IMPORTANCE_DEFAULT));
  }

  static boolean allowed(Context c) {
    NotificationManager n = c.getSystemService(NotificationManager.class);
    NotificationChannel ch = n.getNotificationChannel(CHANNEL);
    return n.areNotificationsEnabled()
        && (ch == null || ch.getImportance() != NotificationManager.IMPORTANCE_NONE)
        && (Build.VERSION.SDK_INT < 33
            || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED);
  }

  static long schedule(Context c) {
    SharedPreferences p = prefs(c);
    AlarmManager manager = c.getSystemService(AlarmManager.class);
    if (!p.getBoolean("reminder", false)) {
      manager.cancel(pending(c, 0));
      p.edit().remove("nextAlarm").apply();
      return 0;
    }
    long due =
        ReminderClock.next(
            Instant.now(), ZoneId.systemDefault(), p.getInt("hour", 8), p.getInt("minute", 0));
    PendingIntent next = pending(c, due);
    boolean scheduled = false;
    if (exact(c))
      try {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, next);
        scheduled = true;
      } catch (SecurityException denied) {
      }
    if (!scheduled) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, next);
    p.edit().putLong("nextAlarm", due).apply();
    return due;
  }

  static String summary(List<City> cities, WeatherStore store, long now) {
    if (cities.isEmpty()) return "Kayıtlı şehir yok. Uygulamadan şehir ekleyip tahmini indir.";
    StringBuilder text = new StringBuilder();
    for (City city : cities) {
      Forecast f = store.cached(city.id);
      text.append(city.name).append(": ");
      if (f == null) text.append("indirilmiş tahmin yok");
      else {
        Forecast.Hour h = f.current(now);
        if (h == null) text.append("tahmin süresi doldu, yenile");
        else {
          text.append(Forecast.value(h.temperature, "°C"))
              .append(" • ")
              .append(Forecast.description(h.code))
              .append(" • Yağış ")
              .append(Forecast.value(h.rain, "%"));
        }
        text.append("\n").append(f.status(now));
      }
      text.append("\n\n");
    }
    return text.append("Kaynak: Open-Meteo • Saatlik model tahmini, ölçüm değildir.")
        .toString()
        .trim();
  }

  static boolean deliver(Context c, String body, String date) {
    channel(c);
    if (!allowed(c)) return false;
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    c.getSystemService(NotificationManager.class)
        .notify(
            21,
            new Notification.Builder(c, CHANNEL)
                .setSmallIcon(com.semih.havapusulasi.R.drawable.ic_notification)
                .setContentTitle("🔔 HavaPusulası • " + date)
                .setContentText("Günün kayıtlı hava tahmini hazır")
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build());
    return true;
  }

  // Persist the deduplication marker before the broadcast process can be reclaimed.
  @android.annotation.SuppressLint("ApplySharedPref")
  @Override
  public void onReceive(Context c, Intent intent) {
    if (!ACTION.equals(intent.getAction())) {
      schedule(c);
      RefreshJob.configure(c);
      WeatherWidget.updateAll(c);
      return;
    }
    SharedPreferences p = prefs(c);
    if (!p.getBoolean("reminder", false)) return;
    long due = intent.getLongExtra("due", 0), now = System.currentTimeMillis();
    if (due <= 0 || due > now + 1000 || now - due > 6 * 3600_000L) {
      schedule(c);
      return;
    }
    String key = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate().toString();
    if (!key.equals(p.getString("lastReminder", ""))) {
      try (WeatherStore store = new WeatherStore(c)) {
        if (deliver(c, summary(store.cities(), store, now), key))
          p.edit().putString("lastReminder", key).commit();
      }
    }
    schedule(c);
  }
}
