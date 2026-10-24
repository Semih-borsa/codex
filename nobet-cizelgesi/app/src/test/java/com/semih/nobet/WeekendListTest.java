package com.semih.nobet;

import static com.semih.nobet.Roster.*;
import static org.junit.Assert.*;

import java.time.*;
import org.junit.Test;

public class WeekendListTest {
  State state() {
    State s = new State();
    s.people.add(new Person("p1", "Semih"));
    s.people.add(new Person("p2", "Serdar"));
    return s;
  }

  Week week(LocalDate sat, String a, String b) {
    Week w = new Week(sat.minusDays(5));
    if (a != null) w.duties.add(new Duty(sat, WORK, a));
    if (b != null) w.duties.add(new Duty(sat.plusDays(1), WORK, b));
    return w;
  }

  @Test
  public void monthBoundaryAndChronologicalNames() {
    State s = state();
    s.weeks.add(week(LocalDate.of(2024, 9, 7), "p2", "p2"));
    s.weeks.add(week(LocalDate.of(2024, 8, 31), "p1", "p1"));
    assertEquals("31 Ağustos–1 Eylül 2024 · Semih", WeekendList.entries(s).get(0).label);
    assertEquals("7–8 Eylül 2024 · Serdar", WeekendList.entries(s).get(1).label);
    assertEquals(LocalDate.of(2024, 8, 26), WeekendList.entries(s).get(0).monday);
  }

  @Test
  public void splitDutiesAndMissingDayAreExplicit() {
    State s = state();
    s.weeks.add(week(LocalDate.of(2026, 10, 24), "p1", null));
    assertTrue(WeekendList.entries(s).get(0).label.contains("Cumartesi: Semih / Pazar: —"));
  }

  @Test
  public void yearBoundaryKeepsBothYears() {
    State s = state();
    s.weeks.add(week(LocalDate.of(2022, 12, 31), "p1", "p2"));
    assertTrue(WeekendList.entries(s).get(0).label.startsWith("31 Aralık 2022–1 Ocak 2023"));
    assertTrue(WeekendList.entries(s).get(0).label.contains("Pazar: Serdar"));
  }

  @Test
  public void noFutureGuessesAndBranchNotTreatedAsWork() {
    State s = state();
    assertTrue(WeekendList.entries(s).isEmpty());
    Week w = week(LocalDate.of(2026, 10, 24), null, null);
    w.duties.add(new Duty(LocalDate.of(2026, 10, 24), BRANCH, "p1"));
    s.weeks.add(w);
    assertTrue(WeekendList.entries(s).get(0).label.endsWith(" · —"));
  }
}
