package tr.gunplan.app;
import android.app.*;import android.content.*;import android.os.Build;import org.json.*;
final class Reminders {
 static final String CHANNEL="gunplan_tasks_v1";
 static void channel(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);NotificationChannel ch=new NotificationChannel(CHANNEL,"Görev hatırlatmaları",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Planladığınız görevlerin zamanı geldiğinde hatırlatır.");ch.enableVibration(true);n.createNotificationChannel(ch);}
 static boolean allowed(Context c){channel(c);NotificationManager n=c.getSystemService(NotificationManager.class);return n.areNotificationsEnabled()&&n.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
 static boolean exact(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
 static PendingIntent alarm(Context c,int id,boolean snooze){Intent i=new Intent(c,ReminderReceiver.class).setAction(snooze?"SNOOZE_FIRE":"FIRE").putExtra("id",id);return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void schedule(Context c,JSONObject t)throws Exception{
  if(!t.optBoolean("enabled"))return;long due=t.getLong("due");if(due>0&&t.optLong("notifiedAt")!=due)set(c,alarm(c,t.getInt("id"),false),due);
  if(t.optLong("snoozeAt")>0)set(c,alarm(c,t.getInt("id"),true),t.getLong("snoozeAt"));
 }
 static void set(Context c,PendingIntent p,long at){AlarmManager a=c.getSystemService(AlarmManager.class);at=Math.max(System.currentTimeMillis()+1200,at);try{if(exact(c)){a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);return;}}catch(SecurityException ignored){}a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);}
 static void cancel(Context c,int id){AlarmManager a=c.getSystemService(AlarmManager.class);a.cancel(alarm(c,id,false));a.cancel(alarm(c,id,true));c.getSystemService(NotificationManager.class).cancel(id);}
 static void restore(Context c,boolean timezone){synchronized(TaskStore.class){try{JSONObject s=TaskStore.read(c);JSONArray a=s.getJSONArray("tasks");for(int i=0;i<a.length();i++){JSONObject t=a.getJSONObject(i);if(timezone&&t.optBoolean("enabled")&&t.optLong("due")>System.currentTimeMillis()){long next=TaskStore.next(t,System.currentTimeMillis()-1);if(next>0)t.put("due",next).put("notifiedAt",0);}schedule(c,t);}if(timezone)TaskStore.write(c,s);}catch(Exception e){android.util.Log.e("GunPlan","Hatırlatmalar yeniden kurulamadı",e);}}}
 static PendingIntent action(Context c,int id,String action,long occurrence){return PendingIntent.getBroadcast(c,id,new Intent(c,ReminderReceiver.class).setAction(action).putExtra("id",id).putExtra("occurrence",occurrence),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void notifyTask(Context c,JSONObject t,long occurrence)throws Exception{
  int id=t.getInt("id");Intent open=new Intent(c,MainActivity.class).putExtra("taskId",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(tr.gunplan.app.R.drawable.ic_notification).setContentTitle(t.getString("title")).setContentText(t.optString("note").isEmpty()?t.getString("category")+" · Görevinizin zamanı geldi":t.getString("note")).setStyle(new Notification.BigTextStyle().bigText(t.optString("note"))).setContentIntent(PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PRIVATE).setOnlyAlertOnce(false).addAction(new Notification.Action.Builder(null,"Tamamla",action(c,id,"COMPLETE",occurrence)).build()).addAction(new Notification.Action.Builder(null,"10 dk ertele",action(c,id,"SNOOZE",occurrence)).build());
  if(allowed(c))c.getSystemService(NotificationManager.class).notify(id,b.build());
 }
 static void test(Context c){channel(c);Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(tr.gunplan.app.R.drawable.ic_notification).setContentTitle("GünPlan hazır").setContentText("Görev hatırlatmalarınız bu şekilde görünecek.").setAutoCancel(true).build();if(allowed(c))c.getSystemService(NotificationManager.class).notify(2000000000,n);}
}
