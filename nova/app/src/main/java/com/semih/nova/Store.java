package com.semih.nova;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

final class Store extends SQLiteOpenHelper {
  Store(Context c) {
    this(c, "nova.db");
  }

  Store(Context c, String name) {
    super(c, name, null, 1);
  }

  public void onCreate(SQLiteDatabase d) {
    d.execSQL(
        "CREATE TABLE messages(id INTEGER PRIMARY KEY,role TEXT NOT NULL,body TEXT NOT NULL,created"
            + " INTEGER NOT NULL)");
    d.execSQL(
        "CREATE TABLE memories(id INTEGER PRIMARY KEY,title TEXT NOT NULL,body TEXT NOT NULL)");
    d.execSQL(
        "CREATE TABLE tasks(id INTEGER PRIMARY KEY,title TEXT NOT NULL,due INTEGER NOT NULL,done"
            + " INTEGER NOT NULL DEFAULT 0,notified INTEGER NOT NULL DEFAULT 0)");
    d.execSQL("CREATE INDEX task_due ON tasks(done,due)");
  }

  public void onUpgrade(SQLiteDatabase d, int old, int next) {
    throw new IllegalStateException("Desteklenmeyen veritabanı sürümü");
  }

  static final class Row {
    long id, time;
    String title, body, role;
    boolean done;
  }

  List<Row> messages() {
    List<Row> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT id,role,body,created FROM (SELECT * FROM messages ORDER BY id DESC LIMIT"
                    + " 60) ORDER BY id",
                null)) {
      while (c.moveToNext()) {
        Row r = new Row();
        r.id = c.getLong(0);
        r.role = c.getString(1);
        r.body = c.getString(2);
        r.time = c.getLong(3);
        out.add(r);
      }
    }
    return out;
  }

  long message(String role, String body) {
    ContentValues v = new ContentValues();
    v.put("role", role);
    v.put("body", body);
    v.put("created", System.currentTimeMillis());
    return getWritableDatabase().insertOrThrow("messages", null, v);
  }

  List<Row> memories() {
    List<Row> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT id,title,body FROM memories ORDER BY id DESC LIMIT 100", null)) {
      while (c.moveToNext()) {
        Row r = new Row();
        r.id = c.getLong(0);
        r.title = c.getString(1);
        r.body = c.getString(2);
        out.add(r);
      }
    }
    return out;
  }

  long saveMemory(long id, String title, String body) {
    ContentValues v = new ContentValues();
    v.put("title", title);
    v.put("body", body);
    if (id > 0) {
      getWritableDatabase().update("memories", v, "id=?", new String[] {"" + id});
      return id;
    }
    return getWritableDatabase().insertOrThrow("memories", null, v);
  }

  List<Row> tasks() {
    List<Row> out = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT id,title,due,done FROM tasks ORDER BY done,due LIMIT 100", null)) {
      while (c.moveToNext()) {
        Row r = new Row();
        r.id = c.getLong(0);
        r.title = c.getString(1);
        r.time = c.getLong(2);
        r.done = c.getInt(3) != 0;
        out.add(r);
      }
    }
    return out;
  }

  Row task(long id) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery("SELECT id,title,due,done FROM tasks WHERE id=?", new String[] {"" + id})) {
      if (!c.moveToFirst()) return null;
      Row r = new Row();
      r.id = c.getLong(0);
      r.title = c.getString(1);
      r.time = c.getLong(2);
      r.done = c.getInt(3) != 0;
      return r;
    }
  }

  long saveTask(long id, String title, long due) {
    ContentValues v = new ContentValues();
    v.put("title", title);
    v.put("due", due);
    v.put("notified", 0);
    if (id > 0) {
      getWritableDatabase().update("tasks", v, "id=?", new String[] {"" + id});
      return id;
    }
    return getWritableDatabase().insertOrThrow("tasks", null, v);
  }

  void done(long id, boolean done) {
    ContentValues v = new ContentValues();
    v.put("done", done ? 1 : 0);
    getWritableDatabase().update("tasks", v, "id=?", new String[] {"" + id});
  }

  void removeMemory(long id) {
    getWritableDatabase().delete("memories", "id=?", new String[] {"" + id});
  }

  void removeTask(long id) {
    getWritableDatabase().delete("tasks", "id=?", new String[] {"" + id});
  }

  void clearChat() {
    getWritableDatabase().delete("messages", null, null);
  }
}
