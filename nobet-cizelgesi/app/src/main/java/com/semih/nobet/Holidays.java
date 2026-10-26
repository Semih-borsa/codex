package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import java.time.LocalDate;
import java.util.*;

/** Offline Turkish public holidays. Religious dates are confirmed only for 2020–2032. */
public final class Holidays {
  public static final int FIRST_YEAR = 2020, LAST_RELIGIOUS_YEAR = 2032;

  public static final class Holiday {
    public final String id, name;
    public final LocalDate date;

    /** Half-day units: 1 = 0.5 day, 2 = full day. */
    public final int units;

    public final boolean enabled;

    public Holiday(String id, String name, LocalDate date, int units, boolean enabled) {
      if (id == null
          || id.trim().isEmpty()
          || name == null
          || name.trim().isEmpty()
          || date == null
          || (units != 1 && units != 2))
        throw new IllegalArgumentException("Tatil adı, tarihi ve süresi geçerli olmalıdır.");
      this.id = id;
      this.name = name.trim();
      this.date = date;
      this.units = units;
      this.enabled = enabled;
    }

    public boolean automatic() {
      return id.startsWith("tr:");
    }
  }

  // Gregorian first days of Ramadan / Sacrifice holidays, 2020..2032.
  // Source and coverage: docs/RESMI-TATILLER.md (Vacanza Turkey confirmed calendar).
  private static final int[][] RAMADAN = {
    {5, 24}, {5, 13}, {5, 2}, {4, 21}, {4, 10}, {3, 30}, {3, 20}, {3, 9}, {2, 26}, {2, 14}, {2, 4},
    {1, 24}, {1, 14}
  };
  private static final int[][] SACRIFICE = {
    {7, 31}, {7, 20}, {7, 9}, {6, 28}, {6, 16}, {6, 6}, {5, 27}, {5, 16}, {5, 5}, {4, 24}, {4, 13},
    {4, 2}, {3, 22}
  };

  public static List<Holiday> automatic(int year) {
    List<Holiday> result = new ArrayList<>();
    if (year < FIRST_YEAR) return result;
    fixed(result, year, 1, 1, "Yılbaşı", 2);
    fixed(result, year, 4, 23, "Ulusal Egemenlik Ve Çocuk Bayramı", 2);
    fixed(result, year, 5, 1, "Emek Ve Dayanışma Günü", 2);
    fixed(result, year, 5, 19, "Atatürk'ü Anma, Gençlik Ve Spor Bayramı", 2);
    fixed(result, year, 7, 15, "Demokrasi Ve Millî Birlik Günü", 2);
    fixed(result, year, 8, 30, "Zafer Bayramı", 2);
    fixed(result, year, 10, 28, "Cumhuriyet Bayramı Arifesi", 1);
    fixed(result, year, 10, 29, "Cumhuriyet Bayramı", 2);
    if (year <= LAST_RELIGIOUS_YEAR) {
      religious(result, year, RAMADAN[year - FIRST_YEAR], "ramadan", "Ramazan Bayramı", 3);
      religious(result, year, SACRIFICE[year - FIRST_YEAR], "sacrifice", "Kurban Bayramı", 4);
    }
    return result;
  }

  private static void fixed(List<Holiday> list, int y, int m, int d, String name, int units) {
    list.add(
        new Holiday("tr:" + y + ":fixed:" + m + "-" + d, name, LocalDate.of(y, m, d), units, true));
  }

  private static void religious(
      List<Holiday> list, int year, int[] md, String key, String name, int days) {
    LocalDate first = LocalDate.of(year, md[0], md[1]);
    for (int i = -1; i < days; i++)
      list.add(
          new Holiday(
              "tr:" + year + ":" + key + ":" + i,
              name + (i == -1 ? " Arifesi" : " • " + (i + 1) + ". Gün"),
              first.plusDays(i),
              i == -1 ? 1 : 2,
              true));
  }

  public static List<Holiday> forYear(State s, int year, boolean includeDisabled) {
    Map<String, Holiday> merged = new LinkedHashMap<>();
    for (Holiday h : automatic(year)) merged.put(h.id, h);
    for (Holiday h : s.holidays) merged.put(h.id, h);
    List<Holiday> result = new ArrayList<>();
    for (Holiday h : merged.values())
      if (h.date.getYear() == year && (includeDisabled || h.enabled)) result.add(h);
    result.sort(Comparator.comparing((Holiday h) -> h.date).thenComparing(h -> h.id));
    return result;
  }

  public static List<Holiday> on(State s, LocalDate date) {
    List<Holiday> result = new ArrayList<>();
    for (Holiday h : forYear(s, date.getYear(), false)) if (h.date.equals(date)) result.add(h);
    return result;
  }

  public static int units(State s, LocalDate date) {
    int units = 0;
    for (Holiday h : on(s, date)) units = Math.max(units, h.units);
    return units;
  }

  public static void put(State s, Holiday holiday) {
    s.holidays.removeIf(h -> h.id.equals(holiday.id));
    s.holidays.add(holiday);
  }

  public static String days(long units) {
    return Long.toString(units / 2) + (units % 2 == 0 ? "" : ",5");
  }

  public static List<Place> places(State s) {
    List<Place> result = new ArrayList<>();
    result.add(s.place(WORK));
    result.addAll(statisticPlaces(s));
    for (Place p : s.places)
      if (result.stream().noneMatch(old -> old.id.equals(p.id))
          && s.holidayWork.stream().anyMatch(d -> d.place.equals(p.id))) result.add(p);
    return result;
  }

  public static String workKey(Duty d) {
    return d.date + "|" + d.place + "|" + d.person;
  }

  public static List<Duty> workCandidates(State s, LocalDate date) {
    Map<String, Duty> out = new LinkedHashMap<>();
    for (Week week : s.weeks)
      for (Duty d : week.duties) if (d.date.equals(date)) out.put(workKey(d), d);
    for (Duty d : s.holidayWork) if (d.date.equals(date)) out.put(workKey(d), d);
    List<Duty> result = new ArrayList<>(out.values());
    java.text.Collator sort = java.text.Collator.getInstance(new Locale("tr", "TR"));
    result.sort(
        (a, b) -> {
          int n = sort.compare(s.name(a.person), s.name(b.person));
          return n != 0 ? n : sort.compare(s.place(a.place).name, s.place(b.place).name);
        });
    return result;
  }

  public static void setFullDayWork(State s, LocalDate date, List<Duty> selection) {
    if (units(s, date) == 0)
      throw new IllegalArgumentException("Bu tarih etkin bir resmî tatil değil.");
    Set<String> allowed = new HashSet<>();
    for (Duty d : workCandidates(s, date)) allowed.add(workKey(d));
    Map<String, Duty> checked = new LinkedHashMap<>();
    for (Duty d : selection) {
      if (!d.date.equals(date) || !allowed.contains(workKey(d)))
        throw new IllegalArgumentException("Çizelge değişti; tam gün çalışanları yeniden seçin.");
      checked.put(workKey(d), new Duty(d.date, d.place, d.person));
    }
    s.holidayWork.removeIf(d -> d.date.equals(date));
    s.holidayWork.addAll(checked.values());
  }

  public static List<StatRow> statistics(State s, LocalDate today, boolean future) {
    List<StatRow> rows = new ArrayList<>();
    List<Place> places = places(s);
    Map<LocalDate, Integer> calendar = new HashMap<>();
    for (StatRow base : Roster.statistics(s, today, future)) {
      StatRow row = new StatRow(base.person, places.size());
      Set<String> counted = new HashSet<>();
      for (Duty duty : s.holidayWork)
        if (duty.person.equals(row.person.id) && duty.date.isAfter(today) == future) {
          int units =
              calendar.computeIfAbsent(duty.date, d -> units(s, d)) > 0
                      && counted.add(workKey(duty))
                  ? 2
                  : 0;
          for (int i = 0; i < places.size(); i++)
            if (places.get(i).id.equals(duty.place)) row.counts[i] += units;
        }
      rows.add(row);
    }
    return rows;
  }
}
