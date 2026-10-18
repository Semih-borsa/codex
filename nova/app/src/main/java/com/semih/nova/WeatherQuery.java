package com.semih.nova;

import java.text.Normalizer;
import java.util.*;

final class WeatherQuery {
  final String city;
  final int day;

  WeatherQuery(String city, int day) {
    this.city = city;
    this.day = day;
  }

  static String normalize(String s) {
    return Normalizer.normalize(s.toLowerCase(new Locale("tr")), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .replace('ı', 'i');
  }

  static boolean matches(String q) {
    return normalize(q)
        .matches("(?s).*\\b(hava|havasi|havalar|sicaklik|yagmur|derece|ruzgar)\\b.*");
  }

  static int day(String q) {
    String s = normalize(q);
    return s.contains("obur gun") || s.contains("iki gun sonra") ? 2 : s.contains("yarin") ? 1 : 0;
  }

  static boolean followup(String q) {
    String s = normalize(q);
    return s.length() < 75
        && (s.contains("yarin") || s.contains("bugun") || s.contains("obur gun"));
  }

  static WeatherQuery parse(String q, String fallback) {
    String s = normalize(q);
    int offset = day(q);
    Set<String> stop =
        new HashSet<>(
            Arrays.asList(
                "hava",
                "havasi",
                "havalar",
                "nin",
                "nun",
                "in",
                "un",
                "durumu",
                "nasil",
                "olacak",
                "olur",
                "bugun",
                "yarin",
                "obur",
                "gun",
                "iki",
                "sonra",
                "sicaklik",
                "kac",
                "derece",
                "yagmur",
                "var",
                "mi",
                "mu",
                "yagacak",
                "ruzgar",
                "icin",
                "lutfen",
                "bana",
                "soyle",
                "bak",
                "nedir",
                "goster",
                "peki",
                "da",
                "de",
                "ta",
                "te",
                "bir",
                "kontrol",
                "eder",
                "misin",
                "o",
                "da",
                "ne",
                "bakar",
                "hakkinda",
                "bilgi",
                "ver"));
    List<String> city = new ArrayList<>();
    for (String word : s.split("[^\\p{L}\\p{N}]+"))
      if (!word.isEmpty() && !stop.contains(word)) city.add(word);
    String selected = String.join(" ", city).trim();
    if (selected.length() > 60) selected = "";
    return new WeatherQuery(selected.isEmpty() ? fallback : selected, offset);
  }
}
