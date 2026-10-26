package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.json.*;

final class Store {
  private final AtomicFile file;

  Store(Context context) {
    file = new AtomicFile(new File(context.getFilesDir(), "roster-v1.json"));
  }

  State load() throws Exception {
    if (!file.getBaseFile().exists() && !new File(file.getBaseFile().getPath() + ".bak").exists())
      return new State();
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
    JSONObject root = new JSONObject().put("version", 2);
    JSONArray people = new JSONArray(), leaves = new JSONArray(), weeks = new JSONArray();
    JSONArray places = new JSONArray();
    for (Place p : s.places)
      places.put(
          new JSONObject()
              .put("id", p.id)
              .put("name", p.name)
              .put("days", p.days)
              .put("limit", p.limit)
              .put("automatic", p.automatic)
              .put("deleted", p.deleted));
    JSONArray holidays = new JSONArray();
    for (Holidays.Holiday h : s.holidays)
      holidays.put(
          new JSONObject()
              .put("id", h.id)
              .put("name", h.name)
              .put("date", h.date)
              .put("units", h.units)
              .put("enabled", h.enabled));
    root.put("holidays", holidays);
    JSONArray fullDay = new JSONArray();
    for (Duty d : s.fullDayHolidayWork)
      fullDay.put(
          new JSONObject().put("date", d.date).put("place", d.place).put("person", d.person));
    root.put("fullDayHolidayWork", fullDay);
    root.put("places", places);
    root.put("hiddenPlaces", new JSONArray(s.hiddenPlaces));
    root.put("weekendOrder", new JSONArray(s.weekendOrder));
    root.put("rotationRevision", s.rotationRevision);
    for (Person p : s.people)
      people.put(new JSONObject().put("id", p.id).put("name", p.name).put("active", p.active));
    for (Leave l : s.leaves)
      leaves.put(
          new JSONObject()
              .put("person", l.person)
              .put("id", l.id)
              .put("place", l.place)
              .put("start", l.start)
              .put("end", l.end)
              .put("reason", l.reason)
              .put("days", l.days));
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
              .put("rotationRevision", w.rotationRevision)
              .put("duties", duties)
              .put("debts", debts)
              .put("limitOverrides", new JSONArray(w.limitOverrides))
              .put("simultaneousOverrides", new JSONArray(w.simultaneousOverrides))
              .put("leaveOverrides", new JSONArray(w.leaveOverrides)));
    }
    return root.put("people", people).put("leaves", leaves).put("weeks", weeks).toString();
  }

  static State decode(String json) throws JSONException {
    JSONObject root = new JSONObject(json);
    if (root.getInt("version") != 1 && root.getInt("version") != 2)
      throw new JSONException("Desteklenmeyen veri sürümü");
    State s = new State();
    JSONArray holidays = root.optJSONArray("holidays");
    if (holidays != null)
      for (int i = 0; i < holidays.length(); i++) {
        JSONObject h = holidays.getJSONObject(i);
        Holidays.Holiday holiday =
            new Holidays.Holiday(
                h.getString("id"),
                h.getString("name"),
                LocalDate.parse(h.getString("date")),
                h.getInt("units"),
                h.getBoolean("enabled"));
        if (s.holidays.stream().anyMatch(old -> old.id.equals(holiday.id)))
          throw new JSONException("Mükerrer tatil kimliği");
        s.holidays.add(holiday);
      }
    s.rotationRevision = root.optInt("rotationRevision", 0);
    JSONArray order = root.optJSONArray("weekendOrder");
    if (order != null)
      for (int i = 0; i < order.length(); i++) s.weekendOrder.add(order.getString(i));
    JSONArray hidden = root.optJSONArray("hiddenPlaces");
    if (hidden != null)
      for (int i = 0; i < hidden.length(); i++) s.hiddenPlaces.add(hidden.getString(i));
    JSONArray locations = root.optJSONArray("places");
    if (locations != null) {
      s.places.clear();
      for (int i = 0; i < locations.length(); i++) {
        JSONObject j = locations.getJSONObject(i);
        Place place =
            new Place(j.getString("id"), j.getString("name"), j.getInt("days"), j.getInt("limit"));
        place.automatic = j.optBoolean("automatic", true);
        place.deleted = j.optBoolean("deleted", false);
        s.places.add(place);
      }
      s.place(WORK);
      s.place(BRANCH);
    }
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
      Leave leave =
          new Leave(
              j.getString("person"),
              j.getString("place"),
              LocalDate.parse(j.getString("start")),
              LocalDate.parse(j.getString("end")),
              j.getString("reason"),
              j.optInt("days", 0));
      String legacyIdentity =
          leave.person
              + "\n"
              + leave.place
              + "\n"
              + leave.start
              + "\n"
              + leave.end
              + "\n"
              + leave.reason
              + "\n"
              + leave.days
              + "\n"
              + i;
      String stableId =
          UUID.nameUUIDFromBytes(legacyIdentity.getBytes(StandardCharsets.UTF_8)).toString();
      leave.id = j.optString("id", stableId);
      if (leave.id.trim().isEmpty()) leave.id = stableId;
      s.leaves.add(leave);
    }
    for (int i = 0; i < weeks.length(); i++) {
      JSONObject j = weeks.getJSONObject(i);
      Week w = new Week(LocalDate.parse(j.getString("monday")));
      w.cursor = j.getString("cursor");
      w.rotationRevision = j.optInt("rotationRevision", 0);
      JSONArray overrides = j.optJSONArray("limitOverrides");
      if (overrides != null)
        for (int n = 0; n < overrides.length(); n++) w.limitOverrides.add(overrides.getString(n));
      JSONArray sameDay = j.optJSONArray("simultaneousOverrides");
      if (sameDay != null)
        for (int n = 0; n < sameDay.length(); n++)
          w.simultaneousOverrides.add(sameDay.getString(n));
      JSONArray leaveOverrides = j.optJSONArray("leaveOverrides");
      if (leaveOverrides != null)
        for (int n = 0; n < leaveOverrides.length(); n++)
          w.leaveOverrides.add(leaveOverrides.getString(n));
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
    JSONArray fullDay = root.optJSONArray("fullDayHolidayWork");
    if (fullDay != null) {
      if (fullDay.length() > 100000) throw new JSONException("Tam gün çalışma kayıt sınırı aşıldı");
      java.util.HashSet<String> seen = new java.util.HashSet<>();
      for (int i = 0; i < fullDay.length(); i++) {
        JSONObject item = fullDay.getJSONObject(i);
        Duty duty =
            new Duty(
                LocalDate.parse(item.getString("date")),
                item.getString("place"),
                item.getString("person"));
        if (s.people.stream().noneMatch(p -> p.id.equals(duty.person))
            || s.places.stream().noneMatch(p -> p.id.equals(duty.place)))
          throw new JSONException("Tam gün çalışma kaydında kişi veya görev yeri bulunamadı");
        if (!seen.add(duty.date + "|" + duty.place + "|" + duty.person))
          throw new JSONException("Mükerrer tam gün çalışma kaydı");
        s.fullDayHolidayWork.add(duty);
      }
    }
    return s;
  }
}
