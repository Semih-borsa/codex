package com.semih.sicil360;

import java.text.Normalizer;
import java.time.*;
import java.util.*;

/** Platform-independent rules. Templates are always literal text, never interpolated. */
public final class Model {
  public static final int MAX_PEOPLE = 50000, MAX_HISTORY = 1000;

  public static final class Person {
    public String sicil = "",
        ad = "",
        soyad = "",
        telefon = "",
        birim = "",
        unvan = "",
        email = "",
        not = "",
        durum = "";
    public final LinkedHashMap<String, String> extra = new LinkedHashMap<>();
    private String cachedSource = "", cachedName = "";

    public String searchable() {
      String source = name();
      if (!source.equals(cachedSource)) {
        cachedSource = source;
        cachedName = fold(source);
      }
      return cachedName;
    }

    public String name() {
      return (ad + " " + soyad).trim();
    }

    public Person copy() {
      Person p = new Person();
      p.sicil = sicil;
      p.ad = ad;
      p.soyad = soyad;
      p.telefon = telefon;
      p.birim = birim;
      p.unvan = unvan;
      p.email = email;
      p.not = not;
      p.durum = durum;
      p.extra.putAll(extra);
      return p;
    }
  }

  public static final class Template {
    public String id = UUID.randomUUID().toString(),
        name = "",
        category = "Özel Mesajlar",
        text = "";
    public long lastUsed;

    public Template copy() {
      Template t = new Template();
      t.id = id;
      t.name = name;
      t.category = category;
      t.text = text;
      t.lastUsed = lastUsed;
      return t;
    }
  }

  public enum Status {
    PREPARED("Mesaj Hazırlandı"),
    ATTEMPT("WhatsApp Açılmaya Çalışıldı"),
    CHAT_CONFIRMED("Sohbet Açıldı · Kullanıcı Doğruladı"),
    SENT_MARKED("Gönderim Tamamlandı · Kullanıcı İşaretledi"),
    UNVERIFIED("Gönderim Doğrulanamadı"),
    CANCELLED("İşlem İptal Edildi"),
    ERROR("Açılış Hatası");
    public final String label;

    Status(String label) {
      this.label = label;
    }
  }

  public static final class Event {
    public String id = UUID.randomUUID().toString(), sicil = "", template = "Özel Mesaj";
    public long time = System.currentTimeMillis();
    public Status status = Status.PREPARED;
  }

  public static final class State {
    public long revision;
    public final LinkedHashMap<String, Person> people = new LinkedHashMap<>();
    public final List<Template> templates = new ArrayList<>();
    public final List<Event> history = new ArrayList<>();
    public final List<String> recent = new ArrayList<>();
  }

  public static String fold(String s) {
    return Normalizer.normalize(s.replace('ı', 'i').replace('İ', 'I'), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toUpperCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .trim();
  }

  public static String phone(String raw) {
    String s = raw.trim();
    if (s.isEmpty()) throw new IllegalArgumentException("Telefon Numarası Eksik.");
    if (!s.matches("[+0-9\\s()./-]+"))
      throw new IllegalArgumentException("Telefon numarasında geçersiz karakter var.");
    String digits = s.replaceAll("[\\s()./-]", "");
    if (digits.startsWith("00")) digits = "+" + digits.substring(2);
    if (digits.startsWith("+")) {
      if (!digits.matches("\\+[1-9][0-9]{7,14}"))
        throw new IllegalArgumentException("Uluslararası telefon biçimi geçersiz.");
      digits = digits.substring(1);
    } else if (digits.matches("0[2-5][0-9]{9}")) digits = "90" + digits.substring(1);
    else if (digits.matches("[2-5][0-9]{9}")) digits = "90" + digits;
    else if (!digits.matches("90[2-5][0-9]{9}"))
      throw new IllegalArgumentException("Telefonu 05xx… veya +ülke kodu biçiminde girin.");
    if (digits.startsWith("90") && !digits.matches("90[2-5][0-9]{9}"))
      throw new IllegalArgumentException(
          "Türkiye numarası 10 haneli alan/abone numarası içermeli.");
    return digits;
  }

  public static void validate(Person p) {
    if (!p.sicil.matches("[\\p{L}\\p{N}._/-]{1,64}"))
      throw new IllegalArgumentException("Sicil 1–64 karakter olmalı; boşluk içeremez.");
    if (p.name().isEmpty()) throw new IllegalArgumentException("Ad veya Soyad Girin.");
    if (p.name().length() > 200) throw new IllegalArgumentException("Ad Soyad Çok Uzun.");
    if (!p.telefon.isEmpty()) phone(p.telefon);
    if (p.extra.size() > 20)
      throw new IllegalArgumentException("En fazla 20 özel alan saklanabilir.");
  }

  public static String literalMessage(String text) {
    if (text.trim().isEmpty()) throw new IllegalArgumentException("Mesaj Boş Olamaz.");
    if (text.length() > 10000)
      throw new IllegalArgumentException("Mesaj en fazla 10.000 karakter olabilir.");
    return text;
  }

  public static String whatsapp(Person p, String text) {
    validate(p);
    String number = phone(p.telefon);
    literalMessage(text);
    try {
      return "https://wa.me/"
          + number
          + "?text="
          + java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static List<Person> search(State s, String query, int limit) {
    String q = query.trim(), norm = fold(q), digits = q.replaceAll("\\D", "");
    List<Person> result = new ArrayList<>();
    Person exact = s.people.get(q);
    if (exact != null) {
      result.add(exact);
      return result;
    }
    for (Person p : s.people.values()) {
      if (p == exact) continue;
      if (norm.isEmpty()
          || p.sicil.contains(q)
          || p.searchable().contains(norm)
          || (!digits.isEmpty() && p.telefon.replaceAll("\\D", "").contains(digits))) result.add(p);
      if (result.size() >= limit) break;
    }
    return result;
  }

  public static void addHistory(State s, Event e) {
    s.history.add(0, e);
    while (s.history.size() > MAX_HISTORY) s.history.remove(s.history.size() - 1);
  }

  public static void recent(State s, String id) {
    s.recent.remove(id);
    s.recent.add(0, id);
    while (s.recent.size() > 20) s.recent.remove(20);
  }

  static String field(Person p, int i) {
    return new String[] {
          p.sicil, p.ad, p.soyad, p.telefon, p.birim, p.unvan, p.email, p.not, p.durum
        }
        [i];
  }

  static void setField(Person p, int i, String value) {
    switch (i) {
      case 0:
        p.sicil = value;
        break;
      case 1:
        p.ad = value;
        break;
      case 2:
        p.soyad = value;
        break;
      case 3:
        p.telefon = value;
        break;
      case 4:
        p.birim = value;
        break;
      case 5:
        p.unvan = value;
        break;
      case 6:
        p.email = value;
        break;
      case 7:
        p.not = value;
        break;
      case 8:
        p.durum = value;
        break;
    }
  }

  public enum ImportMode {
    KEEP,
    UPDATE,
    REPLACE
  }

  public static final class Row {
    public int number;
    public Person person;
    public String error = "";
    public boolean[] fields;
    public boolean extraProvided;

    public Row(int n, Person p) {
      number = n;
      person = p;
    }
  }

  public static final class Plan {
    public final List<Row> rows = new ArrayList<>();
    public final LinkedHashMap<String, List<Row>> conflicts = new LinkedHashMap<>();
    public int added, updated, skipped, errors, removed;
    public State result;
  }

  public static Plan plan(
      State base, List<Row> rows, ImportMode mode, Map<String, Integer> resolutions) {
    Plan plan = new Plan();
    plan.result = Codec.copy(base);
    plan.rows.addAll(rows);
    if (mode == ImportMode.REPLACE) plan.result.people.clear();
    LinkedHashMap<String, List<Row>> groups = new LinkedHashMap<>();
    for (Row row : rows) {
      if (row.person == null) {
        plan.errors++;
        continue;
      }
      if (row.error.isEmpty())
        try {
          validate(row.person);
          if (row.person.telefon.isEmpty())
            throw new IllegalArgumentException("Telefon Numarası Eksik.");
        } catch (IllegalArgumentException e) {
          row.error = e.getMessage();
        }
      if (!row.error.isEmpty()) plan.errors++;
      if (!row.person.sicil.isEmpty())
        groups.computeIfAbsent(row.person.sicil, k -> new ArrayList<>()).add(row);
    }
    for (Map.Entry<String, List<Row>> group : groups.entrySet()) {
      Row chosen = group.getValue().get(0);
      if (group.getValue().size() > 1) {
        if (!resolutions.containsKey(group.getKey())) {
          plan.conflicts.put(group.getKey(), group.getValue());
          continue;
        }
        int selected = resolutions.get(group.getKey());
        chosen =
            group.getValue().stream().filter(r -> r.number == selected).findFirst().orElse(null);
        for (Row row : group.getValue()) if (row != chosen && row.error.isEmpty()) plan.skipped++;
      }
      if (chosen == null || !chosen.error.isEmpty()) continue;
      boolean exists = plan.result.people.containsKey(group.getKey());
      if (exists && mode == ImportMode.KEEP) {
        plan.skipped++;
        continue;
      }
      Person imported = chosen.person.copy();
      if (exists && mode == ImportMode.UPDATE && chosen.fields != null) {
        Person old = plan.result.people.get(group.getKey()).copy();
        for (int i = 0; i < 9; i++) if (!chosen.fields[i]) setField(imported, i, field(old, i));
        java.util.LinkedHashMap<String, String> merged = new java.util.LinkedHashMap<>(old.extra);
        if (chosen.extraProvided) merged.putAll(imported.extra);
        imported.extra.clear();
        imported.extra.putAll(merged);
      }
      plan.result.people.put(group.getKey(), imported);
      if (exists) plan.updated++;
      else plan.added++;
    }
    if (mode == ImportMode.REPLACE) {
      plan.removed =
          (int)
              base.people.keySet().stream()
                  .filter(id -> !plan.result.people.containsKey(id))
                  .count();
    }
    if (plan.result.people.size() > MAX_PEOPLE)
      throw new IllegalArgumentException("En fazla 50.000 kişi saklanabilir.");
    plan.result.recent.removeIf(id -> !plan.result.people.containsKey(id));
    return plan;
  }
}
