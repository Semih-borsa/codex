package com.semih.cepbutcem;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Explainable, offline estimates over bounded SQL aggregates, never invented transactions. */
public final class Insights {
  private Insights() {}

  public static final class Forecast {
    public long income,
        spent,
        fixedExpense,
        variableExpense,
        plannedExpense,
        plannedIncome,
        projectedExpense,
        projectedNet,
        liquid,
        reservedCards,
        dailyAvailable;
    public int days, remainingDays, spendingDays;
    public boolean ready;
  }

  public static Forecast forecast(
      List<Finance.Entry> entries,
      LocalDate start,
      LocalDate end,
      LocalDate today,
      long fixed,
      long upcoming,
      long upcomingIncome) {
    Forecast f = new Forecast();
    LocalDate actualEnd = today.isBefore(end) ? today : end;
    f.days = (int) Math.max(0, ChronoUnit.DAYS.between(start, actualEnd) + 1);
    int total = (int) ChronoUnit.DAYS.between(start, end) + 1;
    f.remainingDays = Math.max(0, total - f.days);
    List<Finance.Entry> actual = Finance.period(entries, start, actualEnd);
    f.income = Finance.total(actual, "I");
    f.spent = Finance.total(actual, "E");
    f.fixedExpense = Math.min(f.spent, Math.max(0, fixed));
    f.variableExpense = f.spent - f.fixedExpense;
    f.plannedExpense = upcoming;
    f.plannedIncome = upcomingIncome;
    Set<LocalDate> days = new HashSet<>();
    for (Finance.Entry e : actual) if (e.kind.equals("E")) days.add(e.date);
    f.spendingDays = days.size();
    f.ready = f.days >= 7 && f.spendingDays >= 3;
    f.projectedExpense =
        Math.addExact(
            Math.addExact(f.spent, upcoming),
            f.ready ? Math.round((double) f.variableExpense / f.days * f.remainingDays) : 0);
    f.projectedNet = Math.addExact(f.income, upcomingIncome) - f.projectedExpense;
    return f;
  }

  public static final class Anomaly {
    public long category, current, average;
    public int periods;
    public double ratio;
  }

  public static List<Anomaly> anomalies(
      List<Finance.Entry> entries, YearMonth selected, int payday, LocalDate today) {
    LocalDate start = Finance.cycleStart(selected, payday),
        end = Finance.cycleEnd(selected, payday);
    if (start.isAfter(today)) return Collections.emptyList();
    LocalDate actualEnd = end.isAfter(today) ? today : end;
    int elapsed = (int) ChronoUnit.DAYS.between(start, actualEnd) + 1;
    boolean partial = actualEnd.isBefore(end);
    Map<Long, Long> current = Finance.byCategory(Finance.period(entries, start, actualEnd), "E");
    List<Anomaly> result = new ArrayList<>();
    for (Map.Entry<Long, Long> item : current.entrySet()) {
      long total = 0;
      int count = 0;
      for (int n = 1; n <= 6; n++) {
        YearMonth m = selected.minusMonths(n);
        LocalDate a = Finance.cycleStart(m, payday), b = Finance.cycleEnd(m, payday);
        if (!b.isBefore(today)) continue;
        if (partial) b = a.plusDays(Math.min(elapsed, (int) ChronoUnit.DAYS.between(a, b) + 1) - 1);
        long amount =
            Finance.byCategory(Finance.period(entries, a, b), "E").getOrDefault(item.getKey(), 0L);
        if (amount > 0) {
          total = Math.addExact(total, amount);
          count++;
        }
      }
      if (count < 3) continue;
      long avg = total / count;
      if (item.getValue() < 2d * avg) continue;
      Anomaly a = new Anomaly();
      a.category = item.getKey();
      a.current = item.getValue();
      a.average = avg;
      a.periods = count;
      a.ratio = (double) a.current / avg;
      result.add(a);
    }
    result.sort((a, b) -> Double.compare(b.ratio, a.ratio));
    return result;
  }

  /** Three complete salary cycles with positive net contributions; unknown if history is sparse. */
  public static long goalMonths(
      List<Finance.Entry> entries, long account, long remaining, YearMonth current, int payday) {
    if (remaining <= 0) return 0;
    long total = 0;
    for (int n = 1; n <= 3; n++) {
      long net = 0;
      boolean seen = false;
      for (Finance.Entry e :
          Finance.period(
              entries,
              Finance.cycleStart(current.minusMonths(n), payday),
              Finance.cycleEnd(current.minusMonths(n), payday)))
        if (e.kind.equals("T")) {
          if (e.targetId == account) {
            net = Math.addExact(net, e.amount);
            seen = true;
          }
          if (e.accountId == account) {
            net = Math.subtractExact(net, e.amount);
            seen = true;
          }
        }
      if (!seen || net <= 0) return -1;
      total = Math.addExact(total, net);
    }
    return (long) Math.ceil((double) remaining / (total / 3d));
  }
}
