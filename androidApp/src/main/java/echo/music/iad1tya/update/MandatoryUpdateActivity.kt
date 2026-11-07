package echo.music.iad1tya.update

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import android.net.Uri

/** An app-only update screen. Android still asks the user to approve installation. */
class MandatoryUpdateActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (android.os.Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        ) { finishAffinity() }
        window.statusBarColor = Color.rgb(18, 17, 28)
        window.navigationBarColor = Color.rgb(18, 17, 28)
        val density = resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(28), dp(40), dp(28), dp(40))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(18,17,28), Color.rgb(39,29,62)))
        }
        fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(10)); root.addView(this)
        }
        label("✦  MELOQIS MUSIC", 13f, Color.rgb(199,175,255), true)
        label("Your next chapter\nis ready.", 36f, Color.WHITE, true)
        label("Update to keep listening", 19f, Color.WHITE, true)
        label("This release needs Meloqis ${AppUpdateGate.minimum} or newer. Install the update, then open your music again. Your library stays with you.", 16f, Color.rgb(208,201,224))
        label("Installed ${AppUpdateGate.installed}", 12f, Color.rgb(174,163,196))
        fun action(title: String, block: () -> Unit): Button = Button(this).apply {
            text = title; isAllCaps = false; minHeight = dp(54)
            root.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            setOnClickListener { block() }
        }
        val play = echo.music.iad1tya.BuildKonfig.playStore == 1
        action(if (play) "Update on Google Play" else "Download & install update") {
            if (play) open("https://play.google.com/store/apps/details?id=$packageName")
            else {
                val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull()
                val suffix = when (abi) { "arm64-v8a" -> "-arm64"; "armeabi-v7a" -> "-armeabi-v7a"; "x86_64" -> "-x86_64"; else -> "" }
                echo.music.iad1tya.expect.startUpdateDownload(this, "${AppUpdateGate.RELEASES}/download/Meloqis-Music-latest$suffix.apk")
            }
        }
        action("View release details") { open(AppUpdateGate.RELEASES) }
        action("Check again") { AppUpdateGate.refresh(true) { if (!AppUpdateGate.required()) finish() } }
        action("Close Meloqis") { finishAffinity() }
        setContentView(android.widget.ScrollView(this).apply { isFillViewport = true; addView(root) })
    }
    private fun open(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    @Deprecated("Keep the required update screen visible until installation or exit")
    override fun onBackPressed() { finishAffinity() }
}
