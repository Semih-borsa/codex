package com.semih.cepbutcem;

import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class InsightsTest {
  private Finance.Entry e(
      String kind, long amount, LocalDate date, long source, long target, long category) {
    return new Finance.Entry(1, kind, amount, date, source, target, category, "");
  }

  @Test
  public void forecastDoesNotProjectFixedPaymentsAgainOrFutureIncomeAsActual() {
    LocalDate today = LocalDate.of(2026, 10, 10);
    List<Finance.Entry> es = new ArrayList<>();
    es.add(e("I", 10000, today, 1, 0, 1));
    es.add(e("E", 5000, today, 1, 0, 2));
    es.add(e("E", 100, today.minusDays(1), 1, 0, 2));
    es.add(e("E", 100, today.minusDays(2), 1, 0, 2));
    es.add(e("I", 90000, today.plusDays(1), 1, 0, 1));
    Insights.Forecast f =
        Insights.forecast(
            es, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), today, 5000, 700, 3000);
    assertTrue(f.ready);
    assertEquals(200, f.variableExpense);
    assertEquals(6320, f.projectedExpense);
    assertEquals(6680, f.projectedNet);
    assertEquals(10000, f.income);
  }

  @Test
  public void sparseDaysDoNotProduceStatisticalProjection() {
    LocalDate day = LocalDate.of(2026, 10, 10);
    Insights.Forecast f =
        Insights.forecast(
            List.of(e("E", 100, day, 1, 0, 2)),
            day.withDayOfMonth(1),
            day.withDayOfMonth(31),
            day,
            0,
            50,
            0);
    assertFalse(f.ready);
    assertEquals(150, f.projectedExpense);
  }

  @Test
  public void salaryPeriodClipAndPastPeriodHaveNoRemainingProjection() {
    LocalDate a = Finance.cycleStart(YearMonth.of(2024, 2), 31),
        b = Finance.cycleEnd(YearMonth.of(2024, 2), 31);
    Insights.Forecast f =
        Insights.forecast(
            List.of(e("E", 500, a, 1, 0, 2)), a, b, LocalDate.of(2024, 5, 1), 0, 0, 0);
    assertEquals(LocalDate.of(2024, 2, 29), a);
    assertEquals(31, f.days);
    assertEquals(0, f.remainingDays);
    assertEquals(500, f.spent);
  }

  @Test
  public void anomalyRequiresThreeRecordedComparablePeriods() {
    List<Finance.Entry> es = new ArrayList<>();
    for (int n = 1; n <= 3; n++) {
      es.add(e("E", 100, LocalDate.of(2026, 10, 5).minusMonths(n), 1, 0, 2));
      es.add(e("E", 9900, LocalDate.of(2026, 10, 20).minusMonths(n), 1, 0, 2));
    }
    es.add(e("E", 250, LocalDate.of(2026, 10, 5), 1, 0, 2));
    List<Insights.Anomaly> a =
        Insights.anomalies(es, YearMonth.of(2026, 10), 1, LocalDate.of(2026, 10, 10));
    assertEquals(1, a.size());
    assertEquals(100, a.get(0).average);
    assertEquals(3, a.get(0).periods);
    es.remove(0);
    assertTrue(
        Insights.anomalies(es, YearMonth.of(2026, 10), 1, LocalDate.of(2026, 10, 10)).isEmpty());
  }

  @Test
  public void goalPredictionUsesNetTransfersAndRequiresCompleteHistory() {
    List<Finance.Entry> es = new ArrayList<>();
    for (int n = 1; n <= 3; n++) {
      LocalDate day = LocalDate.of(2026, 10, 5).minusMonths(n);
      es.add(e("T", 300, day, 1, 2, 0));
      es.add(e("T", 100, day, 2, 1, 0));
    }
    assertEquals(3, Insights.goalMonths(es, 2, 500, YearMonth.of(2026, 10), 1));
    es.remove(0);
    assertEquals(-1, Insights.goalMonths(es, 2, 500, YearMonth.of(2026, 10), 1));
    assertEquals(0, Insights.goalMonths(es, 2, 0, YearMonth.of(2026, 10), 1));
  }

  @Test
  public void shoppingInstallmentsKeepOriginalDayAcrossShortMonths() {
    Enhancements.Purchase p = new Enhancements.Purchase();
    p.first = LocalDate.of(2024, 1, 31);
    p.amount = 100;
    p.count = 3;
    p.paid = 1;
    assertEquals(LocalDate.of(2024, 2, 29), p.nextDue());
    assertEquals(LocalDate.of(2024, 3, 31), p.due(2));
    assertEquals(200, p.remaining());
  }
}
