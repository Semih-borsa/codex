package com.semih.nova;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class AssistantBehaviorTest {
  @Test
  public void weatherQuestionKeepsCityAndTomorrow() {
    WeatherQuery q = WeatherQuery.parse("İstanbul'da yarın hava nasıl olacak?", "");
    assertEquals("istanbul", q.city);
    assertEquals(1, q.day);
  }

  @Test
  public void weatherWithoutCityAsksInsteadOfGuessing() {
    assertEquals("", WeatherQuery.parse("Yarın hava nasıl?", "").city);
    assertEquals("Ankara", WeatherQuery.parse("yarın hava nasıl", "Ankara").city);
  }

  @Test
  public void nidgeNameIsNotMutilated() {
    assertEquals("nigde", WeatherQuery.parse("Niğde hava durumu", "").city);
  }

  @Test
  public void airportIsNotWeather() {
    assertFalse(WeatherQuery.matches("havaalanı nedir"));
    assertTrue(WeatherQuery.matches("Yarın yağmur var mı"));
  }

  @Test
  public void tomorrowFollowupPreservesContext() {
    assertTrue(WeatherQuery.followup("Peki yarın?"));
    assertEquals(2, WeatherQuery.day("öbür gün hava durumu"));
  }

  @Test
  public void repeatedAnswerToDifferentQuestionRetries() {
    Store.Row a = new Store.Row();
    a.role = "user";
    a.body = "Birinci soru";
    Store.Row b = new Store.Row();
    b.role = "assistant";
    b.body = "Bu oldukça uzun bir önceki yanıttır ve yeni konuyla ilgili değildir.";
    assertTrue(AnswerQuality.retry(b.body, Arrays.asList(a, b), "Bambaşka bir soru"));
    assertFalse(AnswerQuality.retry(b.body, Arrays.asList(a, b), "Birinci soru"));
  }

  @Test
  public void sentenceLoopIsDetected() {
    assertTrue(AnswerQuality.loop("Aynı cümle tekrar ediyor. ".repeat(10)));
    assertFalse(AnswerQuality.loop("Ankara Türkiye'nin başkentidir."));
  }

  @Test
  public void selfIntroductionIsNotAnAnswerToEverything() {
    assertTrue(
        AnswerQuality.retry(
            "Ben Adaş, kişisel asistan olarak yardımcı olurum.",
            Collections.emptyList(),
            "Fotosentez nedir?"));
    assertFalse(
        AnswerQuality.retry(
            "Ben Adaş, kişisel asistan olarak yardımcı olurum.",
            Collections.emptyList(),
            "Sen kimsin?"));
  }

  @Test
  public void currentDataIsNotFabricated() {
    assertNotNull(AnswerQuality.currentInfo("Güncel dolar kuru kaç?"));
    assertNull(AnswerQuality.currentInfo("Döviz kuru nedir?"));
  }

  @Test
  public void silenceStopsListeningButDoesNotPretendSpeech() {
    SpeechBoundary b = new SpeechBoundary();
    short[] silence = new short[1024];
    boolean ended = false;
    for (int i = 0; i < 125; i++) ended = b.update(silence, silence.length);
    assertTrue(ended);
    assertFalse(b.heard());
  }

  @Test
  public void pauseAfterSpeechEndsTurn() {
    SpeechBoundary b = new SpeechBoundary();
    short[] voiced = new short[1024];
    Arrays.fill(voiced, (short) 2000);
    for (int i = 0; i < 8; i++) assertFalse(b.update(voiced, 1024));
    assertTrue(b.heard());
    short[] quiet = new short[1024];
    for (int i = 0; i < 21; i++) assertFalse(b.update(quiet, 1024));
    assertTrue(b.update(quiet, 1024));
  }

  @Test
  public void aClickIsNotSpeech() {
    SpeechBoundary b = new SpeechBoundary();
    short[] click = new short[1024];
    Arrays.fill(click, (short) 2000);
    b.update(click, 1024);
    for (int i = 0; i < 125; i++) b.update(new short[1024], 1024);
    assertFalse(b.heard());
  }

  @Test
  public void completionAfterStopCannotOpenMicrophone() {
    Conversation c = new Conversation();
    int old = c.start();
    c.move(old, Conversation.State.SPEAKING);
    assertTrue(c.canListen(old));
    c.stop();
    assertFalse(c.canListen(old));
    int next = c.start();
    assertFalse(c.move(old, Conversation.State.SPEAKING));
    assertFalse(c.canListen(old));
    assertTrue(c.move(next, Conversation.State.SPEAKING));
    assertTrue(c.canListen(next));
  }
}
