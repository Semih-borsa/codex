package tr.gunplan.app;
import android.content.*;import org.json.*;import java.time.*;
final class TaskStore {
 static synchronized JSONObject read(Context c)throws Exception{
  String text=c.getSharedPreferences("gunplan_v1",0).getString("state",null);
  if(text==null)return new JSONObject().put("schema",1).put("nextId",1).put("tasks",new JSONArray()).put("history",new JSONArray()).put("theme","system");
  JSONObject s=new JSONObject(text);if(s.getInt("schema")!=1||!(s.get("tasks") instanceof JSONArray)||!(s.get("history") instanceof JSONArray))throw new Exception("Kayıt biçimi okunamadı. Verileriniz değiştirilmedi.");return s;
 }
 static synchronized void write(Context c,JSONObject s)throws Exception{if(!c.getSharedPreferences("gunplan_v1",0).edit().putString("state",s.toString()).commit())throw new Exception("Kayıt kaydedilemedi. Depolama alanını kontrol edin.");}
 static JSONObject find(JSONObject s,int id)throws Exception{JSONArray a=s.getJSONArray("tasks");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getInt("id")==id)return a.getJSONObject(i);return null;}
 static long next(JSONObject t,long after)throws Exception{return Recurrence.next(t.getString("date"),t.getString("time"),t.getString("repeat"),t.optInt("weekMask"),t.optInt("monthDay"),t.optBoolean("nextWeekday",false),after,ZoneId.systemDefault());}
 static JSONObject clean(JSONObject input)throws Exception{
  JSONObject t=new JSONObject();String title=input.optString("title").trim(),note=input.optString("note").trim();if(title.isEmpty()||title.length()>120)throw new Exception("Görev adı 1–120 karakter olmalı.");if(note.length()>2000)throw new Exception("Not en fazla 2000 karakter olabilir.");
  LocalDate.parse(input.getString("date"));LocalTime.parse(input.getString("time"));String repeat=input.getString("repeat"),category=input.optString("category","Kişisel");if(!java.util.Arrays.asList("Kişisel","İş","Ödeme").contains(category))throw new Exception("Kategori geçersiz.");
  t.put("title",title).put("note",note).put("date",input.getString("date")).put("time",input.getString("time")).put("repeat",repeat).put("weekMask",input.optInt("weekMask",1)).put("monthDay",input.optInt("monthDay",LocalDate.parse(input.getString("date")).getDayOfMonth())).put("nextWeekday","monthly".equals(repeat)&&input.optBoolean("nextWeekday",false)).put("category",category).put("priority",input.optBoolean("priority",false));
  next(t,0);return t;
 }
 static synchronized void save(Context c,String text)throws Exception{
  JSONObject input=new JSONObject(text),s=read(c),old=find(s,input.optInt("id")),t=clean(input);JSONArray a=s.getJSONArray("tasks");int id=old==null?s.getInt("nextId"):old.getInt("id");if(old==null&&a.length()>=1000)throw new Exception("En fazla 1000 görev kaydedilebilir.");
  long now=System.currentTimeMillis(),due=next(t,now-1);if(due<0){if(old==null)throw new Exception("Hatırlatma için ileri bir tarih ve saat seçin.");due=LocalDate.parse(t.getString("date")).atTime(LocalTime.parse(t.getString("time"))).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}
  t.put("id",id).put("due",due).put("enabled",true).put("completed",false).put("notifiedAt",old!=null&&old.optLong("due")==due?old.optLong("notifiedAt"):0).put("createdAt",old==null?now:old.optLong("createdAt"));
  if(old==null){a.put(t);s.put("nextId",id+1);}else{for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getInt("id")==id)a.put(i,t);}
  write(c,s);Reminders.cancel(c,id);Reminders.schedule(c,t);
 }
 static void log(JSONObject s,JSONObject t,long occurrence)throws Exception{JSONArray h=s.getJSONArray("history");h.put(new JSONObject().put("taskId",t.getInt("id")).put("title",t.getString("title")).put("category",t.getString("category")).put("at",System.currentTimeMillis()).put("occurrence",occurrence));while(h.length()>300)h.remove(0);}
 static synchronized void complete(Context c,int id,long occurrence)throws Exception{
  JSONObject s=read(c),t=find(s,id);if(t==null)return;long due=t.getLong("due");if(occurrence>0&&t.optLong("lastNotified")!=occurrence)return;
  if(occurrence<=0&&t.optBoolean("completed"))return;
  log(s,t,occurrence>0?occurrence:due);t.put("snoozeAt",0);
  if(occurrence<=0||"none".equals(t.getString("repeat"))){if("none".equals(t.getString("repeat")))t.put("enabled",false).put("completed",true);else t.put("due",next(t,Math.max(due,System.currentTimeMillis()))).put("notifiedAt",0);}
  t.put("lastNotified",0);write(c,s);Reminders.cancel(c,id);Reminders.schedule(c,t);
 }
 static synchronized void toggle(Context c,int id)throws Exception{JSONObject s=read(c),t=find(s,id);if(t==null)return;boolean enabled=!t.optBoolean("enabled");if(enabled){long due=next(t,System.currentTimeMillis()-1);if(due<0)throw new Exception("Bu görevin tarihi geçti. Düzenleyerek yeni tarih seçin.");t.put("due",due).put("notifiedAt",0).put("completed",false);}t.put("enabled",enabled).put("snoozeAt",0);write(c,s);Reminders.cancel(c,id);Reminders.schedule(c,t);}
 static synchronized void delete(Context c,int id)throws Exception{JSONObject s=read(c);JSONArray a=s.getJSONArray("tasks");for(int i=a.length()-1;i>=0;i--)if(a.getJSONObject(i).getInt("id")==id)a.remove(i);write(c,s);Reminders.cancel(c,id);}
 static synchronized void theme(Context c,String theme)throws Exception{if(!java.util.Arrays.asList("system","light","dark").contains(theme))return;JSONObject s=read(c);s.put("theme",theme);write(c,s);}
}
