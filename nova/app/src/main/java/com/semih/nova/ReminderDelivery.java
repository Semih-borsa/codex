package com.semih.nova;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;

final class ReminderDelivery {
  static boolean deliver(Context c, Store.Row task) {
    NotificationManager nm = c.getSystemService(NotificationManager.class);
    nm.createNotificationChannel(
        new NotificationChannel(
            ReminderReceiver.CHANNEL,
            "Adaş görev hatırlatmaları",
            NotificationManager.IMPORTANCE_HIGH));
    if (Build.VERSION.SDK_INT >= 33
        && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return false;
    if (!nm.areNotificationsEnabled()) return false;
    NotificationChannel channel = nm.getNotificationChannel(ReminderReceiver.CHANNEL);
    if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE)
      return false;
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class).putExtra("tasks", true),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder n =
        new Notification.Builder(c, ReminderReceiver.CHANNEL)
            .setSmallIcon(R.drawable.ic_nova)
            .setContentTitle("🔔 Adaş hatırlatması")
            .setContentText(task.title)
            .setStyle(new Notification.BigTextStyle().bigText(task.title))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(open);
    if (task.audible) {
      PendingIntent read =
          PendingIntent.getForegroundService(
              c,
              (int) task.id,
              new Intent(c, ReminderSpeechService.class)
                  .putExtra("id", task.id)
                  .putExtra("text", task.title),
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      n.addAction(new Notification.Action.Builder(null, "Sesli oku", read).build());
    }
    nm.notify("task", (int) task.id, n.build());
    if (task.audible && System.currentTimeMillis() - task.time <= 10 * 60 * 1000L) {
      try {
        c.startForegroundService(
            new Intent(c, ReminderSpeechService.class)
                .putExtra("id", task.id)
                .putExtra("text", task.title));
      } catch (RuntimeException restricted) {
        nm.notify(
            "task",
            (int) task.id,
            n.setSubText("Otomatik ses açılamadı; Sesli oku düğmesine dokun.").build());
      }
    }
    return true;
  }
}
