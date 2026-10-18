package com.semih.nova;

import java.util.regex.*;

final class MessageDraft {
  String recipient = "", hint = "", body = "";
  boolean whatsapp = true;

  static boolean matches(String q) {
    String s = WeatherQuery.normalize(q);
    return !s.contains("nedir")
        && !s.contains("ne demek")
        && (s.matches("(?s).*\\bmesaj(?:i|ini)?\\b.*\\b(at|gonder|yaz|hazirla)\\b.*")
            || s.matches("(?s).*\\b(whatsapp|sms)\\b.*\\b(gonder|yaz|hazirla|at)\\b.*"));
  }

  static MessageDraft parse(String q) {
    MessageDraft d = new MessageDraft();
    String s = WeatherQuery.normalize(q);
    d.whatsapp = !s.matches("(?s).*\\bsms\\b.*");
    Matcher phone = Pattern.compile("(?<!\\d)(\\+?[0-9][0-9 ()-]{8,}[0-9])(?!\\d)").matcher(q);
    if (phone.find()) d.recipient = phone.group(1).trim();
    int colon = q.indexOf(':');
    if (colon >= 0) d.body = q.substring(colon + 1).trim();
    else {
      Matcher quote = Pattern.compile("[\"“]([^\"”]+)[\"”]").matcher(q);
      if (quote.find()) d.body = quote.group(1).trim();
    }
    if (d.recipient.isEmpty()) {
      Matcher person =
          Pattern.compile(
                  "^(.{1,60}?)(?:['’](?:ya|ye|a|e)|me|ma)\\s",
                  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
              .matcher(q);
      if (person.find()) d.hint = person.group(1).trim();
    }
    return d;
  }

  static String phone(String input, boolean international) {
    String p = input.replaceAll("[\\s()\\-]", "");
    if (!p.matches("\\+?[0-9]{8,15}"))
      throw new IllegalArgumentException(
          "Alıcı için geçerli telefon numarası seç veya yaz. Kişi adı tek başına gönderim adresi"
              + " değildir.");
    if (international) {
      if (p.startsWith("+")) p = p.substring(1);
      else if (p.matches("05[0-9]{9}")) p = "90" + p.substring(1);
      else if (p.matches("5[0-9]{9}")) p = "90" + p;
      else if (!p.startsWith("90"))
        throw new IllegalArgumentException("WhatsApp numarasını ülke koduyla yaz (örneğin +90…).");
    }
    return p;
  }

  String preview() {
    String target = phone(recipient, whatsapp);
    if (body.trim().isEmpty()) throw new IllegalArgumentException("Mesaj metni boş olamaz.");
    if (body.length() > 2000)
      throw new IllegalArgumentException("Mesaj en fazla 2.000 karakter olmalı.");
    return (whatsapp ? "WhatsApp" : "SMS")
        + "\nAlıcı: "
        + target
        + "\n\n"
        + body
        + "\n\n"
        + "Yalnızca taslak ekranı açılacak. Son Gönder işlemini ilgili uygulamada sen yapacaksın.";
  }
}
