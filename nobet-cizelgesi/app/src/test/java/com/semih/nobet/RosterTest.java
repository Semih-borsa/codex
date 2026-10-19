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
  public void pastEditPreservesLaterWeeks() {
    State s = people(5);
    approve(s, generate(s, mon));
    approve(s, generate(s, mon.plusWeeks(1)));
    approve(s, generate(s, mon));
    assertEquals(2, s.weeks.size());
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
  public void renamedPlaceKeepsHistoryAndStatistics() {
    State s = people(5);
    Week w = generate(s, mon);
    approve(s, w);
    long[] before = totals(s, "p0", WORK, mon.plusDays(6));
    s.place(WORK).name = "Merkez";
    assertEquals("Merkez", s.placeName(WORK));
    assertArrayEquals(before, totals(s, "p0", WORK, mon.plusDays(6)));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void extraBranchAssignedWithNoConflicts() {
    State s = people(5);
    s.places.add(new Place("new", "Ek Bina", 31, 1));
    Week w = generate(s, mon);
    assertEquals(17, expectedSlots(s));
    assertEquals(5, w.duties.stream().filter(d -> d.place.equals("new")).count());
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void extraPlaceHonorsDaysAndGlobalLeave() {
    State s = people(5);
    s.places.add(new Place("new", "Ek Bina", 5, 1));
    s.leaves.add(new Leave("p2", ALL, mon, mon.plusDays(6), "Rapor"));
    Week w = generate(s, mon);
    assertFalse(w.duties.stream().anyMatch(d -> d.person.equals("p2")));
    assertTrue(
        w.duties.stream()
            .filter(d -> d.place.equals("new"))
            .allMatch(d -> d.date.equals(mon) || d.date.equals(mon.plusDays(2))));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void manualPlaceDoesNotAutoAssign() {
    State s = people(5);
    Place p = new Place("new", "Ek", 127, 0);
    p.automatic = false;
    s.places.add(p);
    assertFalse(generate(s, mon).duties.stream().anyMatch(d -> d.place.equals("new")));
  }

  @Test
  public void savedRevisionUpdatesCountsWithoutDuplicatingOrLosingWeeks() {
    State s = people(5);
    Week w = generate(s, mon);
    approve(s, w);
    approve(s, generate(s, mon.plusWeeks(1)));
    Duty d =
        w.duties.stream().filter(x -> !weekend(x.date) && x.place.equals(WORK)).findFirst().get();
    long before = totals(s, d.person, WORK, mon.plusWeeks(3))[0];
    Week edit = copy(w);
    edit.duties.removeIf(x -> x.date.equals(d.date) && x.place.equals(WORK));
    approve(s, edit);
    assertEquals(before - 1, totals(s, d.person, WORK, mon.plusWeeks(3))[0]);
    assertEquals(2, s.weeks.size());
    approve(s, edit);
    assertEquals(before - 1, totals(s, d.person, WORK, mon.plusWeeks(3))[0]);
    assertEquals(2, s.weeks.size());
  }

  @Test
  public void newBranchWeekendCannotUseRestingPersonOnThursday() {
    State s = people(5);
    s.places.add(new Place("new", "Ek", 127, 0));
    Week w = generate(s, mon);
    String id = weekendPerson(w);
    assertFalse(
        w.duties.stream().anyMatch(d -> d.person.equals(id) && d.date.equals(mon.plusDays(3))));
  }

  @Test
  public void pendingTurnFromEditedOlderWeekSurvivesLaterSavedWeek() {
    State s = people(5);
    Week old = generate(s, mon);
    approve(s, old);
    approve(s, generate(s, mon.plusWeeks(1)));
    old.duties.removeIf(d -> weekend(d.date) && d.place.equals(WORK));
    old.debts.add(new Debt("p0", mon.plusDays(6)));
    assertEquals("p0", weekendPerson(generate(s, mon.plusWeeks(2))));
    assertEquals(2, s.weeks.size());
  }

  @Test
  public void reassignmentChangesBothPersonnelCounts() {
    State s = people(5);
    Week w = generate(s, mon);
    approve(s, w);
    Duty d =
        w.duties.stream().filter(x -> x.date.equals(mon) && x.place.equals(WORK)).findFirst().get();
    Person replacement =
        s.people.stream()
            .filter(
                p ->
                    !p.id.equals(d.person)
                        && w.duties.stream()
                            .noneMatch(x -> x.date.equals(mon) && x.person.equals(p.id)))
            .findFirst()
            .get();
    long old = totals(s, d.person, WORK, mon.plusDays(6))[0],
        other = totals(s, replacement.id, WORK, mon.plusDays(6))[0];
    Week revised = copy(w);
    revised.duties.removeIf(x -> x.date.equals(mon) && x.place.equals(WORK));
    revised.duties.add(new Duty(mon, WORK, replacement.id));
    approve(s, revised);
    assertEquals(old - 1, totals(s, d.person, WORK, mon.plusDays(6))[0]);
    assertEquals(other + 1, totals(s, replacement.id, WORK, mon.plusDays(6))[0]);
  }

  @Test
  public void secondBranchAssignmentNeedsExplicitException() {
    State s = people(5);
    Week w = generate(s, mon);
    Duty first = w.duties.stream().filter(d -> d.place.equals(BRANCH)).findFirst().get();
    Person person = s.people.stream().filter(p -> p.id.equals(first.person)).findFirst().get();
    LocalDate target = null;
    for (int day = 0; day < 5; day++) {
      LocalDate d = mon.plusDays(day);
      if (!d.equals(first.date) && free(s, w, person, BRANCH, d)) {
        target = d;
        break;
      }
    }
    assertNotNull(target);
    LocalDate date = target;
    w.duties.removeIf(d -> d.place.equals(BRANCH) && d.date.equals(date));
    w.duties.add(new Duty(date, BRANCH, person.id));
    assertTrue(validate(s, w).stream().anyMatch(e -> e.contains("haftalık sınırı")));
    w.limitOverrides.add(BRANCH + "|" + person.id);
    assertTrue(validate(s, w).isEmpty());
    approve(s, w);
    assertEquals(2, totals(s, person.id, BRANCH, mon.plusDays(6))[0]);
    assertTrue(copy(w).limitOverrides.contains(BRANCH + "|" + person.id));
  }

  @Test
  public void manualExceptionDoesNotDisableLeaveOrDoubleDutyChecks() {
    State s = people(5);
    Week w = generate(s, mon);
    String id = weekendPerson(w);
    w.limitOverrides.add(BRANCH + "|" + id);
    w.duties.add(new Duty(mon.plusDays(3), BRANCH, id));
    assertFalse(validate(s, w).isEmpty());
  }

  @Test
  public void statisticRowsAreTurkishAlphabetical() {
    State s = new State();
    String[] names = {"Zeynep", "Ömer", "İpek", "Işık", "Çağrı", "Ali"};
    for (int i = 0; i < names.length; i++) s.people.add(new Person("p" + i, names[i]));
    List<StatRow> rows = statistics(s, mon, false);
    assertArrayEquals(
        new String[] {"Ali", "Çağrı", "Işık", "İpek", "Ömer", "Zeynep"},
        rows.stream().map(r -> r.person.name).toArray(String[]::new));
  }

  @Test
  public void statisticTableSeparatesWorkdaysWeekendsAndNamedBranches() {
    State s = people(1);
    s.place(BRANCH).name = "Merkez Şube";
    s.places.add(new Place("new", "Ek Şube", 31, 0));
    Week w = new Week(mon);
    w.duties.add(new Duty(mon, WORK, "p0"));
    w.duties.add(new Duty(mon.plusDays(5), WORK, "p0"));
    w.duties.add(new Duty(mon.plusDays(1), BRANCH, "p0"));
    w.duties.add(new Duty(mon.plusDays(2), "new", "p0"));
    s.weeks.add(w);
    assertArrayEquals(new long[] {1, 1, 1, 1}, statistics(s, mon.plusDays(6), false).get(0).counts);
    assertEquals("Merkez Şube", statisticPlaces(s).get(0).name);
    assertEquals("Ek Şube", statisticPlaces(s).get(1).name);
    assertArrayEquals(new long[] {0, 1, 1, 1}, statistics(s, mon, true).get(0).counts);
  }

  @Test
  public void originalWorkDaysAreRespected() {
    State s = people(5);
    s.place(WORK).days = 5;
    Week w = generate(s, mon);
    assertEquals(2, w.duties.stream().filter(d -> d.place.equals(WORK)).count());
    assertTrue(
        w.duties.stream()
            .filter(d -> d.place.equals(WORK))
            .allMatch(d -> d.date.equals(mon) || d.date.equals(mon.plusDays(2))));
    assertTrue(validate(s, w).isEmpty());
    assertEquals(7, expectedSlots(s));
  }

  @Test
  public void originalBranchCanSelectWeekendDays() {
    State s = people(5);
    s.place(BRANCH).days = 96;
    Week w = generate(s, mon);
    assertEquals(2, w.duties.stream().filter(d -> d.place.equals(BRANCH)).count());
    assertTrue(
        w.duties.stream().filter(d -> d.place.equals(BRANCH)).allMatch(d -> weekend(d.date)));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void saturdayOnlyMainWeekendIsValid() {
    State s = people(5);
    s.place(WORK).days = 63;
    Week w = generate(s, mon);
    assertEquals(6, w.duties.stream().filter(d -> d.place.equals(WORK)).count());
    assertFalse(
        w.duties.stream()
            .anyMatch(d -> d.place.equals(WORK) && d.date.getDayOfWeek() == DayOfWeek.SUNDAY));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void sundayOnlyIgnoresClosedSaturdayLeave() {
    State s = people(5);
    s.place(WORK).days = 64;
    s.leaves.add(new Leave("p0", ALL, mon.plusDays(5), mon.plusDays(5), "Rapor"));
    Week w = generate(s, mon);
    assertTrue(
        w.duties.stream()
            .anyMatch(
                d ->
                    d.place.equals(WORK)
                        && d.person.equals("p0")
                        && d.date.equals(mon.plusDays(6))));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void sundayOnlySettlesPendingTurn() {
    State s = people(5);
    s.place(WORK).days = 64;
    Week previous = new Week(mon.minusWeeks(1));
    previous.cursor = "p1";
    previous.debts.add(new Debt("p0", mon.plusDays(5)));
    s.weeks.add(previous);
    Week w = generate(s, mon);
    assertTrue(w.duties.stream().anyMatch(d -> d.place.equals(WORK) && d.person.equals("p0")));
    approve(s, w);
    assertTrue(pendingDebts(s, mon.plusWeeks(1)).isEmpty());
  }

  @Test
  public void allDaysCanBeTurnedOffWithoutAdvancingRotation() {
    State s = people(5);
    s.place(WORK).days = 0;
    s.place(BRANCH).days = 0;
    Week w = generate(s, mon);
    assertTrue(w.duties.isEmpty());
    assertTrue(w.cursor.isEmpty());
    assertEquals(0, expectedSlots(s));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void deletingExtraPlaceRetainsPastAndClearsOnlyFuture() {
    State s = people(5);
    s.places.add(new Place("extra", "Ek", 31, 1));
    Week old = new Week(mon);
    old.duties.add(new Duty(mon, "extra", "p0"));
    old.duties.add(new Duty(mon.plusDays(3), "extra", "p1"));
    s.weeks.add(old);
    deletePlace(s, "extra", mon.plusDays(2));
    assertTrue(s.place("extra").deleted);
    assertEquals(1, old.duties.size());
    assertEquals(1, totals(s, "p0", "extra", mon.plusDays(2))[0]);
    assertTrue(statisticPlaces(s).stream().anyMatch(p -> p.id.equals("extra")));
    Week next = generate(s, mon.plusWeeks(1));
    assertFalse(next.duties.stream().anyMatch(d -> d.place.equals("extra")));
    assertFalse(displayPlaces(s, next).stream().anyMatch(p -> p.id.equals("extra")));
    assertTrue(displayPlaces(s, old).stream().anyMatch(p -> p.id.equals("extra")));
    s.place("extra").deleted = false;
    assertEquals(
        5,
        generate(s, mon.plusWeeks(1)).duties.stream().filter(d -> d.place.equals("extra")).count());
    assertEquals(1, old.duties.size());
  }

  @Test
  public void hiddenBranchesKeepDutiesAndStatistics() {
    State s = people(5);
    s.places.add(new Place("extra", "Ek", 31, 1));
    Week w = generate(s, mon);
    approve(s, w);
    long[] before = totals(s, "p0", "extra", mon.plusDays(6));
    s.hiddenPlaces.add(BRANCH);
    s.hiddenPlaces.add("extra");
    assertEquals(1, displayPlaces(s, w).size());
    assertEquals(17, w.duties.size());
    assertEquals(2, statisticPlaces(s).size());
    assertArrayEquals(before, totals(s, "p0", "extra", mon.plusDays(6)));
    assertEquals(17, generate(s, mon.plusWeeks(1)).duties.size());
  }

  @Test
  public void originalRolesCannotBeDeleted() {
    State s = people(5);
    try {
      deletePlace(s, WORK, mon);
      fail();
    } catch (IllegalArgumentException expected) {
      assertFalse(s.place(WORK).deleted);
    }
    try {
      deletePlace(s, BRANCH, mon);
      fail();
    } catch (IllegalArgumentException expected) {
      assertFalse(s.place(BRANCH).deleted);
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

  @Test
  public void configuredWeekendOrderRepeatsInChosenOrder() {
    State s = people(5);
    List<String> order = Arrays.asList("p3", "p1", "p4", "p0", "p2");
    setWeekendOrder(s, order);
    for (int i = 0; i < 10; i++) {
      Week w = generate(s, mon.plusWeeks(i));
      assertEquals(order.get(i % 5), weekendPerson(w));
      approve(s, w);
    }
    assertEquals("p0", s.people.get(0).id); // Weekday staffing list is independent.
  }

  @Test
  public void reorderedRotationRestartsWithoutChangingSavedWeeks() {
    State s = people(5);
    Week original = generate(s, mon);
    approve(s, original);
    setWeekendOrder(s, Arrays.asList("p4", "p3", "p2", "p1", "p0"));
    Week next = generate(s, mon.plusWeeks(1));
    assertEquals("p4", weekendPerson(next));
    assertEquals("p0", weekendPerson(s.weeks.get(0)));
    approve(s, next);
    assertEquals("p3", weekendPerson(generate(s, mon.plusWeeks(2))));
    assertEquals(next.rotationRevision, copy(next).rotationRevision);
  }

  @Test
  public void newPersonnelCanBeInsertedAtChosenPosition() {
    State s = people(5);
    s.people.add(new Person("new", "Yeni Personel"));
    setWeekendOrder(s, Arrays.asList("p0", "new", "p1", "p2", "p3", "p4"));
    approve(s, generate(s, mon));
    assertEquals("new", weekendPerson(generate(s, mon.plusWeeks(1))));
  }

  @Test
  public void configuredOrderSkipsInactivePersonnel() {
    State s = people(5);
    setWeekendOrder(s, Arrays.asList("p4", "p3", "p2", "p1", "p0"));
    s.people.get(4).active = false;
    assertEquals("p3", weekendPerson(generate(s, mon)));
  }

  @Test
  public void customOrderPreservesLeaveDebtAndRegularCursor() {
    State s = people(5);
    setWeekendOrder(s, Arrays.asList("p2", "p4", "p1", "p3", "p0"));
    s.leaves.add(new Leave("p2", ALL, mon, mon.plusDays(6), "Rapor"));
    Week first = generate(s, mon);
    assertEquals("p4", weekendPerson(first));
    approve(s, first);
    Week owed = generate(s, mon.plusWeeks(1));
    assertEquals("p2", weekendPerson(owed));
    approve(s, owed);
    assertEquals("p1", weekendPerson(generate(s, mon.plusWeeks(2))));
    assertTrue(validate(s, owed).isEmpty());
  }

  @Test
  public void invalidOrderNeverMutatesConfiguration() {
    State s = people(3);
    for (List<String> bad :
        Arrays.asList(
            Arrays.asList("p0", "p0", "p2"),
            Arrays.asList("p0", "p1"),
            Arrays.asList("p0", "p1", "unknown"))) {
      try {
        setWeekendOrder(s, bad);
        fail("Invalid order accepted");
      } catch (IllegalArgumentException expected) {
      }
      assertTrue(s.weekendOrder.isEmpty());
      assertEquals(0, s.rotationRevision);
    }
  }

  @Test
  public void weeklyDaysRepeatWithoutBlockingOtherDays() {
    Leave l = new Leave("p0", ALL, mon, LocalDate.MAX, "Haftalık Kısıt", 5);
    for (int week = 0; week < 12; week++) {
      assertTrue(l.blocks("p0", WORK, mon.plusWeeks(week)));
      assertTrue(l.blocks("p0", BRANCH, mon.plusWeeks(week).plusDays(2)));
      assertFalse(l.blocks("p0", WORK, mon.plusWeeks(week).plusDays(1)));
      assertFalse(l.blocks("p1", WORK, mon.plusWeeks(week)));
    }
  }

  @Test
  public void weeklyDateRangeIsInclusive() {
    Leave l = new Leave("p0", BRANCH, mon.plusWeeks(1), mon.plusWeeks(2), "Kısıt", 1);
    assertFalse(l.blocks("p0", BRANCH, mon));
    assertTrue(l.blocks("p0", BRANCH, mon.plusWeeks(1)));
    assertTrue(l.blocks("p0", BRANCH, mon.plusWeeks(2)));
    assertFalse(l.blocks("p0", BRANCH, mon.plusWeeks(3)));
    assertFalse(l.blocks("p0", WORK, mon.plusWeeks(1)));
  }

  @Test
  public void multipleRestrictionsForOnePersonAccumulateIndependently() {
    State s = people(5);
    Leave a = new Leave("p0", BRANCH, mon, LocalDate.MAX, "Şube", 1);
    Leave b = new Leave("p0", WORK, mon, LocalDate.MAX, "İşyeri", 4);
    Leave c = new Leave("p0", ALL, mon.plusDays(4), mon.plusDays(4), "Rapor");
    s.leaves.addAll(Arrays.asList(a, b, c));
    assertFalse(available(s, "p0", BRANCH, mon));
    assertTrue(available(s, "p0", WORK, mon));
    assertFalse(available(s, "p0", WORK, mon.plusDays(2)));
    assertTrue(available(s, "p0", BRANCH, mon.plusDays(2)));
    assertFalse(available(s, "p0", BRANCH, mon.plusDays(4)));
    assertFalse(available(s, "p0", WORK, mon.plusDays(4)));
    s.leaves.remove(a);
    assertTrue(available(s, "p0", BRANCH, mon));
    assertFalse(available(s, "p0", WORK, mon.plusDays(2)));
    assertEquals(2, s.leaves.size());
  }

  @Test
  public void generatedWeeksRespectRecurringRestrictions() {
    State s = people(5);
    Leave a = new Leave("p0", ALL, mon, LocalDate.MAX, "Haftalık", 5);
    Leave b = new Leave("p1", BRANCH, mon, LocalDate.MAX, "Haftalık", 10);
    s.leaves.addAll(Arrays.asList(a, b));
    for (int i = 0; i < 8; i++) {
      Week w = generate(s, mon.plusWeeks(i));
      for (Duty d : w.duties) {
        assertFalse(a.blocks(d.person, d.place, d.date));
        assertFalse(b.blocks(d.person, d.place, d.date));
      }
      assertTrue(validate(s, w).isEmpty());
      approve(s, w);
    }
  }

  @Test
  public void manualAssignmentsCannotOverrideRecurringRestriction() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon, LocalDate.MAX, "Haftalık", 1));
    Week w = new Week(mon);
    w.duties.add(new Duty(mon, WORK, "p0"));
    assertFalse(validate(s, w).isEmpty());
  }

  @Test
  public void weekendWeeklyRestrictionKeepsOwedTurnAfterEnd() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon, mon.plusDays(6), "Haftalık", 32));
    Week first = generate(s, mon);
    assertEquals("p1", weekendPerson(first));
    approve(s, first);
    Week next = generate(s, mon.plusWeeks(1));
    assertEquals("p0", weekendPerson(next));
    assertTrue(validate(s, next).isEmpty());
  }

  @Test
  public void invalidRecurringMaskIsRejected() {
    for (int days : new int[] {-1, 128, 255}) {
      try {
        new Leave("p0", ALL, mon, LocalDate.MAX, "Haftalık", days);
        fail("Invalid mask accepted");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void leaveEndingBeforeOwnTurnNeverCreatesWeekendPriority() {
    State s = people(5);
    s.leaves.add(new Leave("p4", ALL, mon, mon.plusDays(6), "Senelik İzin"));
    for (int i = 0; i < 5; i++) {
      Week w = generate(s, mon.plusWeeks(i));
      assertEquals("p" + i, weekendPerson(w));
      assertTrue(w.debts.isEmpty());
      approve(s, w);
    }
  }

  @Test
  public void reportOverSeveralWeekendsButBeforeOwnTurnDoesNotSkipQueue() {
    State s = people(5);
    s.leaves.add(new Leave("p3", ALL, mon, mon.plusDays(13), "Rapor"));
    for (int i = 0; i < 4; i++) {
      Week w = generate(s, mon.plusWeeks(i));
      assertEquals("p" + i, weekendPerson(w));
      assertTrue(w.debts.isEmpty());
      approve(s, w);
    }
  }

  @Test
  public void onlyActuallySkippedPersonReturnsBeforeRegularQueue() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon, mon.plusDays(9), "Rapor"));
    s.leaves.add(new Leave("p4", ALL, mon, mon.plusDays(9), "Senelik İzin"));
    Week first = generate(s, mon);
    assertEquals("p1", weekendPerson(first));
    assertEquals(1, first.debts.size());
    assertEquals("p0", first.debts.get(0).person);
    approve(s, first);
    Week returned = generate(s, mon.plusWeeks(1));
    assertEquals("p0", weekendPerson(returned));
    assertTrue(returned.debts.isEmpty());
    approve(s, returned);
    assertEquals("p2", weekendPerson(generate(s, mon.plusWeeks(2))));
  }

  @Test
  public void skippedTurnWaitsForEndThenRegularQueueContinuesUnconsumed() {
    State s = people(5);
    s.leaves.add(new Leave("p0", ALL, mon, mon.plusDays(13), "Rapor"));
    String[] expected = {"p1", "p2", "p0", "p3", "p4"};
    for (int i = 0; i < expected.length; i++) {
      Week w = generate(s, mon.plusWeeks(i));
      assertEquals(expected[i], weekendPerson(w));
      assertTrue(validate(s, w).isEmpty());
      approve(s, w);
    }
  }

  @Test
  public void weeklyRestrictionUsesActualDateAcrossYears() {
    Leave l = new Leave("p0", ALL, LocalDate.of(2026, 12, 1), LocalDate.MAX, "Haftalık Kısıt", 5);
    LocalDate start = LocalDate.of(2026, 12, 20);
    for (int i = 0; i < 420; i++) {
      LocalDate day = start.plusDays(i);
      boolean blocked =
          day.getDayOfWeek() == DayOfWeek.MONDAY || day.getDayOfWeek() == DayOfWeek.WEDNESDAY;
      assertEquals(blocked, l.blocks("p0", WORK, day));
      assertEquals(blocked, l.blocks("p0", BRANCH, day));
    }
  }

  @Test
  public void independentWeeklyRowsApplyOnlyTheirOwnLocationsAndDays() {
    State s = people(5);
    s.leaves.add(new Leave("p0", BRANCH, mon, LocalDate.MAX, "Haftalık Kısıt", 5));
    s.leaves.add(new Leave("p0", WORK, mon, LocalDate.MAX, "Haftalık Kısıt", 16));
    for (int week = 0; week < 6; week++) {
      LocalDate start = mon.plusWeeks(week);
      assertFalse(available(s, "p0", BRANCH, start));
      assertTrue(available(s, "p0", WORK, start));
      assertFalse(available(s, "p0", WORK, start.plusDays(4)));
      assertTrue(available(s, "p0", BRANCH, start.plusDays(4)));
      Week w = generate(s, start);
      assertTrue(validate(s, w).isEmpty());
      for (Duty d : w.duties)
        if (d.person.equals("p0")) {
          assertFalse(
              d.place.equals(BRANCH)
                  && (d.date.getDayOfWeek() == DayOfWeek.MONDAY
                      || d.date.getDayOfWeek() == DayOfWeek.WEDNESDAY));
          assertFalse(d.place.equals(WORK) && d.date.getDayOfWeek() == DayOfWeek.FRIDAY);
        }
      approve(s, w);
    }
  }

  private State multiPlaceState() {
    State s = people(5);
    s.places.add(new Place("extra", "Ek Şube", 31, 0));
    return s;
  }

  private Week twoPlaces(State s) {
    Week w = new Week(mon);
    w.duties.add(new Duty(mon, BRANCH, "p0"));
    w.duties.add(new Duty(mon, "extra", "p0"));
    return w;
  }

  @Test
  public void sameDayBranchesRequireExplicitApproval() {
    State s = multiPlaceState();
    Week w = twoPlaces(s);
    assertFalse(validate(s, w).isEmpty());
    w.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "extra"));
    assertTrue(validate(s, w).isEmpty());
    assertEquals(w.simultaneousOverrides, copy(w).simultaneousOverrides);
    approve(s, w);
    StatRow row =
        statistics(s, mon, false).stream().filter(r -> r.person.id.equals("p0")).findFirst().get();
    assertEquals(1, row.counts[2]);
    assertEquals(1, row.counts[3]);
  }

  @Test
  public void sameDayApprovalCannotBeReusedForOtherDatesPeopleOrPlaces() {
    State s = multiPlaceState();
    s.places.add(new Place("third", "Üçüncü Şube", 31, 0));
    for (int variant = 0; variant < 3; variant++) {
      Week w = new Week(mon);
      LocalDate date = variant == 0 ? mon.plusDays(1) : mon;
      String person = variant == 1 ? "p1" : "p0", place = variant == 2 ? "third" : "extra";
      w.duties.add(new Duty(date, BRANCH, person));
      w.duties.add(new Duty(date, place, person));
      w.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "extra"));
      assertFalse(validate(s, w).isEmpty());
    }
  }

  @Test
  public void thirdBranchNeedsApprovalForEveryAdditionalPair() {
    State s = multiPlaceState();
    s.places.add(new Place("third", "Üçüncü Şube", 31, 0));
    Week w = twoPlaces(s);
    w.duties.add(new Duty(mon, "third", "p0"));
    w.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "extra"));
    assertFalse(validate(s, w).isEmpty());
    w.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "third"));
    assertFalse(validate(s, w).isEmpty());
    w.simultaneousOverrides.add(simultaneousKey(mon, "p0", "extra", "third"));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void sameDayApprovalCannotOverrideLeaveOrRecurringRestriction() {
    State s = multiPlaceState();
    Week w = twoPlaces(s);
    w.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "extra"));
    s.leaves.add(new Leave("p0", ALL, mon, mon, "Rapor"));
    assertFalse(validate(s, w).isEmpty());
    s.leaves.clear();
    s.leaves.add(new Leave("p0", "extra", mon, LocalDate.MAX, "Haftalık Kısıt", 1));
    assertFalse(validate(s, w).isEmpty());
  }

  @Test
  public void sameDayApprovalDoesNotWaiveThursdayFridayRest() {
    State s = multiPlaceState();
    Week w = new Week(mon);
    LocalDate thursday = mon.plusDays(3);
    w.duties.add(new Duty(thursday, BRANCH, "p0"));
    w.duties.add(new Duty(thursday, "extra", "p0"));
    w.duties.add(new Duty(mon.plusDays(5), WORK, "p0"));
    w.duties.add(new Duty(mon.plusDays(6), WORK, "p0"));
    w.simultaneousOverrides.add(simultaneousKey(thursday, "p0", BRANCH, "extra"));
    assertFalse(validate(s, w).isEmpty());
  }

  @Test
  public void sameDayApprovalDoesNotAllowDuplicateSlotOrWaiveWeeklyLimit() {
    State s = multiPlaceState();
    Week w = twoPlaces(s);
    w.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "extra"));
    w.duties.add(new Duty(mon.plusDays(1), BRANCH, "p0"));
    assertFalse(validate(s, w).isEmpty());
    w.limitOverrides.add(BRANCH + "|p0");
    assertTrue(validate(s, w).isEmpty());
    w.duties.add(new Duty(mon, BRANCH, "p0"));
    assertFalse(validate(s, w).isEmpty());
  }

  @Test
  public void automaticGenerationNeverCreatesSimultaneousAssignments() {
    State s = multiPlaceState();
    Week old = twoPlaces(s);
    old.simultaneousOverrides.add(simultaneousKey(mon, "p0", BRANCH, "extra"));
    approve(s, old);
    Week next = generate(s, mon.plusWeeks(1));
    Set<String> seen = new HashSet<>();
    for (Duty d : next.duties) assertTrue(seen.add(d.date + "|" + d.person));
    assertTrue(next.simultaneousOverrides.isEmpty());
    assertTrue(validate(s, next).isEmpty());
  }
}
