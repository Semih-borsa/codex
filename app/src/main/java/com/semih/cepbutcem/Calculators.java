package com.semih.cepbutcem;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estimates are separate from ledger writes; round once at the displayed monetary boundary. */
public final class Calculators {
  private Calculators() {}

  private static long money(BigDecimal value) {
    return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
  }

  public static final class Salary {
    public final long estimated;
    public final BigDecimal cumulative, difference, increase;
    public final BigDecimal[] monthCumulative;

    Salary(
        long estimated,
        BigDecimal cumulative,
        BigDecimal difference,
        BigDecimal increase,
        BigDecimal[] monthCumulative) {
      this.estimated = estimated;
      this.cumulative = cumulative;
      this.difference = difference;
      this.increase = increase;
      this.monthCumulative = monthCumulative;
    }
  }

  /** App.jsx MaasTab: compound six months, offset current raise, apply next raise once. */
  public static Salary salary(
      long current, BigDecimal currentRaise, BigDecimal nextRaise, BigDecimal[] inflation) {
    if (current <= 0 || current > 100_000_000_000L || inflation.length != 6)
      throw new IllegalArgumentException("Pozitif maaş ve altı aylık enflasyon gerekli.");
    validatePercent(currentRaise);
    validatePercent(nextRaise);
    java.math.MathContext mc = java.math.MathContext.DECIMAL128;
    BigDecimal compound = BigDecimal.ONE;
    BigDecimal[] months = new BigDecimal[6];
    for (int i = 0; i < 6; i++) {
      validatePercent(inflation[i]);
      compound = compound.multiply(BigDecimal.ONE.add(inflation[i].movePointLeft(2)), mc);
      months[i] = compound.subtract(BigDecimal.ONE).movePointRight(2);
    }
    BigDecimal factor =
        compound
            .divide(BigDecimal.ONE.add(currentRaise.movePointLeft(2)), mc)
            .subtract(BigDecimal.ONE)
            .max(BigDecimal.ZERO);
    BigDecimal estimated =
        BigDecimal.valueOf(current)
            .multiply(BigDecimal.ONE.add(factor), mc)
            .multiply(BigDecimal.ONE.add(nextRaise.movePointLeft(2)), mc);
    return new Salary(
        money(estimated),
        months[5],
        factor.movePointRight(2),
        estimated
            .divide(BigDecimal.valueOf(current), mc)
            .subtract(BigDecimal.ONE)
            .movePointRight(2),
        months);
  }

  public static void validatePercent(BigDecimal percent) {
    if (percent == null
        || percent.compareTo(new BigDecimal("-100")) <= 0
        || percent.compareTo(new BigDecimal("1000")) > 0)
      throw new IllegalArgumentException("Oran %-100'den büyük, %1000 veya altında olmalı.");
  }

  public static BigDecimal salaryPercent(String value) {
    if (value.trim().isEmpty()) return BigDecimal.ZERO;
    if (!value.trim().matches("-?[0-9]+([.,][0-9]{1,4})?"))
      throw new IllegalArgumentException("Geçerli yüzde gir (en çok 4 ondalık).");
    BigDecimal result = new BigDecimal(value.trim().replace(',', '.'));
    validatePercent(result);
    return result;
  }

  public static long creditInstallment(long principal, BigDecimal monthlyPercent, int count) {
    if (principal <= 0
        || principal > 100_000_000_000L
        || count < 1
        || count > 360
        || monthlyPercent.signum() < 0
        || monthlyPercent.compareTo(BigDecimal.valueOf(50)) > 0)
      throw new IllegalArgumentException("Anapara pozitif, vade 1–360 ve aylık faiz %0–50 olmalı.");
    if (monthlyPercent.signum() == 0)
      return money(
          BigDecimal.valueOf(principal)
              .divide(BigDecimal.valueOf(count), 12, RoundingMode.HALF_UP));
    java.math.MathContext mc = new java.math.MathContext(32, RoundingMode.HALF_UP);
    BigDecimal r = monthlyPercent.movePointLeft(2), power = BigDecimal.ONE.add(r).pow(count, mc);
    return money(
        BigDecimal.valueOf(principal)
            .multiply(r, mc)
            .multiply(power, mc)
            .divide(power.subtract(BigDecimal.ONE), mc));
  }

  public static BigDecimal decimal(String input) {
    if (!input.trim().matches("[0-9]+([.,][0-9]{1,4})?"))
      throw new IllegalArgumentException("Geçerli bir sayı gir (en çok 4 ondalık).");
    return new BigDecimal(input.trim().replace(',', '.'));
  }
}
