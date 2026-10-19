package com.semih.nobet;

import static com.semih.nobet.Roster.*;
import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class RosterTest {
  LocalDate mon = LocalDate.of(2026, 10, 19);

  State people(int count) {
    State s = new State();
    for (int i = 0; i < count; i++) s.people.add(new Person("p" + i, "Personel " + i));
    return s;
  }

  String weekendPerson(Week w) {
    return w.duties.stream()
        .filter(d -> d.date.getDayOfWeek() == DayOfWeek.SATURDAY)
        .map(d -> d.person)
        .findFirst()
        .orElse("");
  }

  @Test
  public void sevenDaysTwelveSlots() {
    State s = people(5);
    Week w = generate(s, mon);
    assertEquals(12, w.duties.size());
    assertTrue(validate(s, w).isEmpty());
    assertEquals("p0", weekendPerson(w));
    for (Person p : s.people)
      assertEquals(
          1,
          w.duties.stream().filter(d -> d.person.equals(p.id) && d.place.equals(BRANCH)).count());
  }

  @Test
  public void weekendSamePersonAndThursdayFridayOff() {
    State s = people(5);
    Week w = generate(s, mon);
    String id = weekendPerson(w);
    assertEquals(2, w.duties.stream().filter(d -> d.person.equals(id) && weekend(d.date)).count());
    assertFalse(
        w.duties.stream()
            .anyMatch(
                d ->
                    d.person.equals(id)
                        && (d.date.equals(mon.plusDays(3)) || d.date.equals(mon.plusDays(4)))));
  }

  @Test
  public void annualLeaveBlocksBothLocationsInclusive() {
    State s = people(5);
    s.leaves.add(new Leave("p1", ALL, mon, mon.plusDays(6), "Senelik izin"));
    Week w = generate(s, mon);
    assertFalse(w.duties.stream().anyMatch(d -> d.person.equals("p1")));
    assertTrue(validate(s, w).isEmpty());
    assertTrue(w.warnings.stream().anyMatch(x -> x.contains("şubeye atanmadı")));
  }

  @Test
  public void reportBoundaries() {
    State s = people(5);
    s.leaves.add(new Leave("p1", ALL, mon.plusDays(1), mon.plusDays(3), "Rapor"));
    assertTrue(available(s, "p1", WORK, mon));
    assertFalse(available(s, "p1", BRANCH, mon.plusDays(1)));
    assertFalse(available(s, "p1", WORK, mon.plusDays(3)));
    assertTrue(available(s, "p1", WORK, mon.plusDays(4)));
  }

  @Test
  public void owedWeekendFirstAfterEndThenRotationResumes() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon.plusDays(5), mon.plusDays(9), "Rapor"));
    Week first = generate(s, mon);
    assertEquals("p1", weekendPerson(first));
    assertEquals(1, first.debts.size());
    approve(s, first);
    Week next = generate(s, mon.plusWeeks(1));
    assertEquals("p0", weekendPerson(next));
    assertTrue(next.debts.isEmpty());
    approve(s, next);
    assertEquals("p2", weekendPerson(generate(s, mon.plusWeeks(2))));
  }

  @Test
  public void sundayEndDefersUntilFollowingWeekend() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon.plusDays(5), mon.plusDays(13), "Rapor"));
    approve(s, generate(s, mon));
    assertNotEquals("p0", weekendPerson(generate(s, mon.plusWeeks(1))));
    approve(s, generate(s, mon.plusWeeks(1)));
    assertEquals("p0", weekendPerson(generate(s, mon.plusWeeks(2))));
  }

  @Test
  public void noForcedAssignmentWhenAllUnavailable() {
    State s = people(5);
    for (Person p : s.people) s.leaves.add(new Leave(p.id, ALL, mon, mon.plusDays(6), "İzin"));
    Week w = generate(s, mon);
    assertTrue(w.duties.isEmpty());
    assertFalse(w.warnings.isEmpty());
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void branchOnlyConstraintStillAllowsWork() {
    State s = people(5);
    s.leaves.add(new Leave("p2", BRANCH, mon, mon.plusDays(4), "Muafiyet"));
    Week w = generate(s, mon);
    assertFalse(w.duties.stream().anyMatch(d -> d.person.equals("p2") && d.place.equals(BRANCH)));
    assertTrue(available(s, "p2", WORK, mon));
  }

  @Test
  public void noSimultaneousLocations() {
    State s = people(5);
    Week w = generate(s, mon);
    w.duties.add(
        new Duty(
            mon,
            BRANCH,
            w.duties.stream()
                .filter(d -> d.date.equals(mon) && d.place.equals(WORK))
                .findFirst()
                .get()
                .person));
    assertFalse(validate(s, w).isEmpty());
  }

  @Test
  public void manualThursdayWeekendConflictRejected() {
    State s = people(5);
    Week w = generate(s, mon);
    w.duties.removeIf(d -> d.date.equals(mon.plusDays(3)) && d.place.equals(WORK));
    w.duties.add(new Duty(mon.plusDays(3), WORK, weekendPerson(w)));
    assertTrue(validate(s, w).stream().anyMatch(e -> e.contains("perşembe/cuma")));
  }

  @Test
  public void inactiveExcludedButHistoryRetained() {
    State s = people(5);
    approve(s, generate(s, mon));
    s.people.get(0).active = false;
    Week w = generate(s, mon.plusWeeks(1));
    assertFalse(w.duties.stream().anyMatch(d -> d.person.equals("p0")));
    assertTrue(s.weeks.get(0).duties.stream().anyMatch(d -> d.person.equals("p0")));
  }

  @Test
  public void previewDoesNotChangeState() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon, mon.plusDays(6), "Rapor"));
    generate(s, mon);
    assertTrue(s.weeks.isEmpty());
  }

  @Test
  public void pastEditInvalidatesLaterWeeks() {
    State s = people(5);
    approve(s, generate(s, mon));
    approve(s, generate(s, mon.plusWeeks(1)));
    approve(s, generate(s, mon));
    assertEquals(1, s.weeks.size());
  }

  @Test
  public void hundredWeeksFairAndValid() {
    State s = people(5);
    Map<String, Integer> weekend = new HashMap<>(), weekday = new HashMap<>();
    for (int n = 0; n < 100; n++) {
      Week w = generate(s, mon.plusWeeks(n));
      assertEquals(12, w.duties.size());
      assertTrue(validate(s, w).isEmpty());
      weekend.merge(weekendPerson(w), 1, Integer::sum);
      for (Duty d : w.duties)
        if (d.place.equals(WORK) && !weekend(d.date)) weekday.merge(d.person, 1, Integer::sum);
      approve(s, w);
    }
    assertEquals(1, new HashSet<>(weekend.values()).size());
    assertTrue(Collections.max(weekday.values()) - Collections.min(weekday.values()) <= 2);
  }

  @Test
  public void sixPeopleStayWithinOneBranchDay() {
    State s = people(6);
    Week w = generate(s, mon);
    assertEquals(12, w.duties.size());
    assertTrue(validate(s, w).isEmpty());
    assertTrue(w.warnings.stream().anyMatch(x -> x.contains("şubeye atanmadı")));
  }

  @Test
  public void emptyPersonnelRejected() {
    try {
      generate(people(0), mon);
      fail();
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("personel"));
    }
  }

  @Test
  public void branchSolverHonorsConstrainedPerson() {
    State s = people(5);
    for (int i = 1; i < 5; i++)
      s.leaves.add(new Leave("p4", BRANCH, mon.plusDays(i), mon.plusDays(i), "Kısıt"));
    Week w = generate(s, mon);
    assertEquals(5, w.duties.stream().filter(d -> d.place.equals(BRANCH)).count());
    assertTrue(
        w.duties.stream()
            .anyMatch(d -> d.person.equals("p4") && d.place.equals(BRANCH) && d.date.equals(mon)));
  }

  @Test
  public void manualWeekendReplacementHonorsRest() {
    State s = people(5);
    Week w = generate(s, mon, "p3");
    assertEquals("p3", weekendPerson(w));
    assertTrue(validate(s, w).isEmpty());
    assertFalse(
        w.duties.stream()
            .anyMatch(
                d ->
                    d.person.equals("p3")
                        && (d.date.equals(mon.plusDays(3)) || d.date.equals(mon.plusDays(4)))));
  }

  @Test
  public void manualWeekendCannotOverrideReport() {
    State s = people(5);
    s.leaves.add(new Leave("p3", ALL, mon, mon.plusDays(6), "Rapor"));
    try {
      generate(s, mon, "p3");
      fail();
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("mazereti"));
    }
  }

  @Test
  public void cloneIsIndependent() {
    State s = people(5);
    Week w = generate(s, mon);
    Week copy = copy(w);
    copy.duties.clear();
    assertEquals(12, w.duties.size());
  }
}
