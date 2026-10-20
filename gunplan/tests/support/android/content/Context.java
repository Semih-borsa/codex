package android.content;
import java.util.*;
public class Context{
 public final SharedPreferences prefs=new SharedPreferences();
 public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
 public <T>T getSystemService(Class<T> type){try{return type.getConstructor().newInstance();}catch(Exception e){throw new RuntimeException(e);}}
 public static final class SharedPreferences{
  private final Map<String,String> map=new HashMap<>();public boolean fail=false;
  public String getString(String key,String def){return map.getOrDefault(key,def);}
  public Editor edit(){return new Editor();}
  public final class Editor{private String key,value;public Editor putString(String k,String v){key=k;value=v;return this;}public boolean commit(){if(fail)return false;map.put(key,value);return true;}}
 }
}
