package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Trailer mode, V2L discharge and screen brightness, read from the classes that own them on
 * the Shark 6 (research/device-dump/methods.txt). Reads only, apart from screen brightness
 * which is BYD's own display setting.
 */
class EnergyBridge(context: Context) {
    val setting = BydDevice(context, "android.hardware.bydauto.setting.BYDAutoSettingDevice")
    val charging = BydDevice(context, "android.hardware.bydauto.charging.BYDAutoChargingDevice")
    val instrument = BydDevice(context, "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")

    data class Trailer(val modeState: Int?, val modeCode: Int?, val dragType: Int?, val lightCheck: Int?, val towingProhibited: Int?,
                       val smallLimit: Int?, val middleLimit: Int?, val largeLimit: Int?, val modeMileage: Int?) {
        /** BYD switch convention: 1 on, 2 off. */
        val active: Boolean? get() = modeState?.let { it == 1 }
        val sizeName: String get() = when (dragType) { 1 -> "small"; 2 -> "medium"; 3 -> "large"; 15, 0 -> "none"; null -> "--"; else -> "type $dragType" }
    }

    data class V2L(val toggle: Int?, val carState: Int?, val volts: Int?, val amps: Double?, val energyKwh: Double?, val remainMin: Int?, val limitPercent: Int?,
                   val totalMin: Int?, val campingBalance: Int?) {
        val on: Boolean? get() = toggle?.let { it == 1 } ?: carState?.let { it != 0 }
        val watts: Double? get() = if (volts != null && amps != null) volts * amps else null
    }

    data class State(val trailer: Trailer, val v2l: V2L, val brightness: Int?)

    fun read(): State = State(
        Trailer(
            modeState = setting.getInt("getTrailerModeState"), modeCode = setting.getInt("getTrailerModeCode"), dragType = setting.getInt("getTrailerDragType"),
            lightCheck = setting.getInt("getTrailerLightCheckState"), towingProhibited = setting.getInt("getTowingProhibitedState"),
            smallLimit = setting.getInt("getTrailerSmallLimitWeight"), middleLimit = setting.getInt("getTrailerMiddleLimitWeight"), largeLimit = setting.getInt("getTrailerLargeLimitWeight"),
            modeMileage = setting.getInt("getTrailerModeMileage"),
        ),
        V2L(
            toggle = charging.getInt("getDischargeToggle"), carState = charging.getInt("getCarDischargeState"), volts = charging.getInt("getDischargeVoltage"),
            amps = charging.getDouble("getDischargeElectric"), energyKwh = instrument.getDouble("getDischargeElecEnergy"), remainMin = charging.getInt("getDischargeRemainTime"),
            limitPercent = charging.getInt("getDischargeLimit"), totalMin = charging.getInt("getDischargeTotalTime"), campingBalance = setting.getInt("getCampingBlanceState"),
        ),
        brightness = instrument.getInt("getBacklightBrightness"),
    )

    /** Trailer size as BYD's own screen sets it: 1 small, 2 medium, 3 large. Read back to confirm. */
    fun setTrailerSize(size: Int): CommandResult {
        val r = setting.call("setTrailerDragType", size); Thread.sleep(300)
        val after = setting.getInt("getTrailerDragType")
        return if (r.ok && after == size) r else r.copy(ok = false, detail = if (r.ok) "vehicle reports $after" else r.detail)
    }
    /** Tow mode switch, BYD convention 1 on 2 off. */
    fun setTowMode(on: Boolean): CommandResult {
        val target = if (on) 1 else 2
        val r = setting.call("setTrailerModeState", target); Thread.sleep(300)
        val after = setting.getInt("getTrailerModeState")
        return if (r.ok && after == target) r else r.copy(ok = false, detail = if (r.ok) "vehicle reports $after" else r.detail)
    }

    /** BYD's own display brightness setting. Range is learned from the read value; 0 to 10 is typical on DiLink. */
    fun setBrightness(level: Int): CommandResult = instrument.call("setBacklightBrightness", level)
}
