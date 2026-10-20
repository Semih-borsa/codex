package tr.gunplan.app;
import android.app.Instrumentation;import android.os.Bundle;import org.json.*;import java.time.*;
public final class PlannerInstrumentation extends Instrumentation{
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 @Override public void onStart(){Bundle r=new Bundle();try{android.content.Context c=getTargetContext();Reminders.channel(c);LocalDateTime time=LocalDateTime.now().plusHours(1);
 JSONObject data=new JSONObject().put("title","GünPlan arka plan testi").put("note","Uygulama kapalıyken gerçek Android alarmı").put("date",time.toLocalDate().toString()).put("time",time.toLocalTime().withSecond(0).withNano(0).toString()).put("repeat","none").put("category","İş").put("weekMask",1).put("monthDay",2);
 TaskStore.save(c,data.toString());JSONObject s=TaskStore.read(c);int id=s.getInt("nextId")-1;JSONObject t=TaskStore.find(s,id);if(t==null)throw new AssertionError("Persistence failed");long due=System.currentTimeMillis()+90000;t.put("due",due);TaskStore.write(c,s);Reminders.schedule(c,t);if(Backup.merge(c,TaskStore.read(c).toString())!=0)throw new AssertionError("Dedup failed");
 r.putString("stream","PASS native storage and backup dedup; real AlarmManager scheduled task "+id+" at "+due+". Check notification after 90 seconds with app background process stopped.\n");r.putInt("taskId",id);finish(android.app.Activity.RESULT_OK,r);
 }catch(Throwable e){r.putString("stream","FAIL "+e.toString()+"\n");finish(android.app.Activity.RESULT_CANCELED,r);}}
}
