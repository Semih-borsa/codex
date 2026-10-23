package com.semih.sicil360;

import static com.semih.sicil360.Model.*;
import static org.junit.Assert.*;

import java.io.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class CrmTest {
  Person person(String id, String name, FollowUp follow) {
    Person p = new Person();
    p.sicil = id;
    p.ad = name;
    p.telefon = "05321234567";
    p.followUp = follow;
    return p;
  }

  @Test
  public void encryptedPayloadRoundTripAndCopyPreserveFollowAndNotes() throws Exception {
    State s = new State();
    Person p = person("0001", "İpek", FollowUp.AGAIN);
    p.not = "Tekrar aranacak";
    p.followUpChanged = 123456;
    s.people.put(p.sicil, p);
    Person loaded = Codec.decode(Codec.encode(s)).people.get("0001");
    assertEquals(FollowUp.AGAIN, loaded.followUp);
    assertEquals(p.not, loaded.not);
    assertEquals(123456, loaded.followUpChanged);
    assertEquals(FollowUp.AGAIN, p.copy().followUp);
    assertEquals(123456, p.copy().followUpChanged);
  }

  @Test
  public void oldVersionOneBackupLoadsWithoutLosingIdentifiersOrNotes() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream o = new DataOutputStream(bytes);
    o.writeInt(0x53333631);
    o.writeLong(7);
    o.writeInt(1);
    for (String value :
        new String[] {
          "0001", "İpek", "Işık", "05321234567", "Birim", "", "", "Eski Not", "Aktif"
        }) {
      byte[] b = value.getBytes("UTF-8");
      o.writeInt(b.length);
      o.write(b);
    }
    for (int i = 0; i < 4; i++) o.writeInt(0); // extras, templates, history, recent
    byte[] raw = bytes.toByteArray();
    State s = Codec.decode(raw);
    Person p = s.people.get("0001");
    assertEquals(7, s.revision);
    assertEquals("Eski Not", p.not);
    assertEquals("05321234567", p.telefon);
    assertEquals(FollowUp.PENDING, p.followUp);
  }

  @Test
  public void reportsRespectLocalMidnightAndDoNotInferSentFromOpening() {
    State s = new State();
    ZoneId zone = ZoneId.of("Europe/Istanbul");
    LocalDate day = LocalDate.of(2026, 10, 23);
    Person p = person("1", "A", FollowUp.COMPLETE);
    p.followUpChanged = Instant.parse("2026-10-22T21:00:00Z").toEpochMilli();
    s.people.put("1", p);
    s.people.put("2", person("2", "B", FollowUp.PENDING));
    s.people.put("3", person("3", "C", FollowUp.AGAIN));
    for (Status status : new Status[] {Status.ATTEMPT, Status.SENT_MARKED, Status.CANCELLED}) {
      Event e = new Event();
      e.status = status;
      e.time = p.followUpChanged;
      s.history.add(e);
    }
    Event yesterday = new Event();
    yesterday.time = p.followUpChanged - 1;
    yesterday.status = Status.SENT_MARKED;
    s.history.add(yesterday);
    Crm.Report r = Crm.report(s, day, zone);
    assertEquals(3, r.total);
    assertEquals(1, r.pending);
    assertEquals(1, r.again);
    assertEquals(1, r.complete);
    assertEquals(1, r.completedToday);
    assertEquals(3, r.preparedToday);
    assertEquals(1, r.sentMarkedToday);
    p.followUp = FollowUp.PENDING;
    assertEquals(0, Crm.report(s, day, zone).completedToday);
  }

  @Test
  public void filtersByExactTrackingAndTurkishNamesWithoutConflatingSicils() {
    State s = new State();
    s.people.put("001", person("001", "İpek", FollowUp.AGAIN));
    s.people.put("1", person("1", "İpek", FollowUp.COMPLETE));
    assertEquals(2, Crm.people(s, null, "ipek").size());
    assertEquals("001", Crm.people(s, FollowUp.AGAIN, "İPEK").get(0).sicil);
    assertTrue(Crm.people(s, FollowUp.PENDING, "").isEmpty());
  }

  @Test
  public void updateImportRetainsSeparateFollowup() {
    State s = new State();
    Person p = person("001", "İpek", FollowUp.AGAIN);
    p.followUpChanged = 345;
    s.people.put(p.sicil, p);
    Row row = new Row(2, person("001", "Yeni Ad", FollowUp.PENDING));
    Plan plan = Model.plan(s, Arrays.asList(row), ImportMode.UPDATE, Collections.emptyMap());
    assertEquals(FollowUp.AGAIN, plan.result.people.get("001").followUp);
    assertEquals(345, plan.result.people.get("001").followUpChanged);
  }
}
