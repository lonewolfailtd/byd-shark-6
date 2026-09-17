package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Ambient lighting and the cabin light sensor, using the method names dumped from the
 * Shark 6 itself (research/device-dump/methods.txt). "IAL" is BYD's interior ambient light.
 * Zones and colour encoding are learned from the probe (getAmbientColors gives the palette).
 */
class LightBridge(context: Context) {
    val device = BydDevice(context, "android.hardware.bydauto.light.BYDAutoLightDevice")
    val sensor = BydDevice(context, "android.hardware.bydauto.sensor.BYDAutoSensorDevice")

    data class Zone(val area: Int, val colour: Int?, val brightness: Int?, val multicolourMode: Int?, val multicolourState: Int?)
    data class State(
        val bound: Boolean, val error: String?,
        val lightIntensity: Int?, val headlightsOn: Boolean?, val lowBeam: Int?,
        val ambientState: Int?, val ambientColoursSupport: Int?, val ambientSwitchConfig: Int?,
        val ringColour: Int?, val ringBrightness: Int?, val themeLinked: Int?, val nightWeaken: Int?,
        val palette: Map<Int, List<Int>>, val zones: List<Zone>,
    )

    fun read(): State {
        val bound = device.bind()
        val areas = 0..4
        return State(
            bound, device.bindError,
            lightIntensity = sensor.getInt("getLightIntensity"),
            headlightsOn = device.getInt("getLightStatus", LOW_BEAM)?.let { it != 0 },
            lowBeam = device.getInt("getLightStatus", LOW_BEAM),
            ambientState = device.getInt("getAmbientState"),
            ambientColoursSupport = device.getInt("getAmbientColorsSupport"),
            ambientSwitchConfig = device.getInt("getAmbientSwitchConfig"),
            ringColour = device.getInt("getAmbRingColor"),
            ringBrightness = device.getInt("getAmbRingBrightness"),
            themeLinked = device.getInt("getThemeColorSwitchState"),
            nightWeaken = device.getInt("getAtmospereLightNightWeakenMode"),
            palette = areas.associateWith { a -> intArray("getAmbientColors", a) }.filterValues { it.isNotEmpty() },
            zones = areas.map { a ->
                Zone(a, device.getInt("getIALColor", a), device.getInt("getIALBrightness", a),
                    device.getInt("getAmbientMulticolorMode", a), device.getInt("getAmbientMulticolorState", a))
            },
        )
    }

    fun setAmbientOn(on: Boolean): CommandResult = device.call("setAmbientState", if (on) 1 else 0)

    /**
     * Colour is whatever getIALColor returns for the zone: a palette index or a packed value.
     * Third argument is the source flag BYD's own UI passes; both variants are tried.
     */
    fun setZoneColour(area: Int, colour: Int): CommandResult = device.firstOk(
        { device.call("setIALColor", area, colour, 1) },
        { device.call("setIALColor", area, colour, 0) },
    )

    fun setZoneBrightness(area: Int, level: Int): CommandResult = device.firstOk(
        { device.call("setIALBrightness", area, level, 1) },
        { device.call("setIALBrightness", area, level, 0) },
    )

    fun setMulticolourMode(area: Int, mode: Int): CommandResult = device.call("setAmbientMulticolorMode", area, mode)
    fun setNightWeaken(on: Boolean): CommandResult = device.call("setAtmospereLightNightWeakenMode", if (on) 1 else 0)

    /** Vehicle slope from the sensor device, degrees or tenths, to be calibrated against the IMU. */
    fun slope(): Int? = sensor.getInt("getSlope")

    private fun intArray(name: String, arg: Int): List<Int> {
        val dev = device.instance ?: return emptyList()
        return runCatching {
            (dev.javaClass.getMethod(name, Int::class.javaPrimitiveType).invoke(dev, arg) as? IntArray)?.toList()
        }.getOrNull() ?: emptyList()
    }

    companion object { const val LOW_BEAM = 2 }
}
