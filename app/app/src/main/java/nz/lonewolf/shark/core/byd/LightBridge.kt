package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Ambient lighting and the cabin light sensor. Open DiKey drives the three ambient bars on
 * the Shark 6 through BYDAutoLightDevice; the exact setter names are confirmed by the
 * Phase 0 method dump, so this bridge tries the documented candidates and reports which
 * one the firmware accepts.
 */
class LightBridge(context: Context) {
    val device = BydDevice(context, "android.hardware.bydauto.light.BYDAutoLightDevice")
    val sensor = BydDevice(context, "android.hardware.bydauto.sensor.BYDAutoSensorDevice")

    data class State(val bound: Boolean, val error: String?, val lightIntensity: Int?, val headlightsOn: Boolean?,
                     val ambientColour: Int?, val ambientBrightness: Int?, val ambientMode: Int?)

    fun read(): State {
        val bound = device.bind()
        return State(
            bound, device.bindError,
            lightIntensity = sensor.getInt("getLightIntensity"),
            headlightsOn = device.getInt("getLightStatus", LOW_BEAM)?.let { it != 0 },
            ambientColour = device.getInt("getAmbientLightColor") ?: device.getInt("getAtmosphereLampColor"),
            ambientBrightness = device.getInt("getAmbientLightBrightness") ?: device.getInt("getAtmosphereLampBrightness"),
            ambientMode = device.getInt("getAmbientLightMode") ?: device.getInt("getAtmosphereLampMode"),
        )
    }

    /** Colour as 0xRRGGBB. Candidate setters from the SDK docs and Open DiKey; first that works wins. */
    fun setAmbientColour(rgb: Int): CommandResult = device.firstOk(
        { device.call("setAmbientLightColor", rgb) },
        { device.call("setAtmosphereLampColor", rgb) },
        { device.call("setAmbientLightColor", (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF) },
        { device.call("setAtmosphereLampColor", (rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF) },
    )

    fun setAmbientBrightness(level: Int): CommandResult = device.firstOk(
        { device.call("setAmbientLightBrightness", level) },
        { device.call("setAtmosphereLampBrightness", level) },
    )

    fun setAmbientMode(mode: Int): CommandResult = device.firstOk(
        { device.call("setAmbientLightMode", mode) },
        { device.call("setAtmosphereLampMode", mode) },
    )

    fun setAmbientOn(on: Boolean): CommandResult = device.firstOk(
        { device.call("setAmbientLightState", if (on) 1 else 0) },
        { device.call("setAtmosphereLampState", if (on) 1 else 0) },
    )

    companion object { const val LOW_BEAM = 2 }
}
