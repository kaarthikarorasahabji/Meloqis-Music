package echo.music.iad1tya.ui.component

import android.content.Context
import android.content.SharedPreferences

internal object LightsPreferences {
    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences("music_lights", Context.MODE_PRIVATE)
    val positions = listOf("Frame", "Bottom", "Top", "Left", "Right")
    val modes = listOf("Flow", "Comet", "Breath", "Twin trails", "Bars", "Wave", "Sparkles")
    val palettes = listOf("Aurora", "Sunset", "Ocean", "Rainbow", "Neon", "Album art", "Custom")
    val presets = listOf("Aurora flow", "Neon trails", "Ocean breath", "Prism spectrum")
    fun read(p: SharedPreferences): LightsSettings {
        val oldMode = p.getString("mode", "Flow")
        val migrated = mapOf("Ribbon" to "Flow", "Pulse" to "Breath", "Orbit" to "Twin trails", "Dots" to "Sparkles")[oldMode] ?: oldMode
        return LightsSettings(
            enabled = p.getBoolean("enabled", true),
            position = p.getString("position", "Frame").takeIf { it in positions } ?: "Frame",
            mode = migrated.takeIf { it in modes } ?: "Flow",
            palette = p.getString("palette", "Aurora").takeIf { it in palettes } ?: "Aurora",
            size = p.getFloat("size", .25f).safe(.1f, .5f, .25f),
            brightness = p.getFloat("brightness", .65f).safe(.1f, 1f, .65f),
            sensitivity = p.getFloat("sensitivity", 1f).safe(.5f, 2f, 1f),
            thickness = p.getFloat("thickness", 2.5f).safe(1f, 6f, 2.5f),
            glow = p.getFloat("glow", .5f).safe(0f, 1f, .5f),
            radius = p.getFloat("radius", 24f).safe(0f, 48f, 24f),
            inset = p.getFloat("inset", 3f).safe(2f, 20f, 3f),
            notch = p.getFloat("notch", 0f).safe(0f, 180f, 0f),
            speed = p.getFloat("speed", 1f).safe(.2f, 2f, 1f),
            hue = p.getFloat("hue", 265f).safe(0f, 360f, 265f),
            secondHue = p.getFloat("second_hue", 175f).safe(0f, 360f, 175f),
            ambient = p.getBoolean("ambient", true),
            mirror = p.getBoolean("mirror", true),
            powerSaver = p.getBoolean("power_saver", true),
            motion = p.getBoolean("motion", true),
        )
    }
    fun save(p: SharedPreferences, s: LightsSettings) {
        p.edit().putBoolean("enabled", s.enabled).putString("position", s.position).putString("mode", s.mode).putString("palette", s.palette)
            .putFloat("size", s.size).putFloat("brightness", s.brightness).putFloat("sensitivity", s.sensitivity)
            .putFloat("thickness", s.thickness).putFloat("glow", s.glow).putFloat("radius", s.radius)
            .putFloat("inset", s.inset).putFloat("notch", s.notch).putFloat("speed", s.speed)
            .putFloat("hue", s.hue).putFloat("second_hue", s.secondHue).putBoolean("ambient", s.ambient)
            .putBoolean("mirror", s.mirror).putBoolean("power_saver", s.powerSaver).putBoolean("motion", s.motion).apply()
    }
    fun preset(name: String, old: LightsSettings) = when (name) {
        "Neon trails" -> old.copy(position="Frame", mode="Twin trails", palette="Neon", thickness=3f, glow=.65f, speed=1.1f)
        "Ocean breath" -> old.copy(position="Frame", mode="Breath", palette="Ocean", thickness=2f, glow=.35f, speed=.65f)
        "Prism spectrum" -> old.copy(position="Frame", mode="Bars", palette="Rainbow", thickness=2f, glow=.4f, size=.25f)
        else -> old.copy(position="Frame", mode="Flow", palette="Aurora", thickness=2.5f, glow=.5f, brightness=.65f, speed=1f)
    }
    private fun Float.safe(low: Float, high: Float, default: Float) = if (isFinite()) coerceIn(low, high) else default
}
internal data class LightsSettings(
    val enabled: Boolean, val position: String, val mode: String, val palette: String,
    val size: Float, val brightness: Float, val sensitivity: Float,
    val thickness: Float, val glow: Float, val radius: Float, val inset: Float, val notch: Float,
    val speed: Float, val hue: Float, val secondHue: Float,
    val ambient: Boolean, val mirror: Boolean, val powerSaver: Boolean, val motion: Boolean,
)
