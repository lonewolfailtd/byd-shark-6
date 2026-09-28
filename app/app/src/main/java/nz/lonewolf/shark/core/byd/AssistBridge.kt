package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Driver assist warning preferences, for the settings memory page.
 *
 * The rule here is "learn, never guess". The app does not know what the numbers mean on a given
 * firmware, so it never invents a value. You set each item the way you like it on BYD's own screen,
 * tap Remember, and the app stores the number the ute reports. On the next start it writes that same
 * number back and reads it again to confirm.
 *
 * Only warning preferences are listed. Emergency braking, stability control, ABS and emergency lane
 * keeping are deliberately not here and must never be added.
 */
class AssistBridge(context: Context) {
    val adas = BydDevice(context, "android.hardware.bydauto.adas.BYDAutoADASDevice")
    val pilot = BydDevice(context, "android.hardware.bydauto.dipilot.BYDAutoDiPilotDevice")

    /** `calls` are tried in order; the first getter that answers decides which setter is used. */
    val instrument = BydDevice(context, "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")

    data class Item(val key: String, val label: String, val note: String, val onAdas: Boolean, val calls: List<Pair<String, String>>, val onInstrument: Boolean = false)

    val items = listOf(
        Item("lane", "Lane departure", "Warning and steering nudge when you drift", false,
            listOf("getLaneAssistType" to "setLaneAssistType", "getDriveDepartureState" to "setDriveDeparturState")),
        Item("laneAdas", "Lane departure type", "Sound, vibration or both", true, listOf("getLDSWType" to "setLDSWType")),
        Item("dms", "Driver monitoring", "The camera that watches the driver", false, listOf("getDmsSwitchStatus" to "setDmsSwitchStatus")),
        Item("fatigue", "Fatigue alert", "Tired driver reminder", false, listOf("getFatigueDetectionAidState" to "setFatigueDetectionAidState")),
        Item("distraction", "Distraction alert", "Eyes off the road reminder", false, listOf("getDistractionDetectionAidState" to "setDistractionDetectionAidState")),
        Item("highBeam", "Auto high beam", "Dips the lights for oncoming traffic", true, listOf("getHMAState" to "setHMAState")),
        Item("speedLimit", "Speed limit warning", "The chime when you pass the limit", false,
            listOf("getIntelligentSpeedLimitAlertState" to "setIntelligentSpeedLimitAlertState", "getISLIWarningLevelSwitchStatus" to "setISLIWarningLevelSwitchStatus")),
        Item("signs", "Traffic sign alerts", "Sign recognition on the cluster", false, listOf("getTrafficSignRecognizeState" to "setTrafficSignRecognizeState")),
        Item("clusterMap", "Map on the driver display", "Full screen navigation in front of you", false, listOf("getINSAdasCenterDisplay" to "setINSAdasCenterDisplay"), onInstrument = true),
        Item("collision", "Collision warning timing", "How early the warning sounds, not the braking", false,
            listOf("getPredictCrashAlert138Mode" to "setPredictCrashAlertMode", "getPredictCrashAlert32DMode" to "setPredictCrashAlertMode")),
    )

    /** Shown on screen for reference. These are read and never written. */
    data class Safety(val autoHold: Int?, val hillDescent: Int?, val stability: Int?, val emergencyBraking: Int?)

    private fun device(item: Item) = when { item.onInstrument -> instrument; item.onAdas -> adas; else -> pilot }

    private fun working(item: Item): Pair<String, String>? = item.calls.firstOrNull { device(item).getInt(it.first) != null }

    fun read(item: Item): Int? = working(item)?.let { device(item).getInt(it.first) }

    fun readAll(): Map<String, Int?> = items.associate { it.key to read(it) }

    fun safety() = Safety(adas.getInt("getAVHState"), adas.getInt("getHDCState"), adas.getInt("getESPState"), adas.getInt("getAEBState"))

    /** Write a value the ute itself reported earlier, then read it back. */
    fun restore(item: Item, value: Int): CommandResult {
        val pair = working(item) ?: return CommandResult(false, item.key, null, "not on this firmware")
        val dev = device(item)
        if (dev.getInt(pair.first) == value) return CommandResult(true, pair.second, 0, "already set")
        val r = dev.call(pair.second, value)
        Thread.sleep(400)
        val after = dev.getInt(pair.first)
        return if (r.ok && after == value) r else r.copy(ok = false, detail = if (r.ok) "ute stayed at $after" else r.detail)
    }
}
