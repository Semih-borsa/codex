package com.semih.nova;

import java.util.*;

final class AnswerQuality {
  static boolean loop(String text) {
    String[] words = WeatherQuery.normalize(text).split("[^\\p{L}\\p{N}]+");
    if (words.length < 24) return false;
    Map<String, Integer> counts = new HashMap<>();
    for (int i = 0; i + 4 <= words.length; i++) {
      String key = String.join(" ", Arrays.copyOfRange(words, i, i + 4));
      int n = counts.getOrDefault(key, 0) + 1;
      counts.put(key, n);
      if (n >= 4) return true;
    }
    return false;
  }

  static boolean retry(String answer, List<Store.Row> history, String question) {
    if (answer.trim().isEmpty() || loop(answer)) return true;
    String previousQuestion = "", previousAnswer = "";
    for (int i = history.size() - 1; i >= 0; i--) {
      Store.Row r = history.get(i);
      if (previousAnswer.isEmpty() && r.role.equals("assistant")) previousAnswer = r.body;
      if (r.role.equals("user")) {
        previousQuestion = r.body;
        break;
      }
    }
    String a = WeatherQuery.normalize(answer).replaceAll("\\s+", " ").trim();
    if (!WeatherQuery.normalize(question).equals(WeatherQuery.normalize(previousQuestion))
        && a.length() > 35
        && a.equals(WeatherQuery.normalize(previousAnswer).replaceAll("\\s+", " ").trim()))
      return true;
    String q = WeatherQuery.normalize(question);
    return a.contains("kisisel asistan")
        && !q.matches("(?s).*\\b(kimsin|merhaba|selam|adin|kendini|asistan)\\b.*");
  }

  static String currentInfo(String q) {
    String s = WeatherQuery.normalize(q);
    if (s.matches("(?s).*\\b(dolar|euro|altin|borsa)\\b.*")
        && s.matches("(?s).*\\b(kac|fiyat|fiyati)\\b.*"))
      return "Canlı fiyat bağlantım yok. Güncel fiyatı uydurmayacağım; güvenilir bir piyasa"
          + " kaynağından kontrol etmelisin.";
    if (s.matches("(?s).*\\b(guncel|anlik|bugunku|son dakika)\\b.*")
        && (s.contains("haber") || s.contains("dolar") || s.contains("kur") || s.contains("borsa")))
      return "Bu konuda doğrulanmış güncel veri bağlantım yok. Canlı fiyat veya haber"
          + " uydurmayacağım. Genel kavramları açıklayabilirim; güncel değeri güvenilir"
          + " kaynaktan kontrol etmelisin.";
    return null;
  }
}
