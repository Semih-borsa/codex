package com.semih.nova;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.*;
import org.json.*;

final class Weather {
  interface Fetcher {
    String get(String url) throws Exception;
  }

  static final class Result {
    final String city, answer;

    Result(String c, String a) {
      city = c;
      answer = a;
    }
  }

  static String fetch(String url) throws Exception {
    HttpURLConnection c = Models.connection(url);
    try (InputStream in = c.getInputStream()) {
      return new String(Documents.bounded(in, 1024 * 1024), StandardCharsets.UTF_8);
    } finally {
      c.disconnect();
    }
  }

  static Result get(WeatherQuery q) throws Exception {
    return get(q, Weather::fetch);
  }

  static Result get(WeatherQuery q, Fetcher fetcher) throws Exception {
    if (q.city.isEmpty())
      throw new IOException("Hangi şehir için hava durumunu öğrenmek istiyorsun?");
    JSONObject geo =
        new JSONObject(
            fetcher.get(
                "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(q.city, "UTF-8")
                    + "&count=5&language=tr&format=json"));
    JSONArray places = geo.optJSONArray("results");
    if (places == null || places.length() == 0)
      throw new IOException("Bu şehri bulamadım. Şehir ve ülke adını daha açık yazabilir misin?");
    JSONObject place = places.getJSONObject(0);
    for (int i = 0; i < places.length(); i++)
      if ("TR".equals(places.getJSONObject(i).optString("country_code"))) {
        place = places.getJSONObject(i);
        break;
      }
    double lat = place.getDouble("latitude"), lon = place.getDouble("longitude");
    String url =
        "https://api.open-meteo.com/v1/forecast?latitude="
            + lat
            + "&longitude="
            + lon
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code&timezone=auto&forecast_days=7";
    JSONObject data = new JSONObject(fetcher.get(url));
    JSONObject daily = data.getJSONObject("daily");
    JSONArray dates = daily.getJSONArray("time");
    String zone = data.getString("timezone");
    LocalDate target = LocalDate.now(ZoneId.of(zone)).plusDays(q.day);
    int index = -1;
    for (int i = 0; i < dates.length(); i++)
      if (dates.getString(i).equals(target.toString())) {
        index = i;
        break;
      }
    if (index < 0) throw new IOException("İstenen güne ait güncel tahmin bulunamadı.");
    double high = value(daily, "temperature_2m_max", index),
        low = value(daily, "temperature_2m_min", index);
    int code = (int) value(daily, "weather_code", index);
    String rain =
        daily.getJSONArray("precipitation_probability_max").isNull(index)
            ? "Yağış olasılığı belirtilmemiş."
            : "Yağış olasılığı yüzde "
                + Math.round(value(daily, "precipitation_probability_max", index))
                + ".";
    String city = place.getString("name");
    String region = place.optString("admin1");
    String country = place.optString("country", place.optString("country_code"));
    String location =
        city + (region.isEmpty() || region.equals(city) ? "" : ", " + region) + ", " + country;
    String when = target.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", new Locale("tr")));
    String answer =
        location
            + " için "
            + when
            + ": "
            + description(code)
            + ". En düşük "
            + degrees(low)
            + ", en yüksek "
            + degrees(high)
            + " derece. "
            + rain
            + "\n\nKaynak: Open-Meteo (https://open-meteo.com/), şehir: GeoNames. Sorgu: "
            + ZonedDateTime.now(ZoneId.of(zone))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            + " ("
            + zone
            + "). Tahminler değişebilir.";
    return new Result(city, answer);
  }

  static double value(JSONObject d, String field, int i) throws Exception {
    JSONArray values = d.getJSONArray(field);
    if (values.isNull(i)) throw new IOException("Hava tahmini eksik; değer uydurmayacağım.");
    double v = values.getDouble(i);
    if (!Double.isFinite(v)) throw new IOException("Geçersiz hava verisi.");
    return v;
  }

  static String degrees(double v) {
    return String.format(new Locale("tr"), "%.0f", v);
  }

  static String description(int code) {
    switch (code) {
      case 0:
        return "açık";
      case 1:
      case 2:
        return "parçalı bulutlu";
      case 3:
        return "kapalı";
      case 45:
      case 48:
        return "sisli";
      case 51:
      case 53:
      case 55:
        return "çisenti";
      case 56:
      case 57:
        return "donan çisenti";
      case 61:
      case 63:
      case 65:
        return "yağmurlu";
      case 66:
      case 67:
        return "donan yağmur";
      case 71:
      case 73:
      case 75:
      case 77:
        return "kar yağışlı";
      case 80:
      case 81:
      case 82:
        return "sağanak yağışlı";
      case 85:
      case 86:
        return "kar sağanaklı";
      case 95:
      case 96:
      case 99:
        return "gök gürültülü fırtına";
      default:
        return "hava durumu kodu " + code;
    }
  }
}
