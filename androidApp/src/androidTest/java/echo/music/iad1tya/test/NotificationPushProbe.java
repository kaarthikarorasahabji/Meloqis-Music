package echo.music.iad1tya.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.Tasks;
import echo.music.iad1tya.ui.component.FestivalEngine;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Single-device, real FCM delivery fixture. The registration token never enters test output. */
public final class NotificationPushProbe extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args=arguments; start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            waitForIdleSync();
            Context c=getTargetContext();
            SharedPreferences prefs=c.getSharedPreferences("festival_wishes",0);
            File backup=new File(c.getFilesDir(),"notification-probe-backup.json");
            String phase=args.getString("phase","verify"),id=args.getString("campaign","delivery-test");
            if(phase.equals("prepare")) {
                if(backup.exists())throw new IllegalStateException("Clean up the previous fixture first");
                Files.write(backup.toPath(),new org.json.JSONObject(prefs.getAll()).toString().getBytes(StandardCharsets.UTF_8));
                prefs.edit().putBoolean("enabled",true).commit();
                FestivalEngine.INSTANCE.getEnabled().setValue(true);
                String token=Tasks.await(FirebaseMessaging.getInstance().getToken(),90,TimeUnit.SECONDS);
                Files.write(new File(c.getFilesDir(),"notification-probe-token.txt").toPath(),token.getBytes(StandardCharsets.UTF_8));
                result.putString("stream","PUSH_PROBE_READY: private device token saved\n");
            } else if(phase.equals("cleanup")) {
                ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel("festival",id.hashCode());
                Tasks.await(FirebaseMessaging.getInstance().unsubscribeFromTopic(FestivalEngine.TOPIC),30,TimeUnit.SECONDS);
                if(backup.exists()) {
                    org.json.JSONObject values=new org.json.JSONObject(new String(Files.readAllBytes(backup.toPath()),StandardCharsets.UTF_8));
                    SharedPreferences.Editor editor=prefs.edit().clear();
                    java.util.Iterator<String> keys=values.keys();
                    while(keys.hasNext()){String key=keys.next();Object value=values.get(key);if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);else if(value instanceof String)editor.putString(key,(String)value);else throw new IllegalStateException("Unsupported fixture preference type");}
                    editor.commit();
                    backup.delete();
                } else prefs.edit().putBoolean("enabled",false).remove("received-"+id).remove("sent-"+id).remove("seen-"+id).commit();
                new File(c.getFilesDir(),"notification-probe-token.txt").delete();
                result.putString("stream","PUSH_PROBE_CLEANED: test notification and token file removed\n");
            } else {
                Bundle waiting = new Bundle(); waiting.putString("stream", "PUSH_PROBE_WAITING\n"); sendStatus(0, waiting);
                long end=System.currentTimeMillis()+60000; boolean received=false,visible=false;
                while(System.currentTimeMillis()<end) {
                    received=prefs.getBoolean("received-"+id,false);
                    for(StatusBarNotification notification:((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications())
                        if("festival".equals(notification.getTag())&&notification.getId()==id.hashCode())visible=true;
                    if(received&&visible)break; Thread.sleep(1000);
                }
                if(!received||!visible)throw new AssertionError("received="+received+", notificationVisible="+visible);
                FestivalEngine.INSTANCE.checkToday(c,id);
                if(FestivalEngine.INSTANCE.getDialog().getValue()==null||!id.equals(FestivalEngine.INSTANCE.getDialog().getValue().getId()))throw new AssertionError("Animated wish model missing");
                result.putString("stream","PUSH_DELIVERED: real Firebase message received, Android notification posted, animated wish queued\n");
            }
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","PUSH_PROBE_FAILED: "+t+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
}
