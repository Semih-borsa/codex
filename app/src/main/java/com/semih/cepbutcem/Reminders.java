package com.semih.cepbutcem;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.*;
import java.util.*;

/**
 * Local, inexact daily digest. No network, exact-alarm permission or background foreground service.
 */
public final class Reminders {
  private static final String CHANNEL = "payments", ACTION = "com.semih.cepbutcem.CHECK_PAYMENTS";

  private Reminders() {}

  public static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("payment-reminders", Context.MODE_PRIVATE);
  }

  private static NotificationManager manager(Context c) {
    return c.getSystemService(NotificationManager.class);
  }

  public static void channel(Context c) {
    NotificationChannel channel =
        new NotificationChannel(
            CHANNEL, "Ödeme hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT);
    channel.setDescription("Faturalar, kredi taksitleri ve kredi kartı ekstreleri");
    channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
    manager(c).createNotificationChannel(channel);
  }

  public static boolean allowed(Context c) {
    channel(c);
    NotificationManager n = manager(c);
    return (Build.VERSION.SDK_INT < 33
            || c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
        && n.areNotificationsEnabled()
        && n.getNotificationChannel(CHANNEL).getImportance() != NotificationManager.IMPORTANCE_NONE;
  }

  private static PendingIntent alarm(Context c) {
    return PendingIntent.getBroadcast(
        c,
        91,
        new Intent(c, ReminderReceiver.class).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public static ZonedDateTime nextCheck(ZonedDateTime now, int hour) {
    ZonedDateTime next = now.toLocalDate().atTime(hour, 0).atZone(now.getZone());
    return next.isAfter(now)
        ? next
        : now.toLocalDate().plusDays(1).atTime(hour, 0).atZone(now.getZone());
  }

  public static void schedule(Context c) {
    channel(c);
    AlarmManager alarms = c.getSystemService(AlarmManager.class);
    PendingIntent pi = alarm(c);
    alarms.cancel(pi);
    if (!prefs(c).getBoolean("enabled", false)) {
      manager(c).cancel(700);
      return;
    }
    long next =
        nextCheck(ZonedDateTime.now(), prefs(c).getInt("hour", 9)).toInstant().toEpochMilli();
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi);
  }

  public static final class Due {
    public final String name, label;
    public final LocalDate date;
    public final long amount;

    Due(String n, LocalDate d, long a) {
      this(n, n, d, a);
    }

    Due(String n, String shortLabel, LocalDate d, long a) {
      name = n;
      label = shortLabel;
      date = d;
      amount = a;
    }
  }

  public static List<Due> upcoming(
      Planning p, List<Finance.Account> accounts, LocalDate today, int lead) {
    List<Due> due = new ArrayList<>();
    LocalDate until = today.plusDays(lead);
    for (Planning.Bill b : p.bills)
      if (!b.isPaid() && !b.due.isAfter(until)) {
        String label =
            b.loan == 0
                ? b.name
                : p.loans.stream()
                    .filter(l -> l.id == b.loan)
                    .map(l -> l.name)
                    .findFirst()
                    .orElse(b.name);
        due.add(new Due(b.name, label, b.due, b.amount));
      }
    for (Planning.Card card : p.cards)
      if (card.updated != null && card.statement > 0) {
        // The snapshot's statement belongs to its last closed cycle, not a new cycle invented
        // today.
        LocalDate date = card.statementDue();
        if (!date.isAfter(until)) {
          String name =
              accounts.stream()
                  .filter(a -> a.id == card.account)
                  .map(a -> a.name)
                  .findFirst()
                  .orElse("Kart");
          due.add(new Due(name + " · ekstre (özeti kontrol et)", name, date, card.statement));
        }
      }
    due.sort(Comparator.comparing(d -> d.date));
    return due;
  }

  private static void show(Context c, int id, String title, String content, List<String> lines) {
    if (!allowed(c)) return;
    manager(c).notify(id, notification(c, title, content, lines));
  }

  static Notification notification(Context c, String title, String content, List<String> lines) {
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            92,
            new Intent(c, MainActivity.class).putExtra("payments", true),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.BigTextStyle style =
        new Notification.BigTextStyle()
            .bigText(content + (lines.isEmpty() ? "" : "\n\n" + String.join("\n", lines)));
    Notification publicVersion =
        new Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 Cep Bütçem")
            .setContentText("Ödeme hatırlatman var")
            .build();
    return new Notification.Builder(c, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(style)
        .setContentIntent(open)
        .setAutoCancel(true)
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .setPublicVersion(publicVersion)
        .setOnlyAlertOnce(true)
        .build();
  }

  public static void test(Context c) {
    show(
        c,
        701,
        "🔔 Cep Bütçem · Bildirim testi",
        "Ödeme hatırlatmaların bu şekilde gelecek.",
        Arrays.asList("Test bildirimi · finansal kayıt oluşturulmadı."));
  }

  public static void check(Context c) {
    if (!prefs(c).getBoolean("enabled", false) || !allowed(c)) return;
    LocalDate today = LocalDate.now();
    String day = today.toString();
    try (Ledger ledger = new Ledger(c)) {
      List<Due> due =
          upcoming(ledger.planning(), ledger.accounts(), today, prefs(c).getInt("lead", 1));
      if (due.isEmpty()) {
        manager(c).cancel(700);
        prefs(c).edit().remove("lastSent").remove("lastDigest").apply();
        return;
      }
      List<String> lines = new ArrayList<>();
      for (int i = 0; i < Math.min(6, due.size()); i++) {
        Due d = due.get(i);
        lines.add(
            d.name
                + " · "
                + d.date
                + " · "
                + Finance.money(d.amount)
                + (d.date.isBefore(today) ? " · Gecikmiş" : ""));
      }
      String todayContent = ReminderText.today(due, today);
      String title = todayContent == null ? "🔔 Ödeme hatırlatması" : "🔔 Son ödeme günü";
      String content =
          todayContent == null ? "Fatura, taksit ve ekstrelerini kontrol et." : todayContent;
      String digest = day + "|" + content + "|" + String.join("|", lines);
      if (day.equals(prefs(c).getString("lastSent", ""))
          && digest.equals(prefs(c).getString("lastDigest", ""))) return;
      show(c, 700, title, content, lines);
      prefs(c).edit().putString("lastSent", day).putString("lastDigest", digest).apply();
    } catch (Exception e) {
      android.util.Log.e("CepButcem", "Ödeme hatırlatma kontrolü tamamlanamadı.");
    }
  }
}
