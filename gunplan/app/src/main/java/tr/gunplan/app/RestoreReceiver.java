package tr.gunplan.app;
import android.content.*;
public final class RestoreReceiver extends BroadcastReceiver{
 @Override public void onReceive(Context c,Intent i){boolean zone=Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction())||Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_BOOT_COMPLETED.equals(i.getAction());Reminders.restore(c,zone);}
}
