package com.semih.ilacim;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.*;
import org.json.*;

final class Scheduler {
  static final String CHANNEL = "ilacim-reminders";

  static void refresh(Context c) {
    NotificationManager n = c.getSystemService(NotificationManager.class);
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL, "İlaç ve randevu hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription("Planlanan dozlar, randevular ve stok kontrolü");
    n.createNotificationChannel(channel);
    AlarmManager alarm = c.getSystemService(AlarmManager.class);
    android.content.SharedPreferences pref = c.getSharedPreferences("reminders", 0);
    int previous = pref.getInt("count", 0);
    for (int i = 1; i <= previous; i++) {
      PendingIntent p =
          PendingIntent.getBroadcast(
              c,
              i,
              new Intent(c, ReminderReceiver.class),
              PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
      if (p != null) {
        alarm.cancel(p);
        p.cancel();
      }
    }
    if (!pref.getBoolean("enabled", true)) {
      pref.edit().putInt("count", 0).apply();
      return;
    }
    ZonedDateTime now = ZonedDateTime.now();
    int count = 0;
    try (Store s = new Store(c)) {
      JSONArray plans = s.list("plans");
      for (int i = 0; i < plans.length(); i++) {
        JSONObject p = plans.getJSONObject(i), m = s.find("meds", p.getString("med"));
        if (m == null || !p.optBoolean("active", true)) continue;
        LocalDate start = LocalDate.parse(p.getString("start")),
            end = LocalDate.parse(p.getString("end"));
        for (String t : Rules.times(p.getString("times"))) {
          LocalDate date = now.toLocalDate().isBefore(start) ? start : now.toLocalDate();
          ZonedDateTime at = date.atTime(LocalTime.parse(t)).atZone(now.getZone());
          if (!at.isAfter(now)) at = at.plusDays(1);
          if (at.toLocalDate().isAfter(end)) continue;
          if (s.find("logs", Rules.slot(p.getString("id"), at.toLocalDate(), t)) != null) {
            at = at.plusDays(1);
            if (at.toLocalDate().isAfter(end)) continue;
          }
          set(
              c,
              ++count,
              at,
              "dose",
              p.getString("id"),
              "İlaç planını kontrol et",
              "Planladığın bir dozun saati geldi. Aldığında uygulamada işaretle.");
        }
      }
      JSONArray a = s.list("appointments");
      for (int i = 0; i < a.length(); i++) {
        JSONObject r = a.getJSONObject(i);
        if (r.optBoolean("done")) continue;
        ZonedDateTime at =
            LocalDateTime.parse(r.getString("when")).atZone(now.getZone()).minusHours(1);
        if (at.isAfter(now))
          set(
              c,
              ++count,
              at,
              "appointment",
              r.getString("id"),
              "Randevun yaklaşıyor",
              "Kayıtlı randevuna yaklaşık bir saat kaldı.");
      }
      ZonedDateTime daily = now.toLocalDate().atTime(9, 0).atZone(now.getZone());
      if (!daily.isAfter(now)) daily = daily.plusDays(1);
      set(
          c,
          ++count,
          daily,
          "inventory",
          "",
          "İlaç dolabını kontrol et",
          "Son kullanma tarihi veya stok uyarın var.");
    } catch (Exception e) {
      pref.edit().putString("error", "Hatırlatıcı planları okunamadı.").apply();
    }
    pref.edit().putInt("count", count).apply();
  }

  static void set(
      Context c, int id, ZonedDateTime at, String kind, String ref, String title, String text) {
    Intent intent =
        new Intent(c, ReminderReceiver.class)
            .putExtra("kind", kind)
            .putExtra("ref", ref)
            .putExtra("title", title)
            .putExtra("text", text)
            .putExtra("id", id);
    PendingIntent p =
        PendingIntent.getBroadcast(
            c, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    c.getSystemService(AlarmManager.class)
        .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), p);
  }

  static void notify(Context c, int id, String title, String text) {
    if (Build.VERSION.SDK_INT >= 33
        && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification n =
        new Notification.Builder(c, CHANNEL)
            .setSmallIcon(com.semih.ilacim.R.drawable.ic_medication)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build();
    c.getSystemService(NotificationManager.class).notify(id, n);
  }
}
