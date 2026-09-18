package nz.lonewolf.shark.data

import android.content.Context
import android.os.Build

/**
 * Remembers the head unit build we were tested on and flags a change. BYD OTAs can rename
 * methods, wipe sideloaded apps and reset ADB, so the first thing to know after an update is
 * that it happened.
 */
class FirmwareWatch(context: Context) {
    private val p = context.getSharedPreferences("firmware", Context.MODE_PRIVATE)
    val current: String = runCatching { prop("ro.product.version.soc.name").ifBlank { Build.DISPLAY } }.getOrDefault(Build.DISPLAY)
    val fingerprint: String = Build.FINGERPRINT
    val tested: String get() = p.getString("tested", "") ?: ""
    val changed: Boolean get() = tested.isNotBlank() && tested != current

    fun markTested() = p.edit().putString("tested", current).apply()

    private fun prop(name: String): String = runCatching {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as String
    }.getOrDefault("")
}
