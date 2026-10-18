package com.semih.nova;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.*;

final class ReminderDraft {
  final String title, issue;
  final ZonedDateTime due;
  final boolean audible;

  ReminderDraft(String title, ZonedDateTime due, boolean audible, String issue) {
    this.title = title;
    this.due = due;
    this.audible = audible;
    this.issue = issue;
  }

  static boolean matches(String q) {
    String s = WeatherQuery.normalize(q);
    return !s.contains("nedir")
        && !s.contains("ne demek")
        && (s.matches("(?s).*\\bhatirlat(?:ir misin|mani istiyorum|sana|iver|ir|\\b).*")
            || s.matches("(?s).*\\bhatirlatma\\b.*\\b(ekle|olustur|kur|ayarla)\\b.*"));
  }

  static final String[] DAYS = {
    "pazartesi", "sali", "carsamba", "persembe", "cuma", "cumartesi", "pazar"
  };
  static final String[] MONTHS = {
    "ocak", "subat", "mart", "nisan", "mayis", "haziran", "temmuz", "agustos", "eylul", "ekim",
    "kasim", "aralik"
  };
  static final String[] UNITS = {
    "sifir", "bir", "iki", "uc", "dort", "bes", "alti", "yedi", "sekiz", "dokuz"
  };
  static final String[] TENS = {"", "on", "yirmi", "otuz", "kirk", "elli"};

  static int number(String text) {
    String n = WeatherQuery.normalize(text).trim();
    if (n.matches("[0-9]{1,3}")) return Integer.parseInt(n);
    for (int i = 0; i < 10; i++) if (UNITS[i].equals(n)) return i;
    for (int i = 1; i < TENS.length; i++) {
      if (TENS[i].equals(n)) return i * 10;
      for (int j = 1; j < 10; j++) if ((TENS[i] + " " + UNITS[j]).equals(n)) return i * 10 + j;
    }
    return -1;
  }

  static String numberPattern() {
    return "(?:[0-9]{1,3}|(?:on|yirmi|otuz|kirk|elli)(?:"
               + " (?:bir|iki|uc|dort|bes|alti|yedi|sekiz|dokuz))?|sifir|bir|iki|uc|dort|bes|alti|yedi|sekiz|dokuz)";
  }

  static ReminderDraft parse(String original, ZonedDateTime now) {
    String source = java.text.Normalizer.normalize(original, java.text.Normalizer.Form.NFC),
        s = WeatherQuery.normalize(source);
    boolean audible = s.contains("sesli") || s.contains("sesle");
    StringBuilder body = new StringBuilder(source);
    String issue = "";
    ZonedDateTime due = null;
    LocalDate date = null;
    LocalTime time = null;
    List<int[]> removed = new ArrayList<>();
    Matcher relative =
        Pattern.compile("\\b(" + numberPattern() + ") (dakika|saat|gun) sonra\\b").matcher(s);
    if (relative.find()) {
      int n = number(relative.group(1));
      if (n <= 0 || n > 365)
        return new ReminderDraft("", null, audible, "Süre pozitif ve en fazla 365 birim olmalı.");
      due =
          relative.group(2).equals("dakika")
              ? now.plusMinutes(n)
              : relative.group(2).equals("saat") ? now.plusHours(n) : now.plusDays(n);
      due = due.withNano(0);
      removed.add(new int[] {relative.start(), relative.end()});
    } else {
      Matcher explicit =
          Pattern.compile("\\b([0-3]?[0-9])[./]([01]?[0-9])(?:[./]([0-9]{4}))?\\b").matcher(s);
      Matcher clock =
          Pattern.compile(
                  "\\b(?:saat\\s+)?([0-2]?[0-9])[:.]([0-5][0-9])(?:['’]?(?:te|ta|de|da))?\\b")
              .matcher(s);
      // A dotted clock needs 'saat' or an explicit locative, otherwise a dotted date stays a date.
      boolean hasClock = false;
      while (clock.find()) {
        String token = clock.group();
        if (token.contains(".")
            && !token.startsWith("saat")
            && !s.matches(
                "(?s).*\\b(bugun|yarin|obur"
                    + " gun|pazartesi|sali|carsamba|persembe|cuma|cumartesi|pazar)\\b.*")
            && Integer.parseInt(clock.group(2)) <= 12
            && !token.matches(".*(?:te|ta|de|da)$")) continue;
        int h = Integer.parseInt(clock.group(1)), m = Integer.parseInt(clock.group(2));
        if (h > 23) {
          issue = "Saat 00–23 arasında olmalı.";
          break;
        }
        time = LocalTime.of(h, m);
        removed.add(new int[] {clock.start(), clock.end()});
        hasClock = true;
        break;
      }
      if (!hasClock && issue.isEmpty()) {
        Matcher whole =
            Pattern.compile(
                    "\\b(?:saat|sabah|aksam|gece|ogleden sonra)\\s+("
                        + numberPattern()
                        + ")(?:['’]?(?:te|ta|de|da|e|a))?\\b")
                .matcher(s);
        if (whole.find()) {
          int h = number(whole.group(1));
          if ((s.contains("aksam") || s.contains("ogleden sonra")) && h >= 1 && h <= 11) h += 12;
          if (h >= 0 && h <= 23) time = LocalTime.of(h, 0);
          else issue = "Saat 00–23 arasında olmalı.";
          removed.add(new int[] {whole.start(), whole.end()});
        }
      }
      if (s.contains("obur gun")) {
        date = now.toLocalDate().plusDays(2);
        mark(s, "obur gun", removed);
      } else if (s.contains("yarin")) {
        date = now.toLocalDate().plusDays(1);
        mark(s, "yarin", removed);
      } else if (s.contains("bugun")) {
        date = now.toLocalDate();
        mark(s, "bugun", removed);
      }
      try {
        String monthNames = String.join("|", MONTHS);
        Matcher named =
            Pattern.compile(
                    "\\b([0-3]?[0-9]) ("
                        + monthNames
                        + ")(?: ([0-9]{4}))?(?:['’]?(?:te|ta|de|da))?\\b")
                .matcher(s);
        if (named.find()) {
          int month = Arrays.asList(MONTHS).indexOf(named.group(2)) + 1;
          date =
              LocalDate.of(
                  named.group(3) == null ? now.getYear() : Integer.parseInt(named.group(3)),
                  month,
                  Integer.parseInt(named.group(1)));
          removed.add(new int[] {named.start(), named.end()});
        } else
          while (explicit.find()) {
            boolean overlaps = false;
            for (int[] span : removed)
              if (explicit.start() < span[1] && explicit.end() > span[0]) overlaps = true;
            if (overlaps) continue;
            date =
                LocalDate.of(
                    explicit.group(3) == null ? now.getYear() : Integer.parseInt(explicit.group(3)),
                    Integer.parseInt(explicit.group(2)),
                    Integer.parseInt(explicit.group(1)));
            removed.add(new int[] {explicit.start(), explicit.end()});
            break;
          }
        if (date == null) {
          for (int i = 0; i < DAYS.length; i++) {
            Matcher day =
                Pattern.compile("\\b" + DAYS[i] + "(?:['’]?(?:ye|ya|te|ta|de|da))?\\b").matcher(s);
            if (day.find()) {
              date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(i + 1)));
              removed.add(new int[] {day.start(), day.end()});
              break;
            }
          }
        }
        if (date == null && time != null) date = now.toLocalDate();
        if (date != null && time != null) {
          LocalDateTime local = LocalDateTime.of(date, time);
          if (now.getZone().getRules().getValidOffsets(local).isEmpty())
            issue = "Bu saat seçili saat diliminde yok. Başka bir saat seç.";
          else due = ZonedDateTime.of(local, now.getZone());
        }
      } catch (DateTimeException e) {
        issue = "Tarih geçersiz. Takvimden geçerli gün ve saati seç.";
        due = null;
      }
    }
    removed.sort((a, b) -> Integer.compare(b[0], a[0]));
    for (int[] r : removed) if (r[1] <= body.length()) body.replace(r[0], r[1], " ");
    String title =
        body.toString()
            .replaceAll(
                "(?iu)\\b(?:bana|lütfen|lutfen|sesli olarak|sesli|sesle|olarak|bir hatırlatma|bir"
                    + " hatirlatma)\\b",
                " ")
            .replaceAll(
                "(?iu)\\bhat[ıi]rlat(?:ır mısın|ir misin|manı istiyorum|mani istiyorum|ma"
                    + " oluştur|ma olustur|ma ekle|ma kur|ma ayarla|ıver|iver|ır|ir)?\\b",
                " ")
            .replaceAll("^[\\s,:'’]+|[\\s,;.!?]+$", "")
            .replaceAll("\\s+", " ")
            .trim();
    if (due != null && !due.toInstant().isAfter(now.toInstant())) {
      issue = "Belirtilen zaman geçmişte. Tarih ve saati tekrar seç.";
      due = null;
    }
    if (due == null && issue.isEmpty())
      issue = "Tarih veya saat net değil; kaydetmeden önce ikisini de seç.";
    return new ReminderDraft(Prompt.cut(title, 200), due, audible, issue);
  }

  static void mark(String text, String value, List<int[]> spans) {
    int i = text.indexOf(value);
    if (i >= 0) spans.add(new int[] {i, i + value.length()});
  }
}
