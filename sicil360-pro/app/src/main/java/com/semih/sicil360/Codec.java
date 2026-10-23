package com.semih.sicil360;

import static com.semih.sicil360.Model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Bounded, versioned binary format; no Java deserialization or executable input. */
public final class Codec {
  static final int MAX_BYTES = 32 * 1024 * 1024;

  static void text(DataOutputStream out, String s) throws IOException {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    if (b.length > 65536) throw new IOException("Alan Çok Uzun.");
    out.writeInt(b.length);
    out.write(b);
  }

  static String text(DataInputStream in) throws IOException {
    int n = in.readInt();
    if (n < 0 || n > 65536) throw new IOException("Geçersiz Alan Uzunluğu.");
    byte[] b = new byte[n];
    in.readFully(b);
    return new String(b, StandardCharsets.UTF_8);
  }

  static int count(DataInputStream in, int max) throws IOException {
    int n = in.readInt();
    if (n < 0 || n > max) throw new IOException("Kayıt Sınırı Aşıldı.");
    return n;
  }

  public static byte[] encode(State s) throws IOException {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    DataOutputStream o = new DataOutputStream(b);
    o.writeInt(0x53333634);
    o.writeLong(s.revision);
    o.writeInt(s.people.size());
    for (Person p : s.people.values()) {
      for (String v :
          new String[] {
            p.sicil, p.ad, p.soyad, p.telefon, p.birim, p.unvan, p.email, p.not, p.durum
          }) text(o, v);
      o.writeInt(p.followUp.ordinal());
      o.writeLong(p.followUpChanged);
      o.writeInt(p.extra.size());
      for (java.util.Map.Entry<String, String> e : p.extra.entrySet()) {
        text(o, e.getKey());
        text(o, e.getValue());
      }
    }
    o.writeInt(s.templates.size());
    for (Template t : s.templates) {
      text(o, t.id);
      text(o, t.name);
      text(o, t.category);
      text(o, t.text);
      o.writeLong(t.lastUsed);
    }
    o.writeInt(s.history.size());
    for (Event e : s.history) {
      text(o, e.id);
      text(o, e.sicil);
      text(o, e.template);
      o.writeLong(e.time);
      o.writeInt(e.status.ordinal());
    }
    o.writeInt(s.recent.size());
    for (String id : s.recent) text(o, id);
    o.writeInt(s.debts.size());
    for (Payments.Ledger l : s.debts) {
      text(o, l.id);
      text(o, l.name);
      text(o, l.fingerprint);
      text(o, l.period);
      o.writeLong(l.created);
      o.writeInt(l.accounts.size());
      for (Payments.Account a : l.accounts.values()) {
        text(o, a.code);
        text(o, a.title);
        text(o, a.first);
        text(o, a.last);
        o.writeLong(a.original);
        o.writeInt(a.payments.size());
        for (Payments.Entry e : a.payments) {
          text(o, e.id);
          text(o, e.note);
          o.writeLong(e.amount);
          o.writeLong(e.time);
          o.writeBoolean(e.cancelled);
        }
      }
    }
    o.flush();
    if (b.size() > MAX_BYTES) throw new IOException("Veri Boyutu 32 MB Sınırını Aştı.");
    return b.toByteArray();
  }

  public static State decode(byte[] bytes) throws IOException {
    if (bytes.length > MAX_BYTES) throw new IOException("Veri Dosyası Çok Büyük.");
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    int version = in.readInt();
    if (version != 0x53333631
        && version != 0x53333632
        && version != 0x53333633
        && version != 0x53333634) throw new IOException("Desteklenmeyen Yedek Sürümü.");
    State s = new State();
    s.revision = in.readLong();
    int n = count(in, MAX_PEOPLE);
    for (int i = 0; i < n; i++) {
      Person p = new Person();
      p.sicil = text(in);
      p.ad = text(in);
      p.soyad = text(in);
      p.telefon = text(in);
      p.birim = text(in);
      p.unvan = text(in);
      p.email = text(in);
      p.not = text(in);
      p.durum = text(in);
      if (version >= 0x53333632) {
        int follow = in.readInt();
        if (follow < 0 || follow >= FollowUp.values().length)
          throw new IOException("Geçersiz Takip Durumu.");
        p.followUp = FollowUp.values()[follow];
        p.followUpChanged = in.readLong();
        if (p.followUpChanged < 0) throw new IOException("Geçersiz Takip Tarihi.");
      }
      int e = count(in, 20);
      for (int k = 0; k < e; k++) p.extra.put(text(in), text(in));
      try {
        validate(p);
      } catch (Exception bad) {
        throw new IOException("Yedekte Geçersiz Kişi Kaydı.");
      }
      if (s.people.put(p.sicil, p) != null) throw new IOException("Yedekte Mükerrer Sicil Var.");
    }
    n = count(in, 1000);
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (int i = 0; i < n; i++) {
      Template t = new Template();
      t.id = text(in);
      t.name = text(in);
      t.category = text(in);
      t.text = text(in);
      t.lastUsed = in.readLong();
      if (!ids.add(t.id) || t.name.trim().isEmpty() || t.text.length() > 10000)
        throw new IOException("Geçersiz Şablon.");
      s.templates.add(t);
    }
    n = count(in, MAX_HISTORY);
    for (int i = 0; i < n; i++) {
      Event e = new Event();
      e.id = text(in);
      e.sicil = text(in);
      e.template = text(in);
      e.time = in.readLong();
      int ordinal = in.readInt();
      if (ordinal < 0 || ordinal >= Status.values().length)
        throw new IOException("Geçersiz İşlem Durumu.");
      e.status = Status.values()[ordinal];
      s.history.add(e);
    }
    n = count(in, 20);
    for (int i = 0; i < n; i++) s.recent.add(text(in));
    if (version >= 0x53333633) {
      int ledgers = count(in, 100);
      java.util.HashSet<String> ledgerIds = new java.util.HashSet<>();
      for (int j = 0; j < ledgers; j++) {
        Payments.Ledger l = new Payments.Ledger();
        l.id = text(in);
        l.name = text(in);
        l.fingerprint = text(in);
        if (version >= 0x53333634) l.period = text(in);
        l.created = in.readLong();
        if (version == 0x53333633)
          l.period =
              java.time.Instant.ofEpochMilli(l.created)
                  .atZone(java.time.ZoneId.systemDefault())
                  .toLocalDate()
                  .toString()
                  .substring(0, 7);
        try {
          java.time.YearMonth.parse(l.period);
        } catch (Exception invalid) {
          throw new IOException("Geçersiz Borç Dönemi.");
        }
        if (l.id.isEmpty() || !ledgerIds.add(l.id) || l.name.length() > 100 || l.created < 0)
          throw new IOException("Geçersiz Borç Listesi.");
        int accounts = count(in, MAX_PEOPLE);
        for (int k = 0; k < accounts; k++) {
          Payments.Account a = new Payments.Account();
          a.code = text(in);
          a.title = text(in);
          a.first = text(in);
          a.last = text(in);
          a.original = in.readLong();
          if (!a.code.matches("[\\p{L}\\p{N}._/-]{1,64}")
              || a.name().isEmpty()
              || a.original < 0
              || a.original > Payments.MAX
              || l.accounts.put(a.code, a) != null) throw new IOException("Geçersiz Borç Kaydı.");
          int payments = count(in, 10000);
          java.util.HashSet<String> paymentIds = new java.util.HashSet<>();
          for (int m = 0; m < payments; m++) {
            Payments.Entry e = new Payments.Entry();
            e.id = text(in);
            e.note = text(in);
            e.amount = in.readLong();
            e.time = in.readLong();
            e.cancelled = in.readBoolean();
            if (e.id.isEmpty()
                || !paymentIds.add(e.id)
                || e.note.length() > 500
                || e.amount <= 0
                || e.amount > Payments.MAX
                || e.time < 0) throw new IOException("Geçersiz Ödeme Kaydı.");
            a.payments.add(e);
          }
          if (a.paid() > Payments.MAX) throw new IOException("Ödeme Tutar Sınırı Aşıldı.");
        }
        s.debts.add(l);
      }
    }
    if (in.read() != -1) throw new IOException("Dosya Sonunda Beklenmeyen Veri.");
    return s;
  }

  public static State copy(State s) {
    try {
      return decode(encode(s));
    } catch (IOException e) {
      throw new IllegalArgumentException("Veri Doğrulanamadı.", e);
    }
  }
}
