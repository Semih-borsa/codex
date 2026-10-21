package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import java.time.LocalDate;
import java.util.*;

/** Shared six-row presentation; actual Saturday/Sunday duties remain separate in storage. */
final class ScheduleTable {
  static boolean isToday(Week week, int row, LocalDate today) {
    return dates(week, row).contains(today);
  }

  static List<LocalDate> dates(Week week, int row) {
    if (row < 0 || row > 5) throw new IllegalArgumentException("Geçersiz çizelge satırı");
    return row == 5
        ? Arrays.asList(week.monday.plusDays(5), week.monday.plusDays(6))
        : Collections.singletonList(week.monday.plusDays(row));
  }

  private static String lookup(State state, Week week, LocalDate date, String place) {
    return week.duties.stream()
        .filter(d -> d.date.equals(date) && d.place.equals(place))
        .map(d -> state.name(d.person))
        .findFirst()
        .orElse("—");
  }

  static String person(State state, Week week, int row, String place) {
    List<LocalDate> days = dates(week, row);
    String first = lookup(state, week, days.get(0), place);
    if (row != 5) return first;
    String second = lookup(state, week, days.get(1), place);
    if (first.equals(second)) return first;
    if (first.equals("—") && !state.place(place).on(days.get(0))) return second;
    if (second.equals("—") && !state.place(place).on(days.get(1))) return first;
    return "Cmt: " + first + "\nPaz: " + second;
  }
}
