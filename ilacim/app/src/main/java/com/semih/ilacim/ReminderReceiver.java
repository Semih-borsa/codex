package com.semih.ilacim;

import android.content.*;
import java.time.*;
import org.json.*;

public class ReminderReceiver extends BroadcastReceiver {
  public void onReceive(Context c, Intent i) {
    boolean show = true;
    if ("inventory".equals(i.getStringExtra("kind"))) {
      show = false;
      try (Store s = new Store(c)) {
        JSONArray a = s.list("meds");
        for (int j = 0; j < a.length(); j++) {
          JSONObject m = a.getJSONObject(j);
          if (!m.optBoolean("archived")
              && (m.getInt("stock") <= m.getInt("low")
                  || !Rules.expiry(m.getString("expiry"))
                      .atEndOfMonth()
                      .isAfter(LocalDate.now().plusDays(60)))) show = true;
        }
      } catch (Exception ignored) {
      }
    }
    if (show)
      Scheduler.notify(
          c, i.getIntExtra("id", 1), i.getStringExtra("title"), i.getStringExtra("text"));
    Scheduler.refresh(c);
  }
}
