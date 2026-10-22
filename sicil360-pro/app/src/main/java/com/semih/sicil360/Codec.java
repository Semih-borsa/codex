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
    o.writeInt(0x53333631);
    o.writeLong(s.revision);
    o.writeInt(s.people.size());
    for (Person p : s.people.values()) {
      for (String v :
          new String[] {
            p.sicil, p.ad, p.soyad, p.telefon, p.birim, p.unvan, p.email, p.not, p.durum
          }) text(o, v);
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
    o.flush();
    if (b.size() > MAX_BYTES) throw new IOException("Veri Boyutu 32 MB Sınırını Aştı.");
    return b.toByteArray();
  }

  public static State decode(byte[] bytes) throws IOException {
    if (bytes.length > MAX_BYTES) throw new IOException("Veri Dosyası Çok Büyük.");
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    if (in.readInt() != 0x53333631) throw new IOException("Desteklenmeyen Yedek Sürümü.");
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
