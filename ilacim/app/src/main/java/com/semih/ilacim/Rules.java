package com.semih.ilacim;

import java.time.*;
import java.time.format.*;
import java.util.*;

public final class Rules {
  private Rules() {}

  public static int quantity(String text) {
    if (!text.matches("[0-9]+([.,][0-9]{1,3})?"))
      throw new IllegalArgumentException("Geçersiz miktar");
    java.math.BigDecimal n = new java.math.BigDecimal(text.replace(',', '.'));
    if (n.compareTo(java.math.BigDecimal.valueOf(1000000)) > 0)
      throw new IllegalArgumentException("Miktar çok büyük");
    return n.movePointRight(3).intValueExact();
  }

  public static YearMonth expiry(String text) {
    if (!text.matches("(0[1-9]|1[0-2])/[0-9]{4}"))
      throw new IllegalArgumentException("Son kullanma tarihini AA/YYYY biçiminde gir: 12/2026");
    YearMonth m =
        YearMonth.parse(
            text, DateTimeFormatter.ofPattern("MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
    if (m.getYear() < 2000 || m.getYear() > 2200)
      throw new IllegalArgumentException("Yılı kontrol et.");
    return m;
  }

  public static boolean expired(String text, LocalDate today) {
    return expiry(text).atEndOfMonth().isBefore(today);
  }

  public static List<String> times(String text) {
    TreeSet<String> result = new TreeSet<>();
    for (String t : text.split(",", -1)) {
      t = t.trim();
      if (!t.matches("([01][0-9]|2[0-3]):[0-5][0-9]"))
        throw new IllegalArgumentException("Saatleri 08:00, 20:00 gibi gir.");
      result.add(t);
    }
    if (result.size() > 12)
      throw new IllegalArgumentException("En fazla 12 günlük saat ekleyebilirsin.");
    return new ArrayList<>(result);
  }

  public static String barcode(String raw) {
    if (raw == null) return "";
    raw = raw.trim();
    // GS1 DataMatrix: AI 01 contains the 14-digit GTIN. Do not treat serials as product codes.
    if (raw.startsWith("]d2")) raw = raw.substring(3);
    if (raw.startsWith("(01)") && raw.length() >= 18 && raw.substring(4, 18).matches("[0-9]{14}"))
      return raw.substring(4, 18);
    if (raw.startsWith("01") && raw.length() > 16 && raw.substring(2, 16).matches("[0-9]{14}"))
      return raw.substring(2, 16);
    if (!raw.matches("[0-9]{8}|[0-9]{12,14}"))
      throw new IllegalArgumentException("EAN/UPC/GTIN barkodu veya ilaç karekodunu kullan.");
    return raw;
  }

  public static boolean validGtin(String code) {
    if (!code.matches("[0-9]{8}|[0-9]{12,14}")) return false;
    int total = 0, weight = 3;
    for (int i = code.length() - 2; i >= 0; i--) {
      total += (code.charAt(i) - '0') * weight;
      weight = 4 - weight;
    }
    return (10 - total % 10) % 10 == code.charAt(code.length() - 1) - '0';
  }

  public static String slot(String plan, LocalDate date, String time) {
    return plan + "|" + date + "|" + time;
  }
}
