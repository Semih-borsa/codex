package com.semih.cepbutcem;

import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class FinanceTest {
  private Finance.Entry entry(
      String kind, long amount, String date, long source, long target, long category) {
    return new Finance.Entry(1, kind, amount, LocalDate.parse(date), source, target, category, "");
  }

  @Test
  public void moneyParsingIsExactAndLocaleAware() {
    assertEquals(123456, Finance.parseMoney("1.234,56"));
    assertEquals(123456, Finance.parseMoney("1234.56"));
    assertEquals(-105, Finance.parseMoney("-1,05"));
    assertEquals(10, Finance.parseMoney("0,10"));
    for (String s : Arrays.asList("NaN", "1,234", "1.2.3", "999999999999999999999999", "")) {
      assertThrows(IllegalArgumentException.class, () -> Finance.parseMoney(s));
    }
  }

  @Test
  public void transfersDoNotInflateCashFlowAndPreserveNetWorth() {
    List<Finance.Entry> entries =
        Arrays.asList(
            entry("I", 100000, "2026-10-01", 1, 0, 1),
            entry("E", 20000, "2026-10-02", 1, 0, 2),
            entry("T", 30000, "2026-10-03", 1, 2, 0));
    assertEquals(100000, Finance.total(entries, "I"));
    assertEquals(20000, Finance.total(entries, "E"));
    long a =
        Finance.balance(
            new Finance.Account(1, "Banka", 5000), entries, LocalDate.parse("2026-10-10"));
    long b =
        Finance.balance(new Finance.Account(2, "Nakit", 0), entries, LocalDate.parse("2026-10-10"));
    assertEquals(55000, a);
    assertEquals(30000, b);
    assertEquals(85000, a + b);
  }

  @Test
  public void futureEntriesDoNotAffectTodaysBalance() {
    assertEquals(
        0,
        Finance.balance(
            new Finance.Account(1, "A", 0),
            Collections.singletonList(entry("I", 500, "2026-11-01", 1, 0, 1)),
            LocalDate.parse("2026-10-10")));
  }

  @Test
  public void periodIncludesBoundariesAndExcludesAdjacentMonths() {
    List<Finance.Entry> entries =
        Arrays.asList(
            entry("E", 10, "2026-09-30", 1, 0, 1),
            entry("E", 20, "2026-10-01", 1, 0, 1),
            entry("E", 30, "2026-10-31", 1, 0, 2),
            entry("E", 40, "2026-11-01", 1, 0, 1));
    List<Finance.Entry> current =
        Finance.period(entries, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
    assertEquals(2, current.size());
    assertEquals(50, Finance.total(current, "E"));
    assertEquals(Arrays.asList(2L, 1L), new ArrayList<>(Finance.byCategory(current, "E").keySet()));
  }

  @Test
  public void forecastsRespectMonthLengthAndFuturePeriods() {
    assertEquals(
        31000, Finance.forecast(10000, YearMonth.of(2026, 10), LocalDate.of(2026, 10, 10)));
    assertEquals(2000, Finance.forecast(2000, YearMonth.of(2026, 9), LocalDate.of(2026, 10, 10)));
    assertEquals(0, Finance.forecast(2000, YearMonth.of(2026, 11), LocalDate.of(2026, 10, 10)));
    assertEquals(2900, Finance.forecast(100, YearMonth.of(2024, 2), LocalDate.of(2024, 2, 1)));
  }

  @Test
  public void recurringRetainsPreferredDayAcrossShortMonths() {
    LocalDate feb = Finance.nextMonthly(LocalDate.of(2024, 1, 31), 31);
    assertEquals(LocalDate.of(2024, 2, 29), feb);
    assertEquals(LocalDate.of(2024, 3, 31), Finance.nextMonthly(feb, 31));
  }

  @Test
  public void percentageWithoutBaselineIsUndefined() {
    assertTrue(Double.isNaN(Finance.change(500, 0)));
    assertTrue(Double.isNaN(Finance.savingRate(0, 100)));
    assertEquals(-20, Finance.savingRate(100, 120), 0.0001);
    assertEquals(50, Finance.change(150, 100), 0.0001);
  }

  @Test
  public void monthlySeriesContainsEmptyMonths() {
    List<Finance.Entry> entries =
        Collections.singletonList(entry("E", 2500, "2026-09-12", 1, 0, 1));
    assertEquals(
        Arrays.asList(0L, 2500L, 0L), Finance.monthly(entries, YearMonth.of(2026, 10), 3, "E"));
  }

  @Test
  public void invalidTransferAndNonpositiveAmountsAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Finance.validate(entry("T", 10, "2026-10-01", 1, 1, 0)));
    assertThrows(
        IllegalArgumentException.class,
        () -> Finance.validate(entry("E", 0, "2026-10-01", 1, 0, 1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> Finance.validate(entry("E", 10, "2026-10-01", 1, 0, 0)));
  }

  @Test
  public void csvEscapesQuotesAndMultilineNotes() {
    assertEquals("\"Bir \"\"not\"\"\nson\"", Finance.csvCell("Bir \"not\"\nson"));
  }
}
