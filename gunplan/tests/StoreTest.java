package tr.gunplan.app;
import android.content.*;import org.json.*;import java.time.*;
public final class StoreTest{
 static int cases;static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);cases++;}
 static JSONObject input(String title,String repeat)throws Exception{return new JSONObject().put("title",title).put("note","Not").put("date",LocalDate.now().toString()).put("time","23:59").put("category","İş").put("repeat",repeat).put("weekMask",1).put("monthDay",2);}
 static JSONObject task(Context c,int id)throws Exception{return TaskStore.find(TaskStore.read(c),id);}
 public static void main(String[]args)throws Exception{
  Context c=new Context();TaskStore.save(c,input("Toplantı","daily").toString());ok(task(c,1)!=null&&Reminders.scheduled.contains(1),"save+alarm");
  JSONObject s=TaskStore.read(c),t=TaskStore.find(s,1);long due=System.currentTimeMillis()-2000;t.put("due",due);TaskStore.write(c,s);
  ReminderReceiver receiver=new ReminderReceiver();receiver.onReceive(c,new Intent("FIRE").putExtra("id",1));long next=task(c,1).getLong("due");ok(next>System.currentTimeMillis(),"next recurrence scheduled");ok(Reminders.notifications.size()==1&&task(c,1).getLong("lastNotified")==due,"delivered current occurrence");
  receiver.onReceive(c,new Intent("FIRE").putExtra("id",1));ok(Reminders.notifications.size()==1,"stale early alarm does not notify");
  receiver.onReceive(c,new Intent("SNOOZE").putExtra("id",1).putExtra("occurrence",due));ok(task(c,1).getLong("snoozeAt")>System.currentTimeMillis()&&task(c,1).getLong("due")==next,"snooze keeps next recurrence");
  TaskStore.complete(c,1,due);ok(task(c,1).getLong("due")==next&&task(c,1).optLong("snoozeAt")==0,"notification completion doesn't skip next occurrence");ok(TaskStore.read(c).getJSONArray("history").length()==1,"completion history saved");
  TaskStore.complete(c,1,due);ok(TaskStore.read(c).getJSONArray("history").length()==1,"duplicate action ignored");
  TaskStore.toggle(c,1);ok(!task(c,1).getBoolean("enabled")&&!Reminders.scheduled.contains(1),"pause cancels alarm");TaskStore.toggle(c,1);ok(task(c,1).getBoolean("enabled")&&Reminders.scheduled.contains(1),"resume plans future");
  s=TaskStore.read(c);TaskStore.find(s,1).put("due",System.currentTimeMillis()-1000);TaskStore.write(c,s);Reminders.allowed=false;int n=Reminders.notifications.size();receiver.onReceive(c,new Intent("FIRE").putExtra("id",1));ok(Reminders.notifications.size()==n&&task(c,1).getLong("due")<System.currentTimeMillis(),"denied permission does not consume occurrence");Reminders.allowed=true;
  String backup=TaskStore.read(c).toString();ok(Backup.merge(c,backup)==0,"restore deduplicates and preserves records");
  JSONObject extra=new JSONObject(backup);extra.getJSONArray("tasks").getJSONObject(0).put("title","Yeni görev");ok(Backup.merge(c,extra.toString())==1&&TaskStore.read(c).getJSONArray("tasks").length()==2,"restore merges without deletion");
  String before=TaskStore.read(c).toString();try{Backup.merge(c,"{\"schema\":2}");throw new AssertionError("bad schema accepted");}catch(Exception expected){}ok(TaskStore.read(c).toString().equals(before),"bad backup leaves all data untouched");
  c.prefs.fail=true;try{TaskStore.save(c,input("Kaydedilemez","daily").toString());throw new AssertionError("write failure ignored");}catch(Exception expected){}c.prefs.fail=false;ok(TaskStore.read(c).getJSONArray("tasks").length()==2,"failed write preserves stored state");
  TaskStore.delete(c,1);ok(task(c,1)==null&&!Reminders.scheduled.contains(1),"delete cancels alarm");receiver.onReceive(c,new Intent("FIRE").putExtra("id",1));ok(Reminders.notifications.size()==n,"deleted task cannot notify");
  Context corrupt=new Context();corrupt.prefs.edit().putString("state","broken json").commit();try{TaskStore.read(corrupt);throw new AssertionError("corruption silently reset");}catch(JSONException expected){cases++;}
  System.out.println("PASS "+cases+" store/receiver/backup transitions using fake Android storage+delivery (not OS delivery)");
 }
}
