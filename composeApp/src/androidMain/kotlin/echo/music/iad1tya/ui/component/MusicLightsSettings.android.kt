package echo.music.iad1tya.ui.component

import android.content.Context
import android.content.SharedPreferences

internal object LightsPreferences {
    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences("music_lights", Context.MODE_PRIVATE)
    val positions = listOf("Frame", "Bottom", "Top", "Left", "Right", "Center")
    val modes = listOf("Ribbon", "Comet", "Pulse", "Bars", "Wave", "Orbit", "Dots")
    val palettes = listOf("Aurora", "Sunset", "Ocean", "Rainbow", "Neon", "Album art", "Custom")
    val clocks = listOf("Digital", "Stacked", "Analog", "Minimal")
    val presets = listOf("Aurora frame", "Neon pulse", "Rainbow trail", "Midnight clock")

    fun read(p: SharedPreferences) = LightsSettings(
        position = p.getString("position", "Frame").takeIf { it in positions } ?: "Frame",
        mode = p.getString("mode", "Ribbon").takeIf { it in modes } ?: "Ribbon",
        palette = p.getString("palette", "Aurora").takeIf { it in palettes } ?: "Aurora",
        size = p.getFloat("size", .35f).safe(.1f, .8f, .35f),
        brightness = p.getFloat("brightness", .65f).safe(.08f, 1f, .65f),
        sensitivity = p.getFloat("sensitivity", 1f).safe(.5f, 2f, 1f),
        thickness = p.getFloat("thickness", 3f).safe(1f, 12f, 3f),
        glow = p.getFloat("glow", .65f).safe(0f, 1f, .65f),
        radius = p.getFloat("radius", 32f).safe(0f, 80f, 32f),
        inset = p.getFloat("inset", 6f).safe(2f, 40f, 6f),
        notch = p.getFloat("notch", 0f).safe(0f, 180f, 0f),
        speed = p.getFloat("speed", 1f).safe(.2f, 2f, 1f),
        hue = p.getFloat("hue", 265f).safe(0f, 360f, 265f),
        secondHue = p.getFloat("second_hue", 175f).safe(0f, 360f, 175f),
        clock = p.getBoolean("clock", true),
        clockStyle = p.getString("clock_style", "Digital").takeIf { it in clocks } ?: "Digital",
        clockY = p.getFloat("clock_y", .43f).safe(.25f, .7f, .43f),
        details = p.getBoolean("details", true),
        battery = p.getBoolean("battery", true),
        ambient = p.getBoolean("ambient", true),
        mirror = p.getBoolean("mirror", true),
        powerSaver = p.getBoolean("power_saver", true),
        sessionMinutes = p.getInt("session_minutes", 30).takeIf { it in listOf(15, 30, 60, 120) } ?: 30,
    )

    fun save(p: SharedPreferences, s: LightsSettings) {
        p.edit().putString("position", s.position).putString("mode", s.mode).putString("palette", s.palette)
            .putFloat("size", s.size).putFloat("brightness", s.brightness).putFloat("sensitivity", s.sensitivity)
            .putFloat("thickness", s.thickness).putFloat("glow", s.glow).putFloat("radius", s.radius)
            .putFloat("inset", s.inset).putFloat("notch", s.notch).putFloat("speed", s.speed)
            .putFloat("hue", s.hue).putFloat("second_hue", s.secondHue).putBoolean("clock", s.clock)
            .putString("clock_style", s.clockStyle).putFloat("clock_y", s.clockY).putBoolean("details", s.details)
            .putBoolean("battery", s.battery).putBoolean("ambient", s.ambient).putBoolean("mirror", s.mirror)
            .putBoolean("power_saver", s.powerSaver).putInt("session_minutes", s.sessionMinutes).apply()
    }

    fun preset(name: String, old: LightsSettings) = when (name) {
        "Neon pulse" -> old.copy(position = "Frame", mode = "Pulse", palette = "Neon", thickness = 4f, glow = .85f)
        "Rainbow trail" -> old.copy(position = "Frame", mode = "Comet", palette = "Rainbow", thickness = 3f, glow = .75f, speed = 1.3f)
        "Midnight clock" -> old.copy(position = "Frame", mode = "Ribbon", palette = "Ocean", thickness = 1.5f, glow = .25f, brightness = .2f, clock = true, clockStyle = "Stacked")
        else -> old.copy(position = "Frame", mode = "Ribbon", palette = "Aurora", thickness = 3f, glow = .65f, brightness = .65f)
    }

    private fun Float.safe(low: Float, high: Float, default: Float) = if (isFinite()) coerceIn(low, high) else default
}

internal data class LightsSettings(
    val position: String, val mode: String, val palette: String,
    val size: Float, val brightness: Float, val sensitivity: Float,
    val thickness: Float, val glow: Float, val radius: Float, val inset: Float, val notch: Float,
    val speed: Float, val hue: Float, val secondHue: Float,
    val clock: Boolean, val clockStyle: String, val clockY: Float,
    val details: Boolean, val battery: Boolean, val ambient: Boolean, val mirror: Boolean,
    val powerSaver: Boolean, val sessionMinutes: Int,
)
