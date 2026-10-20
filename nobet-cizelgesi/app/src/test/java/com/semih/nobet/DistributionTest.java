package com.semih.nobet;

import static com.semih.nobet.Roster.*;
import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

/** Weekly fairness regressions plus an independent exhaustive assignment oracle. */
public class DistributionTest {
  final LocalDate mon = LocalDate.of(2026, 10, 19);

  State people(int n) {
    State s = new State();
    for (int i = 0; i < n; i++) s.people.add(new Person("p" + i, "Personel " + i));
    return s;
  }

  List<Long> counts(Week w, String place, int n) {
    List<Long> result = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      String id = "p" + i;
      result.add(
          w.duties.stream()
              .filter(d -> d.place.equals(place) && d.person.equals(id) && !weekend(d.date))
              .count());
    }
    Collections.sort(result);
    return result;
  }

  void masks(State s, String place, int... masks) {
    for (int p = 0; p < masks.length; p++)
      for (int d = 0; d < 5; d++)
        if ((masks[p] & (1 << d)) == 0)
          s.leaves.add(new Leave("p" + p, place, mon.plusDays(d), mon.plusDays(d), "Kısıt"));
  }

  @Test
  public void fourPeopleFiveDaysAlwaysOneEachAndOneRepeat() {
    State s = people(4);
    s.place(BRANCH).limit = 0;
    Week w = new Week(mon);
    assignPlace(s, w, s.place(BRANCH), s.people);
    assertEquals(Arrays.asList(1L, 1L, 1L, 2L), counts(w, BRANCH, 4));
  }

  @Test
  public void historicalImbalanceCannotGiveOnePersonThreeThisWeek() {
    State s = people(4);
    s.place(BRANCH).limit = 0;
    for (int n = 1; n <= 12; n++) {
      Week old = new Week(mon.minusWeeks(n));
      for (int p = 0; p < 3; p++)
        for (int d = 0; d < 5; d++)
          old.duties.add(new Duty(old.monday.plusDays(d), BRANCH, "p" + p));
      s.weeks.add(old);
    }
    Week w = new Week(mon);
    assignPlace(s, w, s.place(BRANCH), s.people);
    assertEquals(Arrays.asList(1L, 1L, 1L, 2L), counts(w, BRANCH, 4));
    assertEquals(2, w.duties.stream().filter(d -> d.person.equals("p3")).count());
  }

  @Test
  public void personWithOnlyMondayGetsMondayBeforeFlexiblePeople() {
    State s = people(4);
    s.place(BRANCH).limit = 0;
    masks(s, BRANCH, 1, 31, 31, 31);
    Week w = new Week(mon);
    assignPlace(s, w, s.place(BRANCH), s.people);
    assertTrue(w.duties.stream().anyMatch(d -> d.person.equals("p0") && d.date.equals(mon)));
    assertEquals(Arrays.asList(1L, 1L, 1L, 2L), counts(w, BRANCH, 4));
  }

  @Test
  public void overlappingRestrictionsRearrangeEarlierChoices() {
    State s = people(4);
    s.place(BRANCH).limit = 0;
    masks(s, BRANCH, 3, 1, 12, 24);
    Week w = new Week(mon);
    assignPlace(s, w, s.place(BRANCH), s.people);
    assertEquals(5, w.duties.size());
    assertEquals(Arrays.asList(1L, 1L, 1L, 2L), counts(w, BRANCH, 4));
    for (Duty d : w.duties) assertTrue(available(s, d.person, d.place, d.date));
  }

  @Test
  public void hardWeeklyLimitLeavesOneSlotWithWarningInsteadOfSilentOverride() {
    State s = people(4);
    Week w = new Week(mon);
    assignPlace(s, w, s.place(BRANCH), s.people);
    assertEquals(4, w.duties.size());
    assertEquals(1, w.warnings.size());
    assertEquals(Arrays.asList(1L, 1L, 1L, 1L), counts(w, BRANCH, 4));
  }

  @Test
  public void leaveAndInactivePeopleNeverUsedAutomatically() {
    State s = people(5);
    s.place(BRANCH).limit = 0;
    s.people.get(4).active = false;
    s.leaves.add(new Leave("p0", ALL, mon, mon.plusDays(6), "Rapor"));
    Week w = generate(s, mon);
    assertFalse(w.duties.stream().anyMatch(d -> d.person.equals("p0") || d.person.equals("p4")));
    assertTrue(validate(s, w).isEmpty());
  }

  @Test
  public void workWeekCountsExcludeReservedWeekendButHonorRestDays() {
    State s = people(4);
    s.place(BRANCH).days = 0;
    Week w = generate(s, mon);
    assertEquals(Arrays.asList(1L, 1L, 1L, 2L), counts(w, WORK, 4));
    assertTrue(validate(s, w).isEmpty());
    assertFalse(
        w.duties.stream()
            .anyMatch(
                d ->
                    d.person.equals("p0")
                        && (d.date.equals(mon.plusDays(3)) || d.date.equals(mon.plusDays(4)))));
  }

  @Test
  public void restrictiveWorkDayNotConsumedByFlexibleBranchAssignment() {
    State s = people(5);
    masks(s, WORK, 1, 31, 31, 31, 31);
    Week w = generate(s, mon);
    assertTrue(
        w.duties.stream()
            .anyMatch(d -> d.place.equals(WORK) && d.person.equals("p0") && d.date.equals(mon)));
  }

  @Test
  public void disabledOrDeletedWorkPlaceNotAssignedOnWeekdays() {
    State s = people(4);
    s.place(WORK).automatic = false;
    s.place(WORK).days = 31;
    assertFalse(generate(s, mon).duties.stream().anyMatch(d -> d.place.equals(WORK)));
  }

  @Test
  public void randomAvailabilityMatchesExhaustiveMaximumCoverageAndWeeklyBalance() {
    Random random = new Random(435212);
    for (int run = 0; run < 180; run++) {
      State s = people(4);
      s.place(WORK).days = 0;
      Place place = s.place(BRANCH);
      place.limit = random.nextInt(3);
      int[] masks = new int[4];
      for (int p = 0; p < 4; p++) masks[p] = random.nextInt(32);
      masks(s, BRANCH, masks);
      int[] best = {-1, -1, Integer.MAX_VALUE};
      enumerate(masks, place.limit, 0, new int[4], 0, best);
      Week w = new Week(mon);
      assignPlace(s, w, place, s.people);
      List<Long> counts = counts(w, BRANCH, 4);
      long used = counts.stream().filter(v -> v > 0).count(),
          squares = counts.stream().mapToLong(v -> v * v).sum();
      assertEquals("filled run " + run, best[0], w.duties.size());
      assertEquals("coverage run " + run, best[1], used);
      assertEquals("balance run " + run, best[2], squares);
    }
  }

  void enumerate(int[] masks, int limit, int day, int[] counts, int filled, int[] best) {
    if (day == 5) {
      int used = 0, squares = 0;
      for (int n : counts) {
        if (n > 0) used++;
        squares += n * n;
      }
      if (filled > best[0]
          || (filled == best[0] && (used > best[1] || (used == best[1] && squares < best[2])))) {
        best[0] = filled;
        best[1] = used;
        best[2] = squares;
      }
      return;
    }
    enumerate(masks, limit, day + 1, counts, filled, best);
    for (int p = 0; p < masks.length; p++)
      if ((masks[p] & (1 << day)) != 0 && (limit == 0 || counts[p] < limit)) {
        counts[p]++;
        enumerate(masks, limit, day + 1, counts, filled + 1, best);
        counts[p]--;
      }
  }
}
