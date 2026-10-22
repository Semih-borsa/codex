package com.semih.sicil360;

import static com.semih.sicil360.Model.*;
import static org.junit.Assert.*;

import java.net.*;
import java.util.*;
import org.junit.Test;

public class ModelTest {
  Person person(String id) {
    Person p = new Person();
    p.sicil = id;
    p.ad = "İpek";
    p.soyad = "Işık";
    p.telefon = "05321234567";
    return p;
  }

  @Test
  public void turkishPhoneFormatsAgree() {
    for (String n :
        new String[] {
          "0532 123 45 67", "5321234567", "+90 (532) 123-45-67", "00905321234567", "905321234567"
        }) assertEquals("905321234567", phone(n));
  }

  @Test
  public void internationalAndBusinessLandlineSupported() {
    assertEquals("442071234567", phone("+44 20 7123 4567"));
    assertEquals("902121234567", phone("0212 123 45 67"));
  }

  @Test
  public void badPhonesRejected() {
    for (String n :
        new String[] {
          "",
          "0532",
          "abc",
          "0532 1234567 dahili 42",
          "+9005321234567",
          "++905321234567",
          "1234567890",
          "+0123456789"
        }) {
      try {
        phone(n);
        fail(n);
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void exactSicilPreservesZerosAndDoesNotChooseNames() {
    State s = new State();
    s.people.put("0001", person("0001"));
    s.people.put("1", person("1"));
    assertEquals("0001", search(s, "0001", 30).get(0).sicil);
    assertEquals(1, search(s, "0001", 30).size());
    assertEquals("1", search(s, "1", 30).get(0).sicil);
    assertEquals(2, search(s, "ipek isik", 30).size());
  }

  @Test
  public void literalTemplateNeverAddsOrReplacesIdentity() throws Exception {
    Person p = person("0001");
    String text = "Merhaba {AD}. A&B?\nİşlem için görüşelim.";
    String link = whatsapp(p, text);
    String decoded = URLDecoder.decode(link.substring(link.indexOf("text=") + 5), "UTF-8");
    assertEquals(text, decoded);
    assertFalse(decoded.contains(p.sicil));
    assertFalse(decoded.contains(p.ad));
    assertTrue(link.contains("%26"));
    assertTrue(link.contains("%0A"));
  }

  @Test
  public void emptyAndOversizedMessageRejected() {
    for (String t : new String[] {"  ", new String(new char[10001]).replace('\0', 'a')})
      try {
        literalMessage(t);
        fail();
      } catch (IllegalArgumentException expected) {
      }
  }

  @Test
  public void keepUpdateAndReplaceCounts() {
    State s = new State();
    s.people.put("01", person("01"));
    s.people.put("OLD", person("OLD"));
    List<Row> rows = Arrays.asList(new Row(2, person("01")), new Row(3, person("02")));
    Plan keep = plan(s, rows, ImportMode.KEEP, Collections.emptyMap());
    assertEquals(1, keep.added);
    assertEquals(1, keep.skipped);
    assertEquals(0, keep.updated);
    Plan update = plan(s, rows, ImportMode.UPDATE, Collections.emptyMap());
    assertEquals(1, update.updated);
    assertEquals(1, update.added);
    Plan replace = plan(s, rows, ImportMode.REPLACE, Collections.emptyMap());
    assertEquals(1, replace.removed);
    assertFalse(replace.result.people.containsKey("OLD"));
    assertEquals(2, s.people.size());
  }

  @Test
  public void duplicateNeverSilentlySelected() {
    List<Row> rows = Arrays.asList(new Row(2, person("01")), new Row(3, person("01")));
    Plan p = plan(new State(), rows, ImportMode.UPDATE, Collections.emptyMap());
    assertEquals(1, p.conflicts.size());
    assertEquals(0, p.result.people.size());
    p = plan(new State(), rows, ImportMode.UPDATE, Collections.singletonMap("01", 3));
    assertEquals(1, p.added);
    assertEquals(1, p.skipped);
    p = plan(new State(), rows, ImportMode.UPDATE, Collections.singletonMap("01", -1));
    assertEquals(0, p.added);
    assertEquals(2, p.skipped);
  }

  @Test
  public void duplicateWithBadPhoneStillRequiresResolution() {
    Person bad = person("01");
    bad.telefon = "123";
    Plan p =
        plan(
            new State(),
            Arrays.asList(new Row(2, bad), new Row(3, person("01"))),
            ImportMode.UPDATE,
            Collections.emptyMap());
    assertEquals(1, p.errors);
    assertEquals(1, p.conflicts.size());
    assertTrue(p.result.people.isEmpty());
  }

  @Test
  public void unmappedOptionalFieldsSurviveUpdate() {
    State s = new State();
    Person old = person("01");
    old.birim = "Mali İşler";
    old.extra.put("Oda", "12");
    s.people.put("01", old);
    Row row = new Row(2, person("01"));
    row.fields = new boolean[] {true, true, false, true, false, false, false, false, false};
    Person result =
        plan(s, Arrays.asList(row), ImportMode.UPDATE, Collections.emptyMap())
            .result
            .people
            .get("01");
    assertEquals("Mali İşler", result.birim);
    assertEquals("12", result.extra.get("Oda"));
    assertEquals("Işık", result.soyad);
  }

  @Test
  public void badAndBlankPhoneRowsAreVisibleErrors() {
    Person p = person("01");
    p.telefon = "";
    Plan plan =
        plan(new State(), Arrays.asList(new Row(2, p)), ImportMode.KEEP, Collections.emptyMap());
    assertEquals(1, plan.errors);
    assertFalse(plan.rows.get(0).error.isEmpty());
    assertTrue(plan.result.people.isEmpty());
  }

  @Test
  public void codecPreservesAllFieldsAndStatusWithoutMessageHistory() throws Exception {
    State s = new State();
    Person p = person("0001");
    p.extra.put("Alan", "Özel & Türkçe");
    s.people.put(p.sicil, p);
    Template t = new Template();
    t.name = "Özel";
    t.text = "Aynen {AD}";
    s.templates.add(t);
    Event event = new Event();
    event.sicil = p.sicil;
    event.status = Status.UNVERIFIED;
    s.history.add(event);
    State decoded = Codec.decode(Codec.encode(s));
    assertEquals(p.telefon, decoded.people.get("0001").telefon);
    assertEquals("Özel & Türkçe", decoded.people.get("0001").extra.get("Alan"));
    assertEquals(t.text, decoded.templates.get(0).text);
    assertEquals(Status.UNVERIFIED, decoded.history.get(0).status);
  }

  @Test
  public void codecRejectsDuplicateIdsAndTrailingGarbage() throws Exception {
    State s = new State();
    s.people.put("a", person("01"));
    s.people.put("b", person("01"));
    try {
      Codec.decode(Codec.encode(s));
      fail();
    } catch (java.io.IOException expected) {
    }
    byte[] good = Codec.encode(new State());
    try {
      Codec.decode(Arrays.copyOf(good, good.length + 1));
      fail();
    } catch (java.io.IOException expected) {
    }
  }

  @Test
  public void passwordBackupRandomizedAuthenticatedAndPortable() throws Exception {
    byte[] plain = Codec.encode(new State());
    char[] pass = "test-password-123".toCharArray();
    byte[] a = Backup.seal(plain, pass), b = Backup.seal(plain, pass);
    assertFalse(Arrays.equals(a, b));
    assertArrayEquals(plain, Backup.open(a, pass));
    try {
      Backup.open(a, "wrong-password".toCharArray());
      fail();
    } catch (java.io.IOException expected) {
    }
    a[a.length - 1] ^= 1;
    try {
      Backup.open(a, pass);
      fail();
    } catch (java.io.IOException expected) {
    }
  }

  @Test
  public void hundredThousandExactLookupRemainsConstantTime() {
    State s = new State();
    for (int i = 0; i < 100000; i++) {
      Person p = person(String.format(Locale.ROOT, "%06d", i));
      s.people.put(p.sicil, p);
    }
    long start = System.nanoTime();
    for (int i = 0; i < 5000; i++) assertEquals("099999", search(s, "099999", 30).get(0).sicil);
    long ms = (System.nanoTime() - start) / 1000000;
    System.out.println("100000 contacts / 5000 exact lookups: " + ms + " ms");
    assertTrue(ms < 5000);
  }

  @Test
  public void historyBoundedAndRecentDeduplicated() {
    State s = new State();
    for (int i = 0; i < 1100; i++) addHistory(s, new Event());
    assertEquals(1000, s.history.size());
    recent(s, "01");
    recent(s, "02");
    recent(s, "01");
    assertEquals(Arrays.asList("01", "02"), s.recent);
  }

  @Test
  public void formattedPhoneSearchMatchesWithoutChoosing() {
    State s = new State();
    Person p = person("01");
    s.people.put("01", p);
    assertEquals(1, search(s, "0532 123", 30).size());
    assertEquals(0, search(s, "başka", 30).size());
  }
}
