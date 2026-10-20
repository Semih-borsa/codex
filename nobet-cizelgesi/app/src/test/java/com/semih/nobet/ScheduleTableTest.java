package com.semih.nobet;

import static com.semih.nobet.Roster.*;
import static org.junit.Assert.*;

import java.time.LocalDate;
import org.junit.Test;

public class ScheduleTableTest {
  State state() {
    State s = new State();
    s.people.add(new Person("a", "Ahmet"));
    s.people.add(new Person("b", "Elif"));
    return s;
  }

  Week week() {
    return new Week(LocalDate.of(2026, 10, 19));
  }

  @Test
  public void fiveWeekdaysAndOneWeekendCoverAllSevenDates() {
    Week w = week();
    int count = 0;
    for (int i = 0; i < 6; i++) {
      assertEquals(i == 5 ? 2 : 1, ScheduleTable.dates(w, i).size());
      count += ScheduleTable.dates(w, i).size();
    }
    assertEquals(7, count);
    assertEquals(w.monday.plusDays(6), ScheduleTable.dates(w, 5).get(1));
  }

  @Test
  public void sameWeekendPersonAppearsOnceButDutiesStaySeparate() {
    State s = state();
    Week w = week();
    w.duties.add(new Duty(w.monday.plusDays(5), WORK, "a"));
    w.duties.add(new Duty(w.monday.plusDays(6), WORK, "a"));
    assertEquals("Ahmet", ScheduleTable.person(s, w, 5, WORK));
    assertEquals(2, w.duties.size());
  }

  @Test
  public void differentWeekendAssignmentsNeverDisappear() {
    State s = state();
    Week w = week();
    s.place(BRANCH).days = 127;
    w.duties.add(new Duty(w.monday.plusDays(5), BRANCH, "a"));
    w.duties.add(new Duty(w.monday.plusDays(6), BRANCH, "b"));
    assertEquals("Cmt: Ahmet\nPaz: Elif", ScheduleTable.person(s, w, 5, BRANCH));
  }

  @Test
  public void closedSaturdayAndOpenSundayShowsSundayPerson() {
    State s = state();
    Week w = week();
    s.place(WORK).days = 64;
    w.duties.add(new Duty(w.monday.plusDays(6), WORK, "b"));
    assertEquals("Elif", ScheduleTable.person(s, w, 5, WORK));
  }

  @Test
  public void unassignedOpenDayNotSilentlyHidden() {
    State s = state();
    Week w = week();
    w.duties.add(new Duty(w.monday.plusDays(5), WORK, "a"));
    assertEquals("Cmt: Ahmet\nPaz: —", ScheduleTable.person(s, w, 5, WORK));
  }

  @Test
  public void emptySlotsAreDashesAndDatesCrossYears() {
    State s = state();
    Week w = new Week(LocalDate.of(2026, 12, 28));
    assertEquals("—", ScheduleTable.person(s, w, 5, WORK));
    assertEquals(LocalDate.of(2027, 1, 3), ScheduleTable.dates(w, 5).get(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidRowRejected() {
    ScheduleTable.dates(week(), 6);
  }
}
