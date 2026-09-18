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
        val driveName get() = when (operationMode) { 1 -> "Eco"; 2 -> "Normal"; 3 -> "Sport"; null -> "--"; else -> "drive $operationMode" }
        val terrainName get() = when (roadSurface) { 0, 1 -> "Normal"; 2 -> "Snow"; 3 -> "Sand"; 4 -> "Mud"; 5 -> "Mountain"; null -> "--"; else -> "terrain $roadSurface" }
        val crawlName get() = when (creepState) { 1 -> "on"; 2, 0 -> "off"; null -> "--"; else -> "state $creepState" }
    }

    fun read() = State(
        energyMode = energy.getInt("getEnergyMode"), operationMode = energy.getInt("getOperationMode"), roadSurface = energy.getInt("getRoadSurfaceMode"),
        sportState = instrument.getInt("getSportModeState"), driveMode = setting.getInt("getDriveMode"),
        creepState = setting.getInt("getCreepModeState"), creepWork = setting.getInt("getCreepModeWorkState"),
        wadingState = setting.getInt("getWadingPatternStateTips"), wadingSpeedTip = setting.getInt("getWadingPatternSpeedTips"), wadingSocTip = setting.getInt("getWadingPatternSocTips"),
    )
}
