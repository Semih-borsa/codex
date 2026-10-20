package tr.gunplan.app;import android.content.Context;import org.json.*;import java.util.*;
final class Reminders{
 static boolean allowed=true;static List<Long> notifications=new ArrayList<>();static Set<Integer> scheduled=new HashSet<>();
 static boolean allowed(Context c){return allowed;}
 static void cancel(Context c,int id){scheduled.remove(id);}
 static void schedule(Context c,JSONObject t)throws Exception{if(t.optBoolean("enabled"))scheduled.add(t.getInt("id"));}
 static void notifyTask(Context c,JSONObject t,long occurrence){notifications.add(occurrence);}
}
