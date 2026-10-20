package tr.gunplan.app;
import android.content.*;import org.json.*;
public final class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent){synchronized(TaskStore.class){try{
  int id=intent.getIntExtra("id",0);String action=intent.getAction();if("COMPLETE".equals(action)){TaskStore.complete(c,id,intent.getLongExtra("occurrence",0));return;}
  JSONObject s=TaskStore.read(c),t=TaskStore.find(s,id);if(t==null||!t.optBoolean("enabled"))return;long now=System.currentTimeMillis();
  if("SNOOZE".equals(action)){if(t.optLong("lastNotified")!=intent.getLongExtra("occurrence",0))return;t.put("snoozeAt",now+600000);TaskStore.write(c,s);c.getSystemService(android.app.NotificationManager.class).cancel(id);Reminders.schedule(c,t);return;}
  if(!Reminders.allowed(c))return;
  if("SNOOZE_FIRE".equals(action)){long at=t.optLong("snoozeAt");if(at<=0||t.optLong("lastNotified")<=0)return;if(at>now+1500){Reminders.schedule(c,t);return;}Reminders.notifyTask(c,t,t.getLong("lastNotified"));t.put("snoozeAt",0);TaskStore.write(c,s);return;}
  if(!"FIRE".equals(action))return;long due=t.getLong("due");if(due>now+1500){Reminders.schedule(c,t);return;}if(t.optLong("notifiedAt")==due)return;
  Reminders.notifyTask(c,t,due);t.put("lastNotified",due).put("notifiedAt",due);
  if(!"none".equals(t.getString("repeat")))t.put("due",TaskStore.next(t,now)).put("notifiedAt",0);
  TaskStore.write(c,s);Reminders.schedule(c,t);
 }catch(Exception e){android.util.Log.e("GunPlan","Hatırlatma işlenemedi",e);}}}
}
