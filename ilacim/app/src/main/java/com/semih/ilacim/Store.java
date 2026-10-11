package com.semih.ilacim;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.UUID;
import org.json.*;

final class Store extends SQLiteOpenHelper {
  JSONObject data;

  Store(Context c) {
    super(c, "ilacim.db", null, 1);
    try (Cursor cur = getReadableDatabase().rawQuery("SELECT body FROM state WHERE id=1", null)) {
      data = cur.moveToFirst() ? new JSONObject(cur.getString(0)) : empty();
    } catch (JSONException e) {
      throw new IllegalStateException("Kayıtlar okunamadı; uygulama verilerini silme.", e);
    }
  }

  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE state(id INTEGER PRIMARY KEY CHECK(id=1),body TEXT NOT NULL)");
  }

  public void onUpgrade(SQLiteDatabase db, int old, int now) {
    throw new IllegalStateException("Desteklenmeyen sürüm");
  }

  static JSONObject empty() throws JSONException {
    return new JSONObject()
        .put("version", 1)
        .put("meds", new JSONArray())
        .put("plans", new JSONArray())
        .put("appointments", new JSONArray())
        .put("logs", new JSONArray());
  }

  JSONArray list(String key) {
    return data.optJSONArray(key);
  }

  JSONObject find(String key, String id) {
    JSONArray a = list(key);
    for (int i = 0; i < a.length(); i++)
      if (a.optJSONObject(i).optString("id").equals(id)) return a.optJSONObject(i);
    return null;
  }

  void remove(String key, String id) {
    JSONArray a = list(key);
    for (int i = a.length() - 1; i >= 0; i--)
      if (a.optJSONObject(i).optString("id").equals(id)) a.remove(i);
  }

  void save() {
    ContentValues v = new ContentValues();
    v.put("id", 1);
    v.put("body", data.toString());
    if (getWritableDatabase()
            .insertWithOnConflict("state", null, v, SQLiteDatabase.CONFLICT_REPLACE)
        < 0) throw new IllegalStateException("Kayıt kaydedilemedi");
  }

  static String id() {
    return UUID.randomUUID().toString();
  }

  static void validate(JSONObject d) throws JSONException {
    if (d.getInt("version") != 1)
      throw new IllegalArgumentException("Yedek sürümü desteklenmiyor.");
    java.util.Set<String> ids = new java.util.HashSet<>(),
        planIds = new java.util.HashSet<>(),
        slots = new java.util.HashSet<>();
    for (String key : new String[] {"meds", "plans", "appointments", "logs"})
      if (d.getJSONArray(key).length() > 10000)
        throw new IllegalArgumentException("Yedek çok büyük.");
    JSONArray meds = d.getJSONArray("meds");
    for (int i = 0; i < meds.length(); i++) {
      JSONObject m = meds.getJSONObject(i);
      if (m.getString("id").isBlank()
          || !ids.add(m.getString("id"))
          || m.getString("name").isBlank()
          || m.getInt("stock") < 0
          || m.getInt("low") < 0) throw new IllegalArgumentException("İlaç kaydı geçersiz.");
      Rules.expiry(m.getString("expiry"));
    }
    JSONArray plans = d.getJSONArray("plans");
    for (int i = 0; i < plans.length(); i++) {
      JSONObject p = plans.getJSONObject(i);
      if (!planIds.add(p.getString("id"))
          || !ids.contains(p.getString("med"))
          || p.getInt("dose") < 1) throw new IllegalArgumentException("İlaç planı geçersiz.");
      Rules.times(p.getString("times"));
      if (java.time.LocalDate.parse(p.getString("end"))
          .isBefore(java.time.LocalDate.parse(p.getString("start"))))
        throw new IllegalArgumentException("Plan tarihleri geçersiz.");
    }
    JSONArray appointments = d.getJSONArray("appointments");
    java.util.Set<String> appointmentsIds = new java.util.HashSet<>();
    for (int i = 0; i < appointments.length(); i++) {
      JSONObject a = appointments.getJSONObject(i);
      if (!appointmentsIds.add(a.getString("id")) || a.getString("doctor").isBlank())
        throw new IllegalArgumentException("Randevu geçersiz.");
      java.time.LocalDateTime.parse(a.getString("when"));
    }
    JSONArray logs = d.getJSONArray("logs");
    for (int i = 0; i < logs.length(); i++) {
      JSONObject l = logs.getJSONObject(i);
      if (!slots.add(l.getString("id"))
          || !ids.contains(l.getString("med"))
          || l.getInt("amount") < 0
          || !java.util.List.of("taken", "skipped").contains(l.getString("status")))
        throw new IllegalArgumentException("Geçmiş kaydı geçersiz.");
      java.time.LocalDate.parse(l.getString("date"));
    }
  }
}
