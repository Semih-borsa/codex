package com.semih.cepbutcem;

import static org.junit.Assert.*;

import java.time.LocalDate;
import org.junit.Test;

public class PlanningTest {
  private Planning.Card card(long account, long remaining, long paid, long current, long pending) {
    Planning.Card c = new Planning.Card(account, 10, 16);
    c.updated = LocalDate.of(2026, 10, 10);
    c.statement = remaining;
    c.statementPaid = paid;
    c.current = current;
    c.pending = pending;
    return c;
  }

  @Test
  public void partialAndFullPaymentTotalsExcludeUnknownSnapshots() {
    Planning p = new Planning();
    p.cards.add(card(1, 0, 1_000_000, 10000, 5000));
    p.cards.add(card(2, 1_000_000, 0, 20000, 9000));
    p.cards.add(card(3, 1_000_000, 0, 30000, 7000));
    p.cards.add(new Planning.Card(4, 10, 20));
    Planning.CardTotals totals = p.cardTotals();
    assertEquals(2_000_000, totals.remaining);
    assertEquals(1_000_000, totals.paid);
    assertEquals(81000, totals.current);
    assertEquals(3, totals.known);
    assertEquals(1, totals.unknown);
    for (Planning.Card c : p.cards) {
      c.statementPaid += c.statement;
      c.statement = 0;
    }
    totals = p.cardTotals();
    assertEquals(0, totals.remaining);
    assertEquals(3_000_000, totals.paid);
    assertEquals(81000, totals.current);
  }

  @Test
  public void upcomingIncludesOnlyUnpaidTodayThroughSevenDaysAndCards() {
    Planning p = new Planning();
    LocalDate today = LocalDate.of(2026, 10, 11);
    for (int delta : new int[] {-1, 0, 5, 7, 8}) {
      Planning.Bill b = new Planning.Bill();
      b.due = today.plusDays(delta);
      b.amount = 100;
      p.bills.add(b);
    }
    Planning.Bill paid = new Planning.Bill();
    paid.due = today;
    paid.paid = 1;
    p.bills.add(paid);
    Planning.Bill tracked = new Planning.Bill();
    tracked.due = today;
    tracked.trackedPaid = today;
    p.bills.add(tracked);
    p.cards.add(card(1, 100, 0, 0, 0));
    p.cards.add(card(2, 0, 100, 0, 0));
    p.cards.add(new Planning.Card(3, 10, 16));
    assertEquals(4, p.upcoming(today).size());
    assertEquals(today, p.upcoming(today).get(0).date);
    assertEquals(today.plusDays(7), p.upcoming(today).get(3).date);
    assertEquals(1, p.upcoming(today).stream().filter(x -> x.card != null).count());
  }

  @Test
  public void closingAndDueUseSnapshotCycleAndShortMonth() {
    Planning.Card c = new Planning.Card(1, 31, 5);
    c.updated = LocalDate.of(2026, 3, 1);
    assertEquals(LocalDate.of(2026, 2, 28), c.statementClosing());
    assertEquals(LocalDate.of(2026, 3, 5), c.statementDue());
  }
}
