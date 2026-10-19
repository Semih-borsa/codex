package com.semih.havapusulasi;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.*;

final class WeatherApi {
  interface Fetcher {
    String get(String url) throws Exception;
  }

  static String fetch(String url) throws Exception {
    URL target = new URL(url);
    if (!"https".equals(target.getProtocol())
        || !("api.open-meteo.com".equals(target.getHost())
            || "geocoding-api.open-meteo.com".equals(target.getHost())))
      throw new IOException("Desteklenmeyen servis");
    HttpURLConnection c = (HttpURLConnection) target.openConnection();
    c.setConnectTimeout(10000);
    c.setReadTimeout(15000);
    c.setInstanceFollowRedirects(false);
    c.setRequestProperty("User-Agent", "HavaPusulasi/1.0 personal-noncommercial");
    try {
      int status = c.getResponseCode();
      if (status != 200)
        throw new IOException(
            status == 429
                ? "Servis istek sınırına ulaşıldı; biraz sonra dene."
                : "Hava servisi yanıtı: " + status);
      try (InputStream in = c.getInputStream();
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] b = new byte[8192];
        int n, total = 0;
        while ((n = in.read(b)) != -1) {
          total += n;
          if (total > 1024 * 1024) throw new IOException("Servis yanıtı fazla büyük");
          out.write(b, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
      }
    } finally {
      c.disconnect();
    }
  }

  static List<City> search(String name, Fetcher f) throws Exception {
    if (name.trim().length() < 2 || name.length() > 100)
      throw new IOException("En az iki harfle şehir adı yaz.");
    JSONObject root =
        new JSONObject(
            f.get(
                "https://geocoding-api.open-meteo.com/v1/search?count=8&language=tr&format=json&name="
                    + URLEncoder.encode(name.trim(), "UTF-8")));
    if (root.optBoolean("error")) throw new IOException("Şehir servisi sorguyu reddetti.");
    JSONArray results = root.optJSONArray("results");
    List<City> out = new ArrayList<>();
    if (results == null) return out;
    for (int i = 0; i < Math.min(8, results.length()); i++) {
      JSONObject c = results.getJSONObject(i);
      try {
        out.add(
            new City(
                0,
                c.getString("name"),
                c.optString("admin1"),
                c.optString("country", c.optString("country_code")),
                c.getDouble("latitude"),
                c.getDouble("longitude"),
                c.getString("timezone")));
      } catch (IllegalArgumentException | JSONException invalid) {
      }
    }
    return out;
  }

  static String forecastUrl(City city) {
    return "https://api.open-meteo.com/v1/forecast?latitude="
        + city.latitude
        + "&longitude="
        + city.longitude
        + "&timezone=auto&timeformat=unixtime&forecast_days=7&temperature_unit=celsius&wind_speed_unit=kmh&precipitation_unit=mm&hourly=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,weather_code";
  }

  static Forecast parse(String raw, long downloaded) throws Exception {
    JSONObject root = new JSONObject(raw);
    if (root.optBoolean("error")) throw new IOException("Servis geçerli tahmin vermedi.");
    String zone = root.getString("timezone");
    ZoneId.of(zone);
    JSONObject hourly = root.getJSONObject("hourly"), units = root.getJSONObject("hourly_units");
    if (!"unixtime".equals(units.optString("time"))
        || !"°C".equals(units.optString("temperature_2m"))
        || !"km/h".equals(units.optString("wind_speed_10m")))
      throw new IOException("Beklenmeyen ölçü birimi");
    JSONArray times = hourly.getJSONArray("time");
    if (times.length() < 1 || times.length() > 200)
      throw new IOException("Saatlik tahmin aralığı geçersiz");
    List<Forecast.Hour> hours = new ArrayList<>();
    long prev = Long.MIN_VALUE;
    for (int i = 0; i < times.length(); i++) {
      long at = Math.multiplyExact(times.getLong(i), 1000);
      if (at <= prev) throw new IOException("Saatlik sıralama geçersiz");
      prev = at;
      hours.add(
          new Forecast.Hour(
              at,
              number(hourly, "temperature_2m", i, -100, 70),
              number(hourly, "apparent_temperature", i, -120, 90),
              number(hourly, "precipitation_probability", i, 0, 100),
              number(hourly, "wind_speed_10m", i, 0, 500),
              number(hourly, "relative_humidity_2m", i, 0, 100),
              code(hourly, i)));
    }
    JSONObject daily = root.getJSONObject("daily");
    JSONArray dates = daily.getJSONArray("time");
    if (dates.length() > 10) throw new IOException("Günlük tahmin aralığı geçersiz");
    List<Forecast.Day> days = new ArrayList<>();
    for (int i = 0; i < dates.length(); i++)
      days.add(
          new Forecast.Day(
              Instant.ofEpochSecond(dates.getLong(i)).atZone(ZoneId.of(zone)).toLocalDate(),
              number(daily, "temperature_2m_min", i, -100, 70),
              number(daily, "temperature_2m_max", i, -100, 70),
              code(daily, i)));
    return new Forecast(zone, downloaded, hours, days);
  }

  private static Integer code(JSONObject o, int i) throws Exception {
    Double c = number(o, "weather_code", i, 0, 99);
    return c == null ? null : c.intValue();
  }

  private static Double number(JSONObject o, String name, int i, double low, double high)
      throws Exception {
    JSONArray a = o.getJSONArray(name);
    if (i >= a.length()) throw new IOException("Eksik veri dizisi: " + name);
    if (a.isNull(i)) return null;
    double v = a.getDouble(i);
    if (!Double.isFinite(v) || v < low || v > high)
      throw new IOException("Geçersiz değer: " + name);
    return v;
  }
}
