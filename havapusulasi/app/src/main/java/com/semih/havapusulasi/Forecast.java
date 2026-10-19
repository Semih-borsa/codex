package com.semih.havapusulasi;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class Forecast {
  static final Locale TR = new Locale("tr", "TR");
  static final long STALE = 6 * 3600_000L;
  final String zone;
  final long downloaded;
  final List<Hour> hours;
  final List<Day> days;

  Forecast(String zone, long downloaded, List<Hour> hours, List<Day> days) {
    ZoneId.of(zone);
    this.zone = zone;
    this.downloaded = downloaded;
    this.hours = Collections.unmodifiableList(new ArrayList<>(hours));
    this.days = Collections.unmodifiableList(new ArrayList<>(days));
  }

  static final class Hour {
    final long at;
    final Double temperature, feels, rain, wind, humidity;
    final Integer code;

    Hour(long at, Double t, Double f, Double r, Double w, Double h, Integer c) {
      this.at = at;
      temperature = t;
      feels = f;
      rain = r;
      wind = w;
      humidity = h;
      code = c;
    }
  }

  static final class Day {
    final LocalDate date;
    final Double low, high;
    final Integer code;

    Day(LocalDate d, Double l, Double h, Integer c) {
      date = d;
      low = l;
      high = h;
      code = c;
    }
  }

  List<Hour> upcoming(long now, int count) {
    List<Hour> out = new ArrayList<>();
    long hourStart =
        Instant.ofEpochMilli(now)
            .atZone(ZoneId.of(zone))
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli();
    for (Hour h : hours)
      if (h.at >= hourStart && h.at >= now - 3600_000L) {
        out.add(h);
        if (out.size() == count) break;
      }
    return out;
  }

  Hour current(long now) {
    for (Hour h : hours) if (h.at <= now && now - h.at < 3600_000L) return h;
    return null;
  }

  boolean stale(long now) {
    return now < downloaded || now - downloaded > STALE;
  }

  String status(long now) {
    if (upcoming(now, 1).isEmpty()) return "Tahmin süresi doldu • İnternete bağlanıp yenile";
    if (now < downloaded) return "Cihaz saati değişmiş • Kaydı yenile";
    long minutes = (now - downloaded) / 60_000;
    return (stale(now) ? "Eski kayıt • " : "Kaydedilmiş tahmin • ")
        + (minutes < 1
            ? "az önce"
            : minutes < 60
                ? minutes + " dk önce"
                : minutes < 1440 ? minutes / 60 + " sa önce" : minutes / 1440 + " gün önce");
  }

  String time(long at) {
    return Instant.ofEpochMilli(at)
        .atZone(ZoneId.of(zone))
        .format(DateTimeFormatter.ofPattern("d MMM HH:mm", TR));
  }

  static String value(Double x, String suffix) {
    return x == null ? "—" : String.format(TR, "%.0f", x) + suffix;
  }

  static String description(Integer code) {
    if (code == null) return "Veri yok";
    switch (code) {
      case 0:
        return "Açık";
      case 1:
      case 2:
        return "Parçalı bulutlu";
      case 3:
        return "Kapalı";
      case 45:
      case 48:
        return "Sisli";
      case 51:
      case 53:
      case 55:
        return "Çisenti";
      case 56:
      case 57:
        return "Donan çisenti";
      case 61:
      case 63:
      case 65:
        return "Yağmur";
      case 66:
      case 67:
        return "Donan yağmur";
      case 71:
      case 73:
      case 75:
      case 77:
        return "Kar";
      case 80:
      case 81:
      case 82:
        return "Sağanak";
      case 85:
      case 86:
        return "Kar sağanağı";
      case 95:
      case 96:
      case 99:
        return "Gök gürültülü";
      default:
        return "Kod " + code;
    }
  }

  static String icon(Integer c) {
    if (c == null) return "◇";
    if (c == 0) return "☀";
    if (c == 1 || c == 2) return "⛅";
    if (c >= 95) return "ϟ";
    if (c >= 71 && c <= 77 || c == 85 || c == 86) return "❄";
    if (c >= 51) return "☂";
    return "☁";
  }

  static String advice(Hour h) {
    if (h == null) return "Güncel saate ait tahmin yok. Yenilemeden günlük öneri çıkarılmadı.";
    List<String> lines = new ArrayList<>();
    if (h.code != null && h.code >= 95)
      lines.add("Fırtına tahmini var; dışarı çıkmadan resmî uyarıları kontrol et.");
    if (h.rain != null && h.rain >= 50) lines.add("Şemsiye iyi bir fikir; yağış olasılığı yüksek.");
    if (h.wind != null && h.wind >= 40)
      lines.add("Rüzgâr kuvvetli olabilir; açık alan planını kontrol et.");
    if (h.temperature != null && h.temperature <= 5)
      lines.add("Katmanlı ve sıcak tutan kıyafet seçebilirsin.");
    if (h.temperature != null && h.temperature >= 32)
      lines.add("Sıcakta gölge ve su molalarını planla.");
    if (lines.isEmpty())
      return "Yağış ve rüzgâr değerlerini saatlik listeden kontrol ederek plan yapabilirsin.";
    return String.join(" ", lines);
  }
}
