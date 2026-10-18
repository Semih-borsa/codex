package com.semih.nova;

import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class AssistantToolsTest {
  private final ZonedDateTime now =
      ZonedDateTime.of(2026, 10, 18, 10, 0, 0, 0, ZoneId.of("Europe/Istanbul"));

  @Test
  public void relativeSpokenMinutes() {
    ReminderDraft d = ReminderDraft.parse("On beş dakika sonra bana su içmeyi sesli hatırlat", now);
    assertEquals(now.plusMinutes(15), d.due);
    assertEquals("su içmeyi", d.title);
    assertTrue(d.audible);
  }

  @Test
  public void tomorrowDottedClock() {
    ReminderDraft d = ReminderDraft.parse("Yarın 15.00 bana toplantıyı hatırlat", now);
    assertEquals(now.plusDays(1).withHour(15), d.due);
    assertEquals("toplantıyı", d.title);
  }

  @Test
  public void namedDateAndClock() {
    ReminderDraft d = ReminderDraft.parse("20 Ekim 2026 saat 15:30 faturayı hatırlat", now);
    assertEquals(now.plusDays(2).withHour(15).withMinute(30), d.due);
    assertEquals("faturayı", d.title);
  }

  @Test
  public void missingTimeRequiresChoice() {
    ReminderDraft d = ReminderDraft.parse("Yarın faturayı hatırlat", now);
    assertNull(d.due);
    assertFalse(d.issue.isEmpty());
    assertFalse(d.audible);
  }

  @Test
  public void pastNeverSilentlyRolledForward() {
    assertNull(ReminderDraft.parse("Bugün saat 9 ilaç hatırlat", now).due);
  }

  @Test
  public void invalidDateRequiresChoice() {
    assertNull(ReminderDraft.parse("30 Şubat 2027 saat 15 kontrol hatırlat", now).due);
  }

  @Test
  public void weekdayDate() {
    assertEquals(
        now.plusDays(1).withHour(11),
        ReminderDraft.parse("Pazartesi saat on bir toplantı hatırlat", now).due);
  }

  @Test
  public void dottedDateDoesNotBecomeClock() {
    assertEquals(
        now.plusDays(2).withHour(16),
        ReminderDraft.parse("20.10.2026 saat 16 faturayı hatırlat", now).due);
  }

  @Test
  public void daylightGapRequiresChoice() {
    ZonedDateTime berlin = ZonedDateTime.of(2027, 3, 27, 10, 0, 0, 0, ZoneId.of("Europe/Berlin"));
    assertNull(ReminderDraft.parse("28.03.2027 saat 02:30 kontrol hatırlat", berlin).due);
  }

  @Test
  public void commandClassificationAvoidsDefinitions() {
    assertTrue(ReminderDraft.matches("Bir saat sonra beni ara diye hatırlat"));
    assertFalse(ReminderDraft.matches("Hatırlatma nedir?"));
    assertTrue(MessageDraft.matches("Anneme mesaj at: merhaba"));
    assertFalse(MessageDraft.matches("SMS nedir?"));
  }

  @Test
  public void messageDefaultIsInternetDraft() {
    MessageDraft d = MessageDraft.parse("+90 555 123 45 67 mesaj gönder: yarın görüşelim: saat 5");
    assertTrue(d.whatsapp);
    assertEquals("yarın görüşelim: saat 5", d.body);
    assertEquals("905551234567", MessageDraft.phone(d.recipient, true));
    assertTrue(d.preview().contains("taslak"));
  }

  @Test
  public void smsMustBeExplicit() {
    assertFalse(MessageDraft.parse("05551234567 SMS gönder: merhaba").whatsapp);
  }

  @Test
  public void personNameIsNeverSendingAddress() {
    MessageDraft d = MessageDraft.parse("Anneme mesaj at: merhaba");
    assertEquals("", d.recipient);
    try {
      d.preview();
      fail("Person hint accepted as number");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void emptyMessageIsNotReady() {
    MessageDraft d = new MessageDraft();
    d.recipient = "05551234567";
    try {
      d.preview();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void internationalPhoneNormalization() {
    assertEquals("905551234567", MessageDraft.phone("0555 123 45 67", true));
    assertEquals("447700900123", MessageDraft.phone("+44 7700 900123", true));
    try {
      MessageDraft.phone("abc@example.com", true);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      MessageDraft.phone("12345678", true);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void privateAndUnsafeLinksAreFiltered() {
    for (String url :
        new String[] {
          "http://example.com",
          "https://localhost/",
          "https://192.168.1.1/",
          "javascript:alert(1)",
          "https://user:pass@example.com",
          "https://printer.local/"
        }) assertEquals("", Research.safeLink(url));
    assertEquals("https://example.com/article", Research.safeLink("https://example.com/article"));
  }

  @Test
  public void searchResultsUnwrapDeduplicateAndBound() {
    StringBuilder html =
        new StringBuilder(
            "<div class='result'><a class='result__a'"
                + " href='//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Ffirst'>Başlık</a><div"
                + " class='result__snippet'>Kaynak özeti</div></div>");
    html.append(
        "<div class='result'><a class='result__a'"
            + " href='https://example.com/first'>Tekrar</a></div>");
    html.append(
        "<div class='result'><a class='result__a' href='http://localhost/test'>Güvensiz</a></div>");
    for (int i = 0; i < 10; i++)
      html.append(
          "<div class='result'><a class='result__a' href='https://example.org/"
              + i
              + "'>Sonuç</a></div>");
    List<Research.Source> result = Research.parseSearch(html.toString());
    assertEquals(5, result.size());
    assertEquals("https://example.com/first", result.get(0).url);
    assertEquals("Kaynak özeti", result.get(0).text);
  }

  @Test
  public void blockedSearchDoesNotInventResults() {
    assertTrue(Research.parseSearch("<html>Captcha required</html>").isEmpty());
  }

  @Test
  public void researchCommandKeepsTopic() {
    assertTrue(Research.matches("Güneş enerjisi hakkında araştır"));
    assertEquals("Güneş enerjisi", Research.query("Güneş enerjisi araştır"));
  }
}
