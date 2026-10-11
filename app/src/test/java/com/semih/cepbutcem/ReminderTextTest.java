package com.semih.cepbutcem;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public class ReminderTextTest {
  private final LocalDate today = LocalDate.of(2026, 10, 12);

  private Reminders.Due due(String name) {
    return new Reminders.Due(name, today, 100);
  }

  @Test
  public void onePayment() {
    assertEquals(
        "Bugün Elektrik son ödeme günü lütfen kontrol ediniz!",
        ReminderText.today(List.of(due("Elektrik")), today));
  }

  @Test
  public void twoPayments() {
    assertEquals(
        "Bugün Elektrik ve Su son ödeme günü lütfen kontrol ediniz!",
        ReminderText.today(List.of(due("Elektrik"), due("Su")), today));
  }

  @Test
  public void threePayments() {
    assertEquals(
        "Bugün Elektrik, Su ve İnternet son ödeme günü lütfen kontrol ediniz!",
        ReminderText.today(List.of(due("Elektrik"), due("Su"), due("İnternet")), today));
  }

  @Test
  public void excludesOtherDates() {
    assertNull(
        ReminderText.today(
            List.of(
                new Reminders.Due("Eski", today.minusDays(1), 100),
                new Reminders.Due("Yarın", today.plusDays(1), 100)),
            today));
  }

  @Test
  public void deduplicatesLabelsAndCleansWhitespace() {
    assertEquals(
        "Bugün Elektrik ve Bonus son ödeme günü lütfen kontrol ediniz!",
        ReminderText.today(
            List.of(
                due(" Elektrik\n"),
                due("Elektrik"),
                new Reminders.Due("Bonus · ekstre", "Bonus", today, 100)),
            today));
  }

  @Test
  public void largeDigestIsBounded() {
    List<Reminders.Due> all = new ArrayList<>();
    for (int i = 1; i <= 1000; i++) all.add(due("Ödeme " + i));
    String message = ReminderText.today(all, today);
    assertTrue(message.contains("ve diğer 990 ödeme"));
    assertTrue(message.length() < 300);
  }
}
