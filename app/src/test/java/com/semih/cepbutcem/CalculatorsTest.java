package com.semih.cepbutcem;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import org.junit.Test;

public class CalculatorsTest {
  private BigDecimal[] rates(String... values) {
    return java.util.Arrays.stream(values)
        .map(Calculators::salaryPercent)
        .toArray(BigDecimal[]::new);
  }

  @Test
  public void salaryMatchesAppJsxCompoundInflationFormula() {
    Calculators.Salary s =
        Calculators.salary(
            3000000,
            new BigDecimal("10"),
            new BigDecimal("5"),
            rates("3", "2", "1", "4", "2", "1"));
    assertEquals(3255604, s.estimated);
    assertEquals(0, new BigDecimal("13.687745724800").compareTo(s.cumulative));
    assertTrue(s.difference.signum() > 0);
  }

  @Test
  public void currentRaiseNeverAppliedTwiceAndNegativeDifferenceClamped() {
    Calculators.Salary s =
        Calculators.salary(
            3000000, new BigDecimal("15"), new BigDecimal("10"), rates("", "", "", "", "", ""));
    assertEquals(3300000, s.estimated);
    assertEquals(0, s.difference.signum());
  }

  @Test
  public void compoundedMonthsAreNotAdded() {
    Calculators.Salary s =
        Calculators.salary(
            100000, BigDecimal.ZERO, BigDecimal.ZERO, rates("10", "10", "0", "0", "0", "0"));
    assertEquals(121000, s.estimated);
    assertEquals(0, new BigDecimal("21").compareTo(s.cumulative));
    assertEquals(0, new BigDecimal("10").compareTo(s.monthCumulative[0]));
  }

  @Test
  public void deflationAndEmptyMonthsAccepted() {
    assertEquals(
        110000,
        Calculators.salary(
                100000, BigDecimal.ZERO, new BigDecimal("10"), rates("-2,5", "", "", "", "", ""))
            .estimated);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidDenominatorRejected() {
    Calculators.salary(
        100000, new BigDecimal("-100"), BigDecimal.ZERO, rates("", "", "", "", "", ""));
  }

  @Test
  public void zeroInterestAndKnownAnnuity() {
    assertEquals(100000, Calculators.creditInstallment(1200000, BigDecimal.ZERO, 12));
    assertEquals(88849, Calculators.creditInstallment(1000000, new BigDecimal("1"), 12));
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidCreditPeriodRejected() {
    Calculators.creditInstallment(100, BigDecimal.ONE, 0);
  }

  @Test
  public void maximumDurationAndRateStayFinite() {
    assertEquals(500000, Calculators.creditInstallment(1000000, new BigDecimal("50"), 360));
  }

  @Test
  public void decimalAcceptsTurkishSeparators() {
    assertEquals(new BigDecimal("3.5"), Calculators.decimal("3,5"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void decimalRejectsAmbiguousGroups() {
    Calculators.decimal("3.500,5");
  }
}
