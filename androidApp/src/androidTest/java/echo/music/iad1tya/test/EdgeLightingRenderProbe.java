package echo.music.iad1tya.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import echo.music.iad1tya.ui.component.MusicLightsView;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/** Draws actual Android paths offscreen. No user interface actions or audio injection. */
public final class EdgeLightingRenderProbe extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            final Throwable[] error = new Throwable[1];
            runOnMainSync(() -> { try { verify(); } catch (Throwable t) { error[0] = t; } });
            if (error[0] != null) throw new RuntimeException(error[0]);
            result.putString("stream", "EDGE_RENDER_OK: 35 effect/placement combinations, centre migration, disabled/quiet rendering and notch fit passed\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable t) {
            result.putString("stream", "EDGE_RENDER_FAILED: " + t + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void verify() throws Exception {
        Context context = getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("music_lights", Context.MODE_PRIVATE);
        Map<String, ?> original = prefs.getAll();
        File directory = new File(context.getFilesDir(), "edge-render-probe");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Cannot create probe directory");
        JSONObject report = new JSONObject();
        int combinations = 0;
        try {
            prefs.edit().remove("enabled").commit();
            if (!echo.music.iad1tya.ui.component.LightsPreferences.INSTANCE.read(prefs).getEnabled()) throw new AssertionError("New installs must default on");
            for (String mode : new String[]{"Flow", "Comet", "Breath", "Twin trails", "Bars", "Wave", "Sparkles"}) {
                for (String position : new String[]{"Frame", "Top", "Bottom", "Left", "Right"}) {
                    prefs.edit().clear().putString("mode",mode).putString("position",position).commit();
                    Bitmap bitmap=render(context,true,false);
                    if(visiblePixels(bitmap)<100) throw new AssertionError("Empty effect: "+mode+"/"+position);
                    if(interiorPixels(bitmap)>0) throw new AssertionError("Effect entered the centre: "+mode+"/"+position);
                    if(position.equals("Frame"))save(bitmap,new File(directory,"frame-"+mode.toLowerCase(java.util.Locale.ROOT).replace(' ','-')+".png"));
                    bitmap.recycle();combinations++;
                }
            }
            prefs.edit().clear().putString("position","Center").putString("mode","Orbit").commit();
            echo.music.iad1tya.ui.component.LightsSettings legacy=echo.music.iad1tya.ui.component.LightsPreferences.INSTANCE.read(prefs);
            if(!legacy.getPosition().equals("Frame")||!legacy.getMode().equals("Twin trails"))throw new AssertionError("Legacy centre setting did not migrate");
            prefs.edit().clear().putBoolean("enabled",false).commit();
            Bitmap disabled=render(context,false,true);
            if(visiblePixels(disabled)!=0)throw new AssertionError("Disabled in-app lights still rendered");
            disabled.recycle();
            // A real quiet overlay must not invent music when ambient mode is off.
            prefs.edit().clear().putBoolean("ambient", false).commit();
            Bitmap quiet = render(context, false, true);
            if (visiblePixels(quiet) != 0) throw new AssertionError("Silent overlay invented visible audio");
            quiet.recycle();
            prefs.edit().clear().putString("mode", "Flow").putString("position", "Frame")
                .putFloat("notch", 160f).putFloat("radius", 80f).putFloat("thickness", 12f).putFloat("inset", 40f).commit();
            Bitmap notch = render(context, true, false);
            if (visiblePixels(notch) < 100) throw new AssertionError("Notch/corner controls removed all light");
            save(notch, new File(directory, "notch-fit.png")); notch.recycle();
            report.put("passed", true).put("effectPlacementCombinations", combinations).put("centreRemoved", true).put("disabled", true)
                .put("quietOverlay", true).put("notchFit", true).put("width", 1080).put("height", 2400);
            try (FileOutputStream out = new FileOutputStream(new File(directory, "report.json"))) {
                out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            }
        } finally {
            SharedPreferences.Editor restore = prefs.edit().clear();
            for (Map.Entry<String, ?> entry : original.entrySet()) {
                String key = entry.getKey(); Object value = entry.getValue();
                if (value instanceof String) restore.putString(key, (String)value);
                else if (value instanceof Boolean) restore.putBoolean(key, (Boolean)value);
                else if (value instanceof Float) restore.putFloat(key, (Float)value);
                else if (value instanceof Integer) restore.putInt(key, (Integer)value);
                else if (value instanceof Long) restore.putLong(key, (Long)value);
                else if (value instanceof Set) restore.putStringSet(key, (Set<String>)value);
            }
            restore.commit();
        }
    }
    private Bitmap render(Context context, boolean demo, boolean overlay) {
        MusicLightsView view = new MusicLightsView(context, true, !overlay);
        view.setDemo(demo);
        view.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 1080, 2400);
        Bitmap bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap)); view.stop(); return bitmap;
    }
    private int interiorPixels(Bitmap bitmap) {
        int count=0;
        for(int y=170;y<bitmap.getHeight()-170;y+=4)for(int x=170;x<bitmap.getWidth()-170;x+=4)
            if((bitmap.getPixel(x,y)>>>24)>10)count++;
        return count;
    }
    private int visiblePixels(Bitmap bitmap) {
        int count = 0;
        for (int y = 0; y < bitmap.getHeight(); y += 3) for (int x = 0; x < bitmap.getWidth(); x += 3) {
            int colour = bitmap.getPixel(x, y);
            if ((colour & 0x00ffffff) != 0 && (colour >>> 24) != 0) count++;
        }
        return count;
    }
    private void save(Bitmap bitmap, File file) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
    }
}
