package com.semih.havapusulasi;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

final class WeatherStore extends SQLiteOpenHelper {
  WeatherStore(Context c) {
    this(c, "weather.db");
  }

  WeatherStore(Context c, String name) {
    super(c, name, null, 1);
    setWriteAheadLoggingEnabled(true);
  }

  public void onConfigure(SQLiteDatabase db) {
    db.setForeignKeyConstraintsEnabled(true);
  }

  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE cities(id INTEGER PRIMARY KEY,name TEXT NOT NULL,region TEXT NOT NULL,country"
            + " TEXT NOT NULL,lat REAL NOT NULL,lon REAL NOT NULL,zone TEXT NOT"
            + " NULL,UNIQUE(lat,lon))");
    db.execSQL(
        "CREATE TABLE forecasts(city_id INTEGER PRIMARY KEY REFERENCES cities(id) ON DELETE"
            + " CASCADE,raw TEXT NOT NULL,downloaded INTEGER NOT NULL)");
  }

  public void onUpgrade(SQLiteDatabase db, int old, int next) {
    throw new IllegalStateException("Desteklenmeyen şema geçişi");
  }

  List<City> cities() {
    List<City> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT id,name,region,country,lat,lon,zone FROM cities ORDER BY id LIMIT 12",
                null)) {
      while (c.moveToNext())
        out.add(
            new City(
                c.getLong(0),
                c.getString(1),
                c.getString(2),
                c.getString(3),
                c.getDouble(4),
                c.getDouble(5),
                c.getString(6)));
    }
    return out;
  }

  long add(City c) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      try (Cursor old =
          db.rawQuery(
              "SELECT id FROM cities WHERE lat=? AND lon=?",
              new String[] {"" + c.latitude, "" + c.longitude})) {
        if (old.moveToFirst()) {
          long id = old.getLong(0);
          db.setTransactionSuccessful();
          return id;
        }
      }
      if (android.database.DatabaseUtils.longForQuery(db, "SELECT count(*) FROM cities", null)
          >= 12) throw new IllegalStateException("En fazla 12 şehir eklenebilir.");
      ContentValues v = new ContentValues();
      v.put("name", c.name);
      v.put("region", c.region);
      v.put("country", c.country);
      v.put("lat", c.latitude);
      v.put("lon", c.longitude);
      v.put("zone", c.zone);
      long id = db.insertOrThrow("cities", null, v);
      db.setTransactionSuccessful();
      return id;
    } finally {
      db.endTransaction();
    }
  }

  void save(long id, String raw, long downloaded) throws Exception {
    WeatherApi.parse(raw, downloaded); // Reject invalid data before replacing last good forecast.
    ContentValues v = new ContentValues();
    v.put("city_id", id);
    v.put("raw", raw);
    v.put("downloaded", downloaded);
    if (getWritableDatabase()
            .insertWithOnConflict("forecasts", null, v, SQLiteDatabase.CONFLICT_REPLACE)
        < 0) throw new java.io.IOException("Şehir artık kayıtlı değil; tahmin kaydedilmedi.");
  }

  Forecast cached(long id) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT raw,downloaded FROM forecasts WHERE city_id=?", new String[] {"" + id})) {
      if (!c.moveToFirst()) return null;
      try {
        return WeatherApi.parse(c.getString(0), c.getLong(1));
      } catch (Exception invalid) {
        return null;
      }
    }
  }

  void remove(long id) {
    getWritableDatabase().delete("cities", "id=?", new String[] {"" + id});
  }
}
