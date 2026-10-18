package com.semih.nova;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.Build;

public final class ReminderReceiver extends BroadcastReceiver {
  static final String CHANNEL = "nova_tasks";

  static PendingIntent alarm(Context c, long id) {
    return PendingIntent.getBroadcast(
        c,
        (int) id,
        new Intent(c, ReminderReceiver.class).setAction("com.semih.nova.REMIND").putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static void schedule(Context c, long id, long when) {
    AlarmManager manager = c.getSystemService(AlarmManager.class);
    long due = Math.max(when, System.currentTimeMillis() + 1000);
    if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
      try {
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, alarm(c, id));
        return;
      } catch (SecurityException denied) {
      }
    }
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, alarm(c, id));
  }

  static void cancel(Context c, long id) {
    ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(alarm(c, id));
    ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) id);
    ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
        .cancel("task", (int) id);
  }

  static void all(Context c) {
    try (Store db = new Store(c);
        Cursor cur =
            db.getReadableDatabase()
                .rawQuery("SELECT id,due FROM tasks WHERE done=0 AND notified=0", null)) {
      while (cur.moveToNext()) schedule(c, cur.getLong(0), cur.getLong(1));
    }
  }

  @Override
  public void onReceive(Context c, Intent intent) {
    if (!"com.semih.nova.REMIND".equals(intent.getAction())) {
      all(c);
      return;
    }
    long id = intent.getLongExtra("id", -1);
    try (Store db = new Store(c)) {
      Store.Row task = db.task(id);
      if (task == null || task.done || task.notified) return;
      if (task.time > System.currentTimeMillis() + 1000) {
        schedule(c, id, task.time);
        return;
      }
      if (!ReminderDelivery.deliver(c, task)) return;
      ContentValues v = new ContentValues();
      v.put("notified", 1);
      db.getWritableDatabase().update("tasks", v, "id=?", new String[] {"" + id});
    }
  }
}
