package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Drive, energy, terrain and crawl modes, read only. The setters exist on this ute
 * (energy.setOperationMode / setEnergyMode / setRoadSurfaceMode, setting.setCreepModeState)
 * but they change the powertrain, so we show state and hand off to BYD's own screen.
 */
class ModesBridge(context: Context) {
    val energy = BydDevice(context, "android.hardware.bydauto.energy.BYDAutoEnergyDevice")
    val setting = BydDevice(context, "android.hardware.bydauto.setting.BYDAutoSettingDevice")
    val instrument = BydDevice(context, "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")

    data class State(val energyMode: Int?, val operationMode: Int?, val roadSurface: Int?, val sportState: Int?, val driveMode: Int?,
                     val creepState: Int?, val creepWork: Int?, val wadingState: Int?, val wadingSpeedTip: Int?, val wadingSocTip: Int?) {
        val energyName get() = when (energyMode) { 0 -> "EV"; 1 -> "HEV"; null -> "--"; else -> "energy $energyMode" }
        /** Confirmed on the ute 18 Sep 2026: 1 Sport, 2 Eco, 3 Normal. */
        /** Confirmed from the cluster 18 Sep 2026: 1 Eco, 2 Normal, 3 Sport. */
        val driveName get() = when (operationMode) { 1 -> "Eco"; 2 -> "Normal"; 3 -> "Sport"; null -> "--"; else -> "mode $operationMode" }
        val terrainName get() = when (roadSurface) { 0, 1 -> "Normal"; 2 -> "Snow"; 3 -> "Sand"; 4 -> "Mud"; 5 -> "Mountain"; null -> "--"; else -> "terrain $roadSurface" }
        val crawlName get() = when (creepState) { 1 -> "on"; 2, 0 -> "off"; null -> "--"; else -> "state $creepState" }
    }

    /** Verified writes: send, wait, read back; report the value the vehicle settled on. */
    private fun verified(readback: () -> Int?, target: Int, write: () -> CommandResult): CommandResult {
        val r = write()
        Thread.sleep(250)
        val after = readback()
        return if (r.ok && after == target) r else r.copy(ok = false, detail = if (r.ok) "vehicle stayed at $after" else r.detail)
    }
    /** One step round the drive modes, like the wheel button; waits for the vehicle to report the change. */
    fun nextDrive(): CommandResult {
        val before = energy.getInt("getOperationMode")
        val r = energy.call("setOperationMode", before ?: 1)
        if (!r.ok) return r
        repeat(12) { Thread.sleep(150); val now = energy.getInt("getOperationMode"); if (now != null && now != before) return r.copy(detail = "now $now") }
        return r.copy(ok = false, detail = "vehicle still reports $before")
    }
    /** Step round (Eco 1, Normal 2, Sport 3) until the vehicle reports the mode asked for. */
    fun setDrive(target: Int): CommandResult {
        var last = CommandResult(true, "setOperationMode", 0, "already there")
        repeat(3) {
            val now = energy.getInt("getOperationMode")
            if (now == target) return last
            last = nextDrive()
            if (!last.ok) return last
        }
        val after = energy.getInt("getOperationMode")
        return if (after == target) last else last.copy(ok = false, detail = "vehicle settled on $after")
    }

    fun setPower(mode: Int) = verified({ energy.getInt("getEnergyMode") }, mode) { energy.call("setEnergyMode", mode) }
    fun setCrawl(on: Boolean) = verified({ setting.getInt("getCreepModeState") }, if (on) 1 else 2) { setting.call("setCreepModeState", if (on) 1 else 2) }

    fun read() = State(
        energyMode = energy.getInt("getEnergyMode"), operationMode = energy.getInt("getOperationMode"), roadSurface = energy.getInt("getRoadSurfaceMode"),
        sportState = instrument.getInt("getSportModeState"), driveMode = setting.getInt("getDriveMode"),
        creepState = setting.getInt("getCreepModeState"), creepWork = setting.getInt("getCreepModeWorkState"),
        wadingState = setting.getInt("getWadingPatternStateTips"), wadingSpeedTip = setting.getInt("getWadingPatternSpeedTips"), wadingSocTip = setting.getInt("getWadingPatternSocTips"),
    )
}
