package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Reads saved duties, never predicts a future allocation from personnel order alone. */
final class WeekendList {
  static final class Entry {
    final LocalDate monday;
    final String label;

    Entry(LocalDate monday, String label) {
      this.monday = monday;
      this.label = label;
    }
  }

  static String names(State state, Week week, LocalDate day) {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    for (Duty duty : week.duties)
      if (duty.date.equals(day) && duty.place.equals(WORK)) names.add(state.name(duty.person));
    return names.isEmpty() ? "—" : String.join(", ", names);
  }

  static List<Entry> entries(State state) {
    List<Week> weeks = new ArrayList<>(state.weeks);
    weeks.sort(Comparator.comparing(w -> w.monday));
    List<Entry> result = new ArrayList<>();
    Locale tr = new Locale("tr", "TR");
    DateTimeFormatter month = DateTimeFormatter.ofPattern("MMMM", tr);
    for (Week w : weeks) {
      LocalDate sat = w.monday.plusDays(5), sun = w.monday.plusDays(6);
      String range;
      if (sat.getYear() != sun.getYear())
        range =
            sat.format(DateTimeFormatter.ofPattern("d MMMM yyyy", tr))
                + "–"
                + sun.format(DateTimeFormatter.ofPattern("d MMMM yyyy", tr));
      else if (sat.getMonth() == sun.getMonth())
        range =
            sat.getDayOfMonth()
                + "–"
                + sun.getDayOfMonth()
                + " "
                + sun.format(month)
                + " "
                + sun.getYear();
      else
        range =
            sat.getDayOfMonth()
                + " "
                + sat.format(month)
                + "–"
                + sun.getDayOfMonth()
                + " "
                + sun.format(month)
                + " "
                + sun.getYear();
      String a = names(state, w, sat), b = names(state, w, sun);
      String person = a.equals(b) ? a : "Cumartesi: " + a + " / Pazar: " + b;
      result.add(new Entry(w.monday, range + " · " + person));
    }
    return result;
  }
}
