package com.semih.cepbutcem;

import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Year/month keys match App.jsx, including zero-based inflation months. */
public final class SalarySettings {
  public String current = "", year = "", firstYear = "";
  public boolean july = false;
  public final Map<String, String> raises = new TreeMap<>(), inflation = new TreeMap<>();

  public static int year(String input) {
    int y = input.trim().isEmpty() ? LocalDate.now().getYear() : Integer.parseInt(input.trim());
    if (y < 2000 || y > 2099) throw new IllegalArgumentException("Yıl 2000–2099 olmalı.");
    return y;
  }

  public JSONObject json() throws JSONException {
    return new JSONObject()
        .put("current", current)
        .put("year", year)
        .put("firstYear", firstYear)
        .put("july", july)
        .put("raises", new JSONObject(raises))
        .put("inflation", new JSONObject(inflation));
  }

  public static SalarySettings parse(JSONObject o) throws JSONException {
    SalarySettings s = new SalarySettings();
    s.current = string(o, "current");
    s.year = string(o, "year");
    s.firstYear = string(o, "firstYear");
    Object july = o.get("july");
    if (!(july instanceof Boolean)) throw new JSONException("Geçersiz maaş dönemi.");
    s.july = (Boolean) july;
    try {
      year(s.year);
      year(s.firstYear);
      if (!s.current.isEmpty() && Finance.parseMoney(s.current) < 0)
        throw new IllegalArgumentException("Maaş negatif olamaz.");
      read(o.getJSONObject("raises"), s.raises, true);
      read(o.getJSONObject("inflation"), s.inflation, false);
    } catch (IllegalArgumentException e) {
      throw new JSONException(e.getMessage());
    }
    return s;
  }

  private static String string(JSONObject o, String key) throws JSONException {
    Object value = o.get(key);
    if (!(value instanceof String) || ((String) value).length() > 32)
      throw new JSONException("Geçersiz maaş ayarı.");
    return (String) value;
  }

  private static void read(JSONObject o, Map<String, String> to, boolean raises)
      throws JSONException {
    if (o.length() > 1212) throw new JSONException("Maaş tablosu çok büyük.");
    Iterator<String> keys = o.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (!key.matches(
          raises ? "(20[0-9]{2}|2100)-(ocak|temmuz)" : "(20[0-9]{2}|2100)-([0-9]|1[01])"))
        throw new JSONException("Geçersiz yıl/ay anahtarı.");
      String value = string(o, key);
      Calculators.salaryPercent(value);
      to.put(key, value);
    }
  }
}
