package com.semih.cepbutcem;

import android.content.*;

public final class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (!"com.semih.cepbutcem.CHECK_PAYMENTS".equals(action)
        && !Intent.ACTION_BOOT_COMPLETED.equals(action)
        && !Intent.ACTION_TIME_CHANGED.equals(action)
        && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)
        && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
    PendingResult pending = goAsync();
    Context app = context.getApplicationContext();
    new Thread(
            () -> {
              try {
                Reminders.check(app);
              } finally {
                Reminders.schedule(app);
                pending.finish();
              }
            },
            "payment-reminders")
        .start();
  }
}
