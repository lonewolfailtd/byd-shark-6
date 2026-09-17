package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Climate control through BYDAutoAcDevice, using only the calls BYD's own climate UI
 * makes. Method signatures drift between firmwares so each write tries the known
 * variants in order and every write is checked by reading the value back.
 */
class ClimateBridge(context: Context) {
    val device = BydDevice(context, "android.hardware.bydauto.ac.BYDAutoAcDevice")

    data class State(
        val bound: Boolean,
        val error: String?,
        val powerOn: Boolean?,
        val driverTemp: Int?,
        val passengerTemp: Int?,
        val outsideTemp: Int?,
        val fan: Int?,
        val auto: Boolean?,
        val recirc: Boolean?,
        val compressorOn: Boolean?,
        val maxCool: Boolean?,
        val airOnly: Boolean?,
        val frontDemist: Boolean?,
        val rearHeat: Boolean?,
        val synced: Boolean?,
        val windMode: Int?,
    )

    // Remembered writes for values whose getters are dead on some firmwares.
    @Volatile private var lastFan: Int? = null
    @Volatile private var lastRecirc: Boolean? = null
    @Volatile private var lastRearHeat: Boolean? = null
    @Volatile private var lastAirOnly: Boolean? = null
    @Volatile private var lastSynced: Boolean? = null
    @Volatile private var lastWindMode: Int? = null

    fun read(): State {
        val bound = device.bind()
        fun temp(zone: Int) = (device.getInt("getTemprature", zone) ?: device.getInt("getTemperature", zone))?.takeIf { it in -40..50 }
        val ctrlMode = device.getInt("getAcControlMode")
        val cycle = device.getInt("getAcCycleMode")
        return State(
            bound = bound,
            error = device.bindError,
            powerOn = device.getInt("getAcStartState")?.let { it != 0 },
            driverTemp = temp(ZONE_DRIVER),
            passengerTemp = temp(ZONE_PASSENGER),
            outsideTemp = temp(ZONE_OUTSIDE),
            fan = device.getInt("getAcWindLevel") ?: lastFan,
            auto = ctrlMode?.let { it == autoMode() },
            recirc = cycle?.let { it == recircValue() } ?: lastRecirc,
            compressorOn = device.getInt("getAcCompressorMode")?.let { it != 0 },
            maxCool = device.getInt("getAcMaxCoolingState")?.let { it != 0 },
            airOnly = device.getInt("getAcVentilationState")?.let { it != 0 } ?: lastAirOnly,
            frontDemist = defrost(frontArea())?.let { it != 0 },
            rearHeat = rearHeatState()?.let { it != 0 } ?: lastRearHeat,
            synced = synced(),
            windMode = device.getInt("getAcWindMode") ?: lastWindMode,
        )
    }

    fun power(on: Boolean): CommandResult = if (on) device.firstOk(
        { device.call("start", 0) }, { device.call("start", 1) },
        { device.call("setAcStartState", 1) }, { device.call("setAcStartState", 1, 0) },
    ) else device.firstOk(
        { device.call("stop", 0) }, { device.call("stop", 1) },
        { device.call("setAcStartState", 0) }, { device.call("setAcStartState", 0, 0) },
    )

    fun setDriverTemp(c: Int) = setZoneTemp(ZONE_DRIVER, c)

    fun setPassengerTemp(c: Int): CommandResult {
        if (synced() != false) setSynced(false)
        return setZoneTemp(ZONE_PASSENGER, c)
    }

    private fun setZoneTemp(zone: Int, c: Int): CommandResult {
        val t = c.coerceIn(TEMP_MIN, TEMP_MAX)
        return device.firstOk(
            { device.call("setAcTemperature", zone, t, SOURCE_VOICE, 1) },
            { device.call("setAcTemprature", zone, t, SOURCE_VOICE, 1) },
            { device.call("setAcTemperature", zone, t, SOURCE_UI, 0) },
            { device.call("setTemprature", zone, t) },
            { device.call("setTemperature", zone, t) },
        )
    }

    fun setFan(level: Int): CommandResult {
        val lv = level.coerceIn(FAN_MIN, FAN_MAX)
        if (device.getInt("getAcControlMode") == autoMode()) setAuto(false)
        val r = device.firstOk(
            { device.call("set", DEVICE_AC, FEATURE_WIND_LEVEL, lv) },
            { device.call("set", intArrayOf(FEATURE_WIND_LEVEL), intArrayOf(lv)) },
            { device.call("setAcWindLevel", SOURCE_VOICE, lv) },
            { device.call("setAcWindLevel", SOURCE_UI, lv) },
            { device.call("setAcWindLevel", lv) },
        )
        if (r.ok) lastFan = lv
        return r
    }

    fun setAuto(on: Boolean): CommandResult {
        val mode = if (on) autoMode() else manualMode()
        return verified({ device.getInt("getAcControlMode") }, mode,
            { device.call("setAcControlMode", SOURCE_VOICE, mode) },
            { device.call("setAcControlMode", SOURCE_UI, mode) },
        )
    }

    fun setRecirc(on: Boolean): CommandResult {
        val target = if (on) recircValue() else freshValue()
        val r = verified({ device.getInt("getAcCycleMode") }, target,
            { device.call("setAcCycleMode", SOURCE_VOICE, target) },
            { device.call("setAcCycleMode", SOURCE_UI, target) },
            { device.call("setAcCycleMode", target) },
        )
        if (r.ok) lastRecirc = on
        return r
    }

    fun setCompressor(on: Boolean): CommandResult {
        val v = if (on) 1 else 0
        return device.firstOk(
            { device.call("setAcCompressorMode", SOURCE_VOICE, v) },
            { device.call("setAcCompressorMode", SOURCE_UI, v) },
            { device.call("setAcCompressorMode", v) },
        )
    }

    fun setMaxCool(on: Boolean): CommandResult {
        val v = if (on) 1 else 0
        return device.firstOk({ device.call("setAcMaxCoolingState", v) }, { device.call("setAcMaxCoolingState", v, SOURCE_VOICE) })
    }

    fun setAirOnly(on: Boolean): CommandResult {
        val v = if (on) 1 else 0
        val r = verified({ device.getInt("getAcVentilationState") }, v,
            { device.call("setAcVentilationState", SOURCE_VOICE, v) },
            { device.call("setAcVentilationState", SOURCE_UI, v) },
        )
        if (r.ok) lastAirOnly = on
        return r
    }

    fun setFrontDemist(on: Boolean): CommandResult {
        val area = frontArea()
        val v = if (on) 1 else 0
        return verified({ defrost(area) }, v,
            { device.call("setAcDefrostState", SOURCE_UI, area, v) },
            { device.call("setAcDefrostState", SOURCE_VOICE, area, v) },
            { device.call("setAcDefrostState", area, v, SOURCE_UI) },
            { device.call("setAcDefrostState", v) },
        )
    }

    /** Rear window and wing mirror heaters are one switch on the Shark. */
    fun setRearHeat(on: Boolean): CommandResult {
        val v = if (on) 1 else 0
        device.call("setElectricDefrostState", v)
        val r = device.call("setAcDefrostState", SOURCE_VOICE, rearArea(), v)
        if (r.ok) lastRearHeat = on
        return r
    }

    fun setSynced(on: Boolean): CommandResult {
        val mode = if (on) syncedValue() else separateValue()
        val r = verified({ device.getInt("getAcTemperatureControlMode") }, mode,
            { device.call("setAcTemperatureControlMode", SOURCE_VOICE, mode) },
            { device.call("setAcTemperatureControlMode", SOURCE_UI, mode) },
            { device.call("setAcTemperatureControlMode", mode) },
        )
        if (r.ok) lastSynced = on
        return r
    }

    fun setWindMode(mode: Int): CommandResult {
        val r = device.firstOk(
            { device.call("setAcWindMode", SOURCE_VOICE, mode) },
            { device.call("setAcWindMode", SOURCE_UI, mode) },
            { device.call("setAcWindMode", mode) },
        )
        if (r.ok) lastWindMode = mode
        return r
    }

    /** Face, face and foot, foot, foot and demist. Full defrost is its own button. */
    fun windModes(): List<Int> = listOfNotNull(
        device.constant("AC_WINDMODE_FACE", "AC_WIND_FACE"),
        device.constant("AC_WINDMODE_FACE_FOOT", "AC_WINDMODE_FACEANDFOOT", "AC_WIND_FACE_FOOT"),
        device.constant("AC_WINDMODE_FOOT", "AC_WIND_FOOT"),
        device.constant("AC_WINDMODE_FOOT_DEFROST", "AC_WINDMODE_FOOTANDDEFROST"),
    ).distinct().filter { it != WIND_DEFROST }.ifEmpty { listOf(1, 2, 3, 4) }

    // Write then read back; a write "succeeds" only if the vehicle agrees or cannot say.
    private fun verified(readback: () -> Int?, target: Int, vararg attempts: () -> CommandResult): CommandResult {
        var last: CommandResult? = null
        for (a in attempts) {
            val r = a()
            if (r.detail == "no such method") continue
            Thread.sleep(80)
            val after = readback()
            if (r.ok && (after == null || after == target)) return r
            last = if (r.ok) r.copy(ok = false, detail = "vehicle read back $after, wanted $target") else r
        }
        return last ?: CommandResult.missing("verified write")
    }

    private fun synced(): Boolean? {
        val m = device.getInt("getAcTemperatureControlMode") ?: return lastSynced
        return when (m) { syncedValue() -> true; separateValue() -> false; else -> lastSynced }
    }

    private fun defrost(area: Int) = device.getInt("getAcDefrostState", area) ?: device.getInt("getDefrostState", area)
    private fun rearHeatState() = device.getInt("getElectricDefrostState") ?: device.getInt("getAcRearDefrostState") ?: defrost(rearArea())
    private fun frontArea() = device.constant("AC_DEFROST_AREA_FRONT", "DEFROST_FRONT") ?: 1
    private fun rearArea(): Int { val r = device.constant("AC_DEFROST_AREA_REAR", "DEFROST_REAR") ?: 2; return if (r == frontArea()) 2 else r }
    private fun autoMode() = device.constant("AC_CTRLMODE_AUTO", "AC_CONTROLMODE_AUTO") ?: 0
    private fun manualMode() = device.constant("AC_CTRLMODE_MANUAL", "AC_CONTROLMODE_MANUAL") ?: 1
    private fun recircValue() = device.constant("AC_CYCLEMODE_INLOOP", "AC_CYCLE_IN") ?: 1
    private fun freshValue() = device.constant("AC_CYCLEMODE_OUTLOOP", "AC_CYCLE_OUT") ?: 0
    private fun syncedValue() = device.constant("AC_TEMPCTRL_SEPARATE_OFF", "AC_TEMPCTRLMODE_SEPARATE_OFF") ?: 0
    private fun separateValue() = device.constant("AC_TEMPCTRL_SEPARATE_ON", "AC_TEMPCTRLMODE_SEPARATE_ON") ?: 1

    companion object {
        const val ZONE_DRIVER = 1
        const val ZONE_PASSENGER = 2
        const val ZONE_OUTSIDE = 4
        const val SOURCE_UI = 0
        const val SOURCE_VOICE = 1
        const val TEMP_MIN = 17
        const val TEMP_MAX = 32
        const val FAN_MIN = 1
        const val FAN_MAX = 7
        const val WIND_DEFROST = 0
        const val DEVICE_AC = 1000
        const val FEATURE_WIND_LEVEL = 0x1DE0000C
    }
}
