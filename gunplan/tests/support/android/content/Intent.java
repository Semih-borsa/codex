package android.content;
import java.util.*;
public class Intent{
 public static final String ACTION_TIMEZONE_CHANGED="ZONE",ACTION_TIME_CHANGED="TIME";
 String action;Map<String,Object> extras=new HashMap<>();public Intent(String a){action=a;}public String getAction(){return action;}public Intent putExtra(String k,long v){extras.put(k,v);return this;}public int getIntExtra(String k,int d){return extras.containsKey(k)?((Number)extras.get(k)).intValue():d;}public long getLongExtra(String k,long d){return extras.containsKey(k)?((Number)extras.get(k)).longValue():d;}
}
