package com.semih.nobet;

import static com.semih.nobet.Roster.*;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public class HolidaysTest {
  LocalDate d(String date) {
    return LocalDate.parse(date);
  }

  State sample() {
    State s = new State();
    s.people.add(new Person("a", "Zeynep"));
    s.people.add(new Person("b", "Ahmet"));
    return s;
  }

  void duty(State s, String date, String place, String person) {
    LocalDate day = d(date);
    Week week = s.weeks.stream().filter(w -> w.monday.equals(monday(day))).findFirst().orElse(null);
    if (week == null) {
      week = new Week(monday(day));
      s.weeks.add(week);
    }
    week.duties.add(new Duty(day, place, person));
    s.holidayWork.add(new Duty(day, place, person));
  }

  @Test
  public void nationalHolidaysAndHalfRepublicEve() {
    State s = new State();
    for (String day :
        Arrays.asList(
            "2026-01-01",
            "2026-04-23",
            "2026-05-01",
            "2026-05-19",
            "2026-07-15",
            "2026-08-30",
            "2026-10-29")) assertEquals(day, 2, Holidays.units(s, d(day)));
    assertEquals(1, Holidays.units(s, d("2026-10-28")));
    assertEquals(0, Holidays.units(s, d("2026-10-30")));
    assertEquals(0, Holidays.units(s, d("2026-10-19")));
  }

  @Test
  public void religious2026ArefeAndEveryBayramDay() {
    State s = new State();
    assertEquals(1, Holidays.units(s, d("2026-03-19")));
    for (int i = 20; i <= 22; i++) assertEquals(2, Holidays.units(s, LocalDate.of(2026, 3, i)));
    assertEquals(0, Holidays.units(s, d("2026-03-23")));
    assertEquals(1, Holidays.units(s, d("2026-05-26")));
    for (int i = 27; i <= 30; i++) assertEquals(2, Holidays.units(s, LocalDate.of(2026, 5, i)));
    assertEquals(0, Holidays.units(s, d("2026-05-31")));
  }

  @Test
  public void confirmedBoundaryAndLeapYearDates() {
    State s = new State();
    assertEquals(2, Holidays.units(s, d("2020-05-24")));
    assertEquals(2, Holidays.units(s, d("2024-04-10")));
    assertEquals(2, Holidays.units(s, d("2025-03-30")));
    assertEquals(
        0, Holidays.units(s, d("2028-02-29"))); // Leap day follows the three-day Ramadan holiday.
  }

  @Test
  public void unsupportedYearsNeverGuessReligiousDates() {
    State s = new State();
    assertTrue(Holidays.automatic(2019).isEmpty());
    assertEquals(8, Holidays.automatic(2033).size());
    assertEquals(2, Holidays.units(s, d("2033-10-29")));
    assertEquals(2, Holidays.units(s, d("2032-01-14")));
    assertEquals(2, Holidays.units(s, d("2030-02-04")));
    assertEquals(2, Holidays.units(s, d("2031-01-24")));
  }

  @Test
  public void everySupportedYearHasSeventeenUniqueRecordsAndValidReligiousDays() {
    for (int y = 2020; y <= 2032; y++) {
      List<Holidays.Holiday> holidays = Holidays.automatic(y);
      assertEquals(17, holidays.size());
      assertEquals(17, holidays.stream().map(h -> h.id).distinct().count());
      assertEquals(31, holidays.stream().mapToInt(h -> h.units).sum());
      for (Holidays.Holiday h : holidays) assertEquals(y, h.date.getYear());
    }
  }

  @Test
  public void moveAutomaticAcrossYearRemovesOriginalAndResetRestoresIt() {
    State s = new State();
    Holidays.Holiday h = Holidays.on(s, d("2026-10-29")).get(0);
    Holidays.put(s, new Holidays.Holiday(h.id, "Düzeltilmiş Tatil", d("2027-02-01"), 1, true));
    assertEquals(0, Holidays.units(s, d("2026-10-29")));
    assertEquals(1, Holidays.units(s, d("2027-02-01")));
    assertEquals(
        1, Holidays.forYear(s, 2027, false).stream().filter(x -> x.id.equals(h.id)).count());
    s.holidays.clear();
    assertEquals(2, Holidays.units(s, d("2026-10-29")));
    assertEquals(0, Holidays.units(s, d("2027-02-01")));
  }

  @Test
  public void disabledAutomaticSurvivesCalendarRefreshAndCanBeEnabled() {
    State s = new State();
    Holidays.Holiday h = Holidays.on(s, d("2026-10-29")).get(0);
    Holidays.put(s, new Holidays.Holiday(h.id, h.name, h.date, h.units, false));
    assertEquals(0, Holidays.units(s, h.date));
    assertTrue(
        Holidays.forYear(s, 2026, true).stream().anyMatch(x -> x.id.equals(h.id) && !x.enabled));
    Holidays.put(s, h);
    assertEquals(2, Holidays.units(s, h.date));
    assertEquals(1, s.holidays.size());
  }

  @Test
  public void overlapsCountMaximumOnceIncludingWeekendAndManual() {
    State s = sample();
    Holidays.put(s, new Holidays.Holiday("custom:1", "Yarım", d("2026-10-28"), 1, true));
    Holidays.put(s, new Holidays.Holiday("custom:2", "Tam", d("2026-10-28"), 2, true));
    Holidays.put(s, new Holidays.Holiday("custom:3", "Pazar", d("2026-08-30"), 2, true));
    duty(s, "2026-10-28", WORK, "a");
    duty(s, "2026-08-30", WORK, "a");
    assertArrayEquals(
        new long[] {4, 0}, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts);
    assertArrayEquals(
        new long[] {1, 1, 0}, Roster.statistics(s, d("2026-12-31"), false).get(1).counts);
  }

  @Test
  public void holidayEditRecalculatesWithoutChangingNormalStatisticsOrDuties() {
    State s = sample();
    duty(s, "2026-10-19", WORK, "b");
    duty(s, "2026-10-28", BRANCH, "b");
    long[] ordinary = Roster.statistics(s, d("2026-12-31"), false).get(0).counts.clone();
    assertArrayEquals(
        new long[] {0, 1}, Holidays.statistics(s, d("2026-12-31"), false).get(0).counts);
    Holidays.put(s, new Holidays.Holiday("custom:x", "Kurum Tatili", d("2026-10-19"), 2, true));
    assertArrayEquals(
        new long[] {2, 1}, Holidays.statistics(s, d("2026-12-31"), false).get(0).counts);
    Holidays.put(s, new Holidays.Holiday("custom:x", "Taşınmış Tatil", d("2026-10-20"), 1, true));
    assertArrayEquals(
        new long[] {0, 1}, Holidays.statistics(s, d("2026-12-31"), false).get(0).counts);
    assertArrayEquals(ordinary, Roster.statistics(s, d("2026-12-31"), false).get(0).counts);
    assertEquals(2, s.weeks.get(0).duties.size() + s.weeks.get(1).duties.size());
  }

  @Test
  public void holidayTotalsSeparatePlacesPastFutureAndPreserveHiddenDeletedInactive() {
    State s = sample();
    s.place(BRANCH).name = "Çarşı";
    s.places.add(new Place("extra", "Destek", 31, 1));
    s.place("extra").deleted = true;
    s.hiddenPlaces.add(BRANCH);
    s.people.get(0).active = false;
    duty(s, "2026-10-28", WORK, "a");
    duty(s, "2026-10-28", BRANCH, "a");
    duty(s, "2026-10-29", "extra", "a");
    assertArrayEquals(
        new long[] {1, 1, 0}, Holidays.statistics(s, d("2026-10-28"), false).get(1).counts);
    assertArrayEquals(
        new long[] {0, 0, 2}, Holidays.statistics(s, d("2026-10-28"), true).get(1).counts);
    assertEquals("Çarşı", Holidays.places(s).get(1).name);
    assertEquals("Destek", Holidays.places(s).get(2).name);
  }

  @Test
  public void approvedWeekRevisionDoesNotInventOrReplaceFullDayWork() {
    State s = sample();
    Week w = new Week(monday(d("2026-10-29")));
    w.duties.add(new Duty(d("2026-10-29"), WORK, "a"));
    s.weeks.add(w);
    s.holidayWork.add(new Duty(d("2026-10-29"), WORK, "a"));
    assertEquals(2, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
    w.duties.clear();
    w.duties.add(new Duty(d("2026-10-29"), BRANCH, "b"));
    assertArrayEquals(
        new long[] {0, 0}, Holidays.statistics(s, d("2026-12-31"), false).get(0).counts);
    assertArrayEquals(
        new long[] {2, 0}, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts);
  }

  @Test
  public void integerHalfUnitsFormatWithoutRoundingErrors() {
    assertEquals("0", Holidays.days(0));
    assertEquals("0,5", Holidays.days(1));
    assertEquals("1", Holidays.days(2));
    assertEquals("15,5", Holidays.days(31));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidDurationRejected() {
    new Holidays.Holiday("id", "Tatil", d("2026-01-01"), 3, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyNameRejected() {
    new Holidays.Holiday("id", " ", d("2026-01-01"), 2, true);
  }

  @Test
  public void assignmentAloneNeverCountsAsFullDayWork() {
    State s = sample();
    Week w = new Week(monday(d("2026-10-29")));
    w.duties.add(new Duty(d("2026-10-29"), WORK, "a"));
    s.weeks.add(w);
    assertEquals(0, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
    assertEquals(1, Roster.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
  }

  @Test
  public void selectedOffRosterPersonCountsOnlyInHolidayStats() {
    State s = sample();
    s.holidayWork.add(new Duty(d("2026-10-29"), WORK, "a"));
    assertEquals(2, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
    assertEquals(0, Roster.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
    s.holidayWork.clear();
    assertEquals(0, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
  }

  @Test
  public void duplicateConfirmationDoesNotDoubleCountAndHalfDayWeightRemains() {
    State s = sample();
    for (int i = 0; i < 2; i++) s.holidayWork.add(new Duty(d("2026-10-28"), WORK, "a"));
    assertEquals(1, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts[0]);
  }

  @Test
  public void deletedPlaceWithOnlyActualWorkIsStillReported() {
    State s = sample();
    s.places.add(new Place("old", "Eski Şube", 31, 1));
    s.place("old").deleted = true;
    s.holidayWork.add(new Duty(d("2026-10-29"), "old", "a"));
    assertEquals("old", Holidays.places(s).get(2).id);
    assertEquals(2, Holidays.statistics(s, d("2026-12-31"), false).get(1).counts[2]);
  }
}
