package com.semih.sicil360;

import static com.semih.sicil360.Model.*;

import java.time.*;
import java.util.*;

/** Derived reports use local calendar days; opening WhatsApp never implies delivery. */
public final class Crm {
  public static final class Report {
    public int total, pending, again, complete, preparedToday, sentMarkedToday, completedToday;
  }

  static boolean onDay(long time, LocalDate day, ZoneId zone) {
    return time > 0 && Instant.ofEpochMilli(time).atZone(zone).toLocalDate().equals(day);
  }

  public static Report report(State state, LocalDate day, ZoneId zone) {
    Report r = new Report();
    for (Person p : state.people.values()) {
      r.total++;
      switch (p.followUp) {
        case PENDING:
          r.pending++;
          break;
        case AGAIN:
          r.again++;
          break;
        case COMPLETE:
          r.complete++;
          if (onDay(p.followUpChanged, day, zone)) r.completedToday++;
          break;
      }
    }
    for (Event e : state.history)
      if (onDay(e.time, day, zone)) {
        r.preparedToday++;
        if (e.status == Status.SENT_MARKED) r.sentMarkedToday++;
      }
    return r;
  }

  public static List<Person> people(State state, FollowUp filter, String query) {
    String q = fold(query);
    List<Person> result = new ArrayList<>();
    for (Person p : state.people.values())
      if ((filter == null || p.followUp == filter)
          && (q.isEmpty() || p.sicil.contains(query.trim()) || p.searchable().contains(q)))
        result.add(p);
    java.text.Collator tr = java.text.Collator.getInstance(new Locale("tr", "TR"));
    result.sort(
        (a, b) -> {
          int n = tr.compare(a.name(), b.name());
          return n == 0 ? a.sicil.compareTo(b.sicil) : n;
        });
    return result;
  }
}
