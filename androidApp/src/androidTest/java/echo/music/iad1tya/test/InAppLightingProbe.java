package echo.music.iad1tya.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import echo.music.iad1tya.ui.component.InAppEdgeLighting;
import echo.music.iad1tya.ui.component.LightsPreferences;
import echo.music.iad1tya.ui.component.MusicLightsSettingsActivity;
import java.util.Map;
import java.util.Set;

/** Device-level lifecycle checks; no floating-window permission or UI input injection. */
public final class InAppLightingProbe extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        waitForIdleSync();
        Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("music_lights",0);
        Map<String,?> original=prefs.getAll();Bundle result=new Bundle();Activity activity=null;boolean passed=false;
        try {
            prefs.edit().remove("enabled").putBoolean("guide_seen_in_app_v3",true).commit();
            if(!LightsPreferences.INSTANCE.read(prefs).getEnabled())throw new AssertionError("Default-on preference missing");
            activity=startActivitySync(new Intent(c,MusicLightsSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitState(true);
            if(android.os.Build.VERSION.SDK_INT>=29) for(View window:android.view.inspector.WindowInspector.getGlobalWindowViews()) {
                android.view.ViewGroup.LayoutParams params=window.getLayoutParams();
                if(params instanceof WindowManager.LayoutParams && ((WindowManager.LayoutParams)params).type==WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    throw new AssertionError("Lighting created a system overlay");
            }
            runOnMainSync(()->prefs.edit().putBoolean("enabled",false).commit());waitState(false);
            if(LightsPreferences.INSTANCE.read(prefs).getEnabled())throw new AssertionError("Off choice did not persist");
            runOnMainSync(()->prefs.edit().putBoolean("enabled",true).commit());waitState(true);
            final Activity screen=activity;
            runOnMainSync(()->screen.moveTaskToBack(true));waitState(false);
            passed=true;
            result.putString("stream","IN_APP_LIGHTS_OK: default on, persistent off, re-enable, background stop, no system overlay window\n");
        } catch(Throwable t) { result.putString("stream","IN_APP_LIGHTS_FAILED: "+t+"\n"); }
        finally {
            if(activity!=null){final Activity screen=activity;runOnMainSync(screen::finishAffinity);waitForIdleSync();}
            SharedPreferences.Editor restore=prefs.edit().clear();
            for(Map.Entry<String,?> entry:original.entrySet()) {
                Object v=entry.getValue();String k=entry.getKey();
                if(v instanceof String)restore.putString(k,(String)v);
                else if(v instanceof Boolean)restore.putBoolean(k,(Boolean)v);
                else if(v instanceof Float)restore.putFloat(k,(Float)v);
                else if(v instanceof Integer)restore.putInt(k,(Integer)v);
                else if(v instanceof Long)restore.putLong(k,(Long)v);
                else if(v instanceof Set)restore.putStringSet(k,(Set<String>)v);
            }
            restore.commit();
        }
        finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private void waitState(boolean expected)throws Exception {
        long end=System.currentTimeMillis()+5000;
        do { waitForIdleSync();if(Boolean.valueOf(expected).equals(InAppEdgeLighting.INSTANCE.getRunning().getValue()))return;Thread.sleep(100); }while(System.currentTimeMillis()<end);
        throw new AssertionError("Expected in-app lighting active="+expected);
    }
}
