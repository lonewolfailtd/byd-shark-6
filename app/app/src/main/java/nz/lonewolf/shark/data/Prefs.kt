package nz.lonewolf.shark.data

import android.content.Context

/** Small settings the service reads every tick. */
class Prefs(context: Context) {
    private val p = context.getSharedPreferences("shark_prefs", Context.MODE_PRIVATE)
    var autoRecord: Boolean get() = p.getBoolean("autoRecord", false); set(v) = p.edit().putBoolean("autoRecord", v).apply()
    var autoSentry: Boolean get() = p.getBoolean("autoSentry", false); set(v) = p.edit().putBoolean("autoSentry", v).apply()
    var floatingPanel: Boolean get() = p.getBoolean("floatingPanel", true); set(v) = p.edit().putBoolean("floatingPanel", v).apply()
    /** Size limit for rolling clips. Protected clips sit outside it. */
    var storageCapGb: Int get() = p.getInt("storageCapGb", 8); set(v) = p.edit().putInt("storageCapGb", v).apply()
    var clipStamp: Boolean get() = p.getBoolean("clipStamp", true); set(v) = p.edit().putBoolean("clipStamp", v).apply()
    var parkStopSeconds: Int get() = p.getInt("parkStop", 60); set(v) = p.edit().putInt("parkStop", v).apply()
}
