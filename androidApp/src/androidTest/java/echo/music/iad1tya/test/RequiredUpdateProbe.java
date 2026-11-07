package echo.music.iad1tya.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import echo.music.iad1tya.update.AppUpdateGate;
import echo.music.iad1tya.update.MandatoryUpdateActivity;
import echo.music.iad1tya.ui.component.MusicLightsSettingsActivity;

/** Confirms a secondary activity cannot bypass a cached mandatory release policy. */
public final class RequiredUpdateProbe extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        Context c=getTargetContext(); SharedPreferences p=c.getSharedPreferences("required_updates",0);
        String original=p.getString("minimum",""); boolean required=p.getBoolean("required",false);
        Activity gate=null;
        try {
            p.edit().putString("minimum","99.0.0").putBoolean("required",true).commit();
            if(!AppUpdateGate.INSTANCE.required())throw new AssertionError("Higher confirmed floor was ignored");
            ActivityMonitor monitor=addMonitor(MandatoryUpdateActivity.class.getName(),null,false);
            startActivitySync(new Intent(c,MusicLightsSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            gate=waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);
            if(gate==null)throw new AssertionError("Edge lighting activity bypassed required update");
            result.putString("stream","REQUIRED_UPDATE_OK: secondary activity redirected to blocking update screen\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable t){result.putString("stream","REQUIRED_UPDATE_FAILED: "+t+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally {
            p.edit().putString("minimum",original).putBoolean("required",required).commit();
            if(gate!=null){final Activity a=gate;runOnMainSync(a::finishAffinity);}
        }
    }
}
