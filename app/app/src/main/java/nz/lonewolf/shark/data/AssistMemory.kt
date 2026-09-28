package nz.lonewolf.shark.data

import android.content.Context
import nz.lonewolf.shark.core.byd.AssistBridge

/** The driver assist values you asked the app to remember, and whether to put them back on start. */
class AssistMemory(context: Context) {
    private val p = context.getSharedPreferences("assist_memory", Context.MODE_PRIVATE)

    var restoreOnStart: Boolean
        get() = p.getBoolean("restore", false)
        set(v) = p.edit().putBoolean("restore", v).apply()

    var lastReport: String
        get() = p.getString("report", "") ?: ""
        private set(v) = p.edit().putString("report", v).apply()

    fun remembered(key: String): Int? = if (p.contains("v_$key")) p.getInt("v_$key", 0) else null
    fun remember(key: String, value: Int) = p.edit().putInt("v_$key", value).apply()
    fun forget(key: String) = p.edit().remove("v_$key").apply()

    /** Put every remembered value back. Returns one line per item for the page to show. */
    fun restoreAll(bridge: AssistBridge): List<String> {
        val lines = bridge.items.mapNotNull { item ->
            val want = remembered(item.key) ?: return@mapNotNull null
            val r = bridge.restore(item, want)
            "${item.label}: " + if (r.ok) (if (r.detail == "already set") "already right" else "restored") else "not changed (${r.detail})"
        }
        lastReport = lines.joinToString("\n")
        return lines
    }
}
