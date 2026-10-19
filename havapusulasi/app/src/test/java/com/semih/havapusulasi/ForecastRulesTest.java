package com.semih.havapusulasi;

import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class ForecastRulesTest {
  private final long now = Instant.parse("2026-10-18T10:20:00Z").toEpochMilli();

  private Forecast sample(long downloaded) {
    List<Forecast.Hour> hours = new ArrayList<>();
    for (int i = 0; i < 30; i++)
      hours.add(
          new Forecast.Hour(
              Instant.parse("2026-10-18T09:00:00Z").toEpochMilli() + i * 3600_000L,
              20.0,
              null,
              70.0,
              10.0,
              50.0,
              61));
    return new Forecast("Europe/Istanbul", downloaded, hours, Collections.emptyList());
  }

  @Test
  public void includesCurrentHourlyBucket() {
    assertEquals(
        Instant.parse("2026-10-18T10:00:00Z").toEpochMilli(),
        sample(now).upcoming(now, 24).get(0).at);
  }

  @Test
  public void boundsHourlyStrip() {
    assertEquals(24, sample(now).upcoming(now, 24).size());
  }

  @Test
  public void expiredForecastNeverReturnsOldCurrentValue() {
    assertTrue(sample(now).upcoming(now + 10 * 86400_000L, 1).isEmpty());
    assertTrue(sample(now).status(now + 10 * 86400_000L).contains("süresi doldu"));
  }

  @Test
  public void savedForecastNeverClaimsLiveObservation() {
    assertTrue(sample(now).status(now).contains("Kaydedilmiş tahmin"));
  }

  @Test
  public void sixHourAgeIsMarkedOld() {
    assertTrue(sample(now - 7 * 3600_000L).stale(now));
    assertTrue(sample(now - 7 * 3600_000L).status(now).contains("Eski kayıt"));
  }

  @Test
  public void clockRollbackIsVisible() {
    assertTrue(sample(now + 10000).status(now).contains("saati değişmiş"));
  }

  @Test
  public void nullTemperatureIsNeverZero() {
    assertEquals("—", Forecast.value(null, "°C"));
  }

  @Test
  public void unknownCodeNeverBecomesSunny() {
    assertEquals("Veri yok", Forecast.description(null));
    assertEquals("Kod 9", Forecast.description(9));
  }

  @Test
  public void rainAdviceUsesProbability() {
    assertTrue(Forecast.advice(sample(now).hours.get(0)).contains("Şemsiye"));
  }

  @Test
  public void emptyForecastGetsNoFabricatedAdvice() {
    assertTrue(Forecast.advice(null).contains("yok"));
  }

  @Test
  public void noGpsGuessForInvalidCity() {
    try {
      new City(0, "X", "", "", 100, 0, "UTC");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void dailyClockUsesDeviceZone() {
    long at =
        ReminderClock.next(
            Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Europe/Istanbul"), 8, 0);
    assertEquals(Instant.parse("2026-10-18T05:00:00Z").toEpochMilli(), at);
  }

  @Test
  public void elapsedDailyTimeMovesToTomorrow() {
    assertEquals(
        Instant.parse("2026-10-19T05:00:00Z").toEpochMilli(),
        ReminderClock.next(
            Instant.parse("2026-10-18T05:00:00Z"), ZoneId.of("Europe/Istanbul"), 8, 0));
  }

  @Test
  public void midnightIsValid() {
    assertEquals(
        Instant.parse("2026-10-19T00:00:00Z").toEpochMilli(),
        ReminderClock.next(Instant.parse("2026-10-18T22:00:00Z"), ZoneId.of("UTC"), 0, 0));
  }

  @Test
  public void springDstGapMovesToFirstValidLocalTime() {
    long at =
        ReminderClock.next(
            Instant.parse("2027-03-27T23:00:00Z"), ZoneId.of("Europe/Berlin"), 2, 30);
    assertEquals(Instant.parse("2027-03-28T01:30:00Z").toEpochMilli(), at);
  }

  @Test
  public void fallDstDoesNotScheduleSecondDuplicateTime() {
    long at =
        ReminderClock.next(
            Instant.parse("2027-10-31T01:00:00Z"), ZoneId.of("Europe/Berlin"), 2, 30);
    assertEquals(Instant.parse("2027-11-01T01:30:00Z").toEpochMilli(), at);
  }

  @Test
  public void invalidHourRejected() {
    try {
      ReminderClock.next(Instant.now(), ZoneId.of("UTC"), 24, 0);
      fail();
    } catch (DateTimeException expected) {
    }
  }

  @Test
  public void futureHourIsNotCurrentWeather() {
    Forecast f =
        new Forecast(
            "UTC",
            now,
            Collections.singletonList(
                new Forecast.Hour(now + 3600_000L, 20.0, null, null, null, null, 0)),
            Collections.emptyList());
    assertNull(f.current(now));
    assertEquals(1, f.upcoming(now, 24).size());
  }

  @Test
  public void currentHourUsesInstantAtDstOverlap() {
    long at = Instant.parse("2027-10-31T01:20:00Z").toEpochMilli();
    Forecast.Hour previous = new Forecast.Hour(at - 80 * 60_000, 5.0, null, null, null, null, 0);
    Forecast.Hour actual = new Forecast.Hour(at - 20 * 60_000, 7.0, null, null, null, null, 0);
    Forecast f =
        new Forecast("Europe/Berlin", at, Arrays.asList(previous, actual), Collections.emptyList());
    assertEquals(7.0, f.current(at).temperature, 0);
  }

  @Test
  public void lastRefreshIsExactEvenInSameMinute() {
    long at = Instant.parse("2026-10-19T08:47:20Z").toEpochMilli();
    assertEquals(
        "Son yenileme: 11:47", sample(at).lastUpdated(at + 5000, ZoneId.of("Europe/Istanbul")));
    assertEquals(
        "Son yenileme: 11:47",
        sample(at).lastUpdated(at + 2 * 3600_000L, ZoneId.of("Europe/Istanbul")));
  }

  @Test
  public void previousDayIncludesDateAcrossMidnight() {
    long at = Instant.parse("2026-10-18T20:59:00Z").toEpochMilli();
    assertEquals(
        "Son yenileme: 18 Eki 23:59",
        sample(at).lastUpdated(at + 2 * 60_000L, ZoneId.of("Europe/Istanbul")));
  }

  @Test
  public void refreshClockUsesPhoneZoneRatherThanForecastZone() {
    long at = Instant.parse("2026-10-19T08:47:00Z").toEpochMilli();
    assertEquals("Son yenileme: 08:47", sample(at).lastUpdated(at, ZoneId.of("UTC")));
    assertEquals("Son yenileme: 11:47", sample(at).lastUpdated(at, ZoneId.of("Europe/Istanbul")));
  }
}
