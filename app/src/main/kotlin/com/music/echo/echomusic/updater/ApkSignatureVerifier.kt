package iad1tya.echo.music.echomusic.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** Verifies that a downloaded update is signed by the app's current signer. */
object ApkSignatureVerifier {
    fun isSignedByCurrentApp(context: Context, apk: File): Boolean {
        if (!apk.isFile) return false
        val packageManager = context.packageManager
        val current = runCatching {
            packageManager.getPackageInfoCompat(context.packageName)
        }.getOrNull() ?: return false
        val candidate = runCatching {
            packageManager.getPackageArchiveInfoCompat(apk.absolutePath)
        }.getOrNull() ?: return false

        if (candidate.packageName != context.packageName) return false
        val trusted = signerDigests(current)
        val downloaded = signerDigests(candidate)
        return trusted.isNotEmpty() && downloaded.isNotEmpty() && downloaded.all(trusted::contains)
    }

    internal fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun signerDigests(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        return signatures.orEmpty().mapTo(mutableSetOf()) { sha256(it.toByteArray()) }
    }

    private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }

    private fun PackageManager.getPackageArchiveInfoCompat(apkPath: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES)
        }
}
