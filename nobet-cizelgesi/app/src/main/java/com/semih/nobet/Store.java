package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.json.*;

final class Store {
  private final AtomicFile file;

  Store(Context context) {
    file = new AtomicFile(new File(context.getFilesDir(), "roster-v1.json"));
  }

  State load() throws Exception {
    if (!file.getBaseFile().exists()) return new State();
    return decode(new String(file.readFully(), StandardCharsets.UTF_8));
  }

  void save(State s) throws Exception {
    byte[] data = encode(s).getBytes(StandardCharsets.UTF_8);
    FileOutputStream out = null;
    try {
      out = file.startWrite();
      out.write(data);
      file.finishWrite(out);
    } catch (Exception e) {
      if (out != null) file.failWrite(out);
      throw e;
    }
  }

  static String encode(State s) throws JSONException {
    JSONObject root = new JSONObject().put("version", 1);
    JSONArray people = new JSONArray(), leaves = new JSONArray(), weeks = new JSONArray();
    for (Person p : s.people)
      people.put(new JSONObject().put("id", p.id).put("name", p.name).put("active", p.active));
    for (Leave l : s.leaves)
      leaves.put(
          new JSONObject()
              .put("person", l.person)
              .put("place", l.place)
              .put("start", l.start)
              .put("end", l.end)
              .put("reason", l.reason));
    for (Week w : s.weeks) {
      JSONArray duties = new JSONArray(), debts = new JSONArray();
      for (Duty d : w.duties)
        duties.put(
            new JSONObject().put("date", d.date).put("place", d.place).put("person", d.person));
      for (Debt d : w.debts) debts.put(new JSONObject().put("person", d.person).put("end", d.end));
      weeks.put(
          new JSONObject()
              .put("monday", w.monday)
              .put("cursor", w.cursor)
              .put("duties", duties)
              .put("debts", debts));
    }
    return root.put("people", people).put("leaves", leaves).put("weeks", weeks).toString();
  }

  static State decode(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    if (root.getInt("version") != 1) throw new JSONException("Desteklenmeyen veri sürümü");
    State s = new State();
    JSONArray people = root.getJSONArray("people"),
        leaves = root.getJSONArray("leaves"),
        weeks = root.getJSONArray("weeks");
    for (int i = 0; i < people.length(); i++) {
      JSONObject j = people.getJSONObject(i);
      Person p = new Person(j.getString("id"), j.getString("name"));
      p.active = j.getBoolean("active");
      s.people.add(p);
    }
    for (int i = 0; i < leaves.length(); i++) {
      JSONObject j = leaves.getJSONObject(i);
      s.leaves.add(
          new Leave(
              j.getString("person"),
              j.getString("place"),
              LocalDate.parse(j.getString("start")),
              LocalDate.parse(j.getString("end")),
              j.getString("reason")));
    }
    for (int i = 0; i < weeks.length(); i++) {
      JSONObject j = weeks.getJSONObject(i);
      Week w = new Week(LocalDate.parse(j.getString("monday")));
      w.cursor = j.getString("cursor");
      JSONArray ds = j.getJSONArray("duties"), bs = j.getJSONArray("debts");
      for (int n = 0; n < ds.length(); n++) {
        JSONObject d = ds.getJSONObject(n);
        w.duties.add(
            new Duty(
                LocalDate.parse(d.getString("date")), d.getString("place"), d.getString("person")));
      }
      for (int n = 0; n < bs.length(); n++) {
        JSONObject d = bs.getJSONObject(n);
        w.debts.add(new Debt(d.getString("person"), LocalDate.parse(d.getString("end"))));
      }
      s.weeks.add(w);
    }
    return s;
  }
}
