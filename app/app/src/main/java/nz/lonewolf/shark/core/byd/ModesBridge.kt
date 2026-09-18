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
        val driveName get() = when (operationMode) { 1 -> "Sport"; 2 -> "Eco"; 3 -> "Normal"; null -> "--"; else -> "drive $operationMode" }
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
    /**
     * On the Shark 6 setOperationMode steps to the next mode like the wheel button
     * (Normal 3 to Sport 1 to Eco 2 to Normal) whatever value is sent, so step until the
     * vehicle reports the one asked for.
     */
    fun setDrive(mode: Int): CommandResult {
        var last = CommandResult(true, "setOperationMode", 0, "already there")
        repeat(3) {
            val now = energy.getInt("getOperationMode")
            if (now == mode) return last
            last = energy.call("setOperationMode", mode)
            if (!last.ok) return last
            Thread.sleep(400)
        }
        val after = energy.getInt("getOperationMode")
        return if (after == mode) last else last.copy(ok = false, detail = "vehicle settled on $after")
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
