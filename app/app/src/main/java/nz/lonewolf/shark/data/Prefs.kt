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
    var uteColour: String get() = p.getString("uteColour", "white") ?: "white"; set(v) = p.edit().putString("uteColour", v).apply()
    var theme: String get() = p.getString("theme", "GREEN") ?: "GREEN"; set(v) = p.edit().putString("theme", v).apply()
    /** Six home page shortcut slots, blank when empty. */
    var shortcuts: List<String>
        get() = (p.getString("shortcuts", null) ?: "com.byd.dlc.drivingmode|||||").split("|").let { l -> List(6) { l.getOrElse(it) { "" } } }
        set(v) = p.edit().putString("shortcuts", v.joinToString("|")).apply()
    var petTemp: Int get() = p.getInt("petTemp", 21); set(v) = p.edit().putInt("petTemp", v).apply()
    var petMessage: String get() = p.getString("petMessage", "Back soon") ?: ""; set(v) = p.edit().putString("petMessage", v).apply()
    var petPhone: String get() = p.getString("petPhone", "") ?: ""; set(v) = p.edit().putString("petPhone", v).apply()
    var parkStopSeconds: Int get() = p.getInt("parkStop", 60); set(v) = p.edit().putInt("parkStop", v).apply()
}
