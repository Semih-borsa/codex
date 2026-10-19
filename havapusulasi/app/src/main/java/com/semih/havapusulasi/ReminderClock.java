package com.semih.havapusulasi;

import java.time.*;

final class ReminderClock {
  static long next(Instant now, ZoneId zone, int hour, int minute) {
    LocalTime time = LocalTime.of(hour, minute);
    ZonedDateTime local = now.atZone(zone);
    ZonedDateTime candidate = ZonedDateTime.of(local.toLocalDate(), time, zone);
    if (!candidate.toInstant().isAfter(now))
      candidate = ZonedDateTime.of(local.toLocalDate().plusDays(1), time, zone);
    return candidate.toInstant().toEpochMilli();
  }
}
