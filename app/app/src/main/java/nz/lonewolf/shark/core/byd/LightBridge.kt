package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Ambient lighting, exterior light settings and the cabin light sensor.
 *
 * Calls mirror what BYD's own CarSettings app does on the Shark 6 (decompiled, see
 * research/lighting-api.md):
 *   area   = SettingDevice.getIALArea()
 *   colour = SettingDevice.setIALColor(area, sliderValue, 0)   read getIALColor(area)
 *   bright = SettingDevice.setIALBrightness(area, level, 0)    read getIALBrightness(area)
 *   on/off = LightDevice.setAmbientMulticolorState(area, 1 on / 2 off)
 *   mode   = LightDevice.setAmbientMulticolorMode(area, mode)
 *   music  = SettingDevice.setAmbientMusicModeState(area, 1 / 2)
 * BYD's convention for switches is 1 = on, 2 = off.
 */
class LightBridge(context: Context) {
    val device = BydDevice(context, "android.hardware.bydauto.light.BYDAutoLightDevice")
    val setting = BydDevice(context, "android.hardware.bydauto.setting.BYDAutoSettingDevice")
    val sensor = BydDevice(context, "android.hardware.bydauto.sensor.BYDAutoSensorDevice")

    data class State(
        val bound: Boolean, val error: String?,
        val lightIntensity: Int?, val headlightsOn: Boolean?,
        val area: Int?, val colour: Int?, val brightness: Int?,
        val on: Boolean?, val mode: Int?, val musicMode: Boolean?,
        val ambientSupport: Int?, val colourSupport: Int?,
        val welcomeLight: Int?, val leaveHomeDelay: Int?, val backHomeDelay: Int?,
        val cargoLight: Int?, val drl: Int?, val frontFog: Int?, val rearFog: Int?, val headlightMode: Int?,
    )

    private fun area(): Int = setting.getInt("getIALArea") ?: 0

    fun read(): State {
        val bound = device.bind() && setting.bind()
        val a = area()
        return State(
            bound, device.bindError ?: setting.bindError,
            lightIntensity = sensor.getInt("getLightIntensity"),
            headlightsOn = device.getInt("getLightStatus", LOW_BEAM)?.let { it != 0 },
            area = a,
            colour = setting.getInt("getIALColor", a),
            brightness = setting.getInt("getIALBrightness", a),
            on = device.getInt("getAmbientMulticolorState", a)?.let { it == ON },
            mode = device.getInt("getAmbientMulticolorMode", a),
            musicMode = setting.getInt("getAmbientMusicModeState", a)?.let { it == ON },
            ambientSupport = setting.getInt("getAmbientLightSupport"),
            colourSupport = setting.getInt("getAmbientLightColorSupport"),
            welcomeLight = setting.getInt("getSmartWelcomeLightState"),
            leaveHomeDelay = setting.getInt("getLeftHomeLightDelayValue"),
            backHomeDelay = setting.getInt("getBackHomeLightDelayValue"),
            cargoLight = device.getInt("getCargoLightSwitchState"),
            drl = device.getInt("getDayTimeLightState"),
            frontFog = device.getInt("getFrontFogLightSwitchState"),
            rearFog = device.getInt("getRearFogLightSwitchState"),
            headlightMode = device.getInt("getHeadlightControlMode"),
        )
    }

    fun setAmbientOn(on: Boolean) = device.call("setAmbientMulticolorState", area(), if (on) ON else OFF)
    fun setColour(value: Int) = setting.call("setIALColor", area(), value, 0)
    fun setBrightness(level: Int) = setting.call("setIALBrightness", area(), level, 0)
    fun setMode(mode: Int) = device.call("setAmbientMulticolorMode", area(), mode)
    fun setMusicMode(on: Boolean) = setting.call("setAmbientMusicModeState", area(), if (on) ON else OFF)

    fun nudgeColour(delta: Int): CommandResult {
        val cur = setting.getInt("getIALColor", area()) ?: 0
        return setColour((cur + delta).coerceAtLeast(0))
    }

    fun nudgeBrightness(delta: Int): CommandResult {
        val cur = setting.getInt("getIALBrightness", area()) ?: 0
        return setBrightness((cur + delta).coerceAtLeast(0))
    }

    // Exterior and convenience lights that BYD's own settings screen exposes.
    fun setWelcomeLight(on: Boolean) = setting.call("setSmartWelcomeLightState", if (on) ON else OFF)
    fun setCargoLight(on: Boolean) = device.call("setCargoLightSwitchState", if (on) ON else OFF)
    fun setDaytimeRunningLights(on: Boolean) = device.call("setDayTimeLightState", if (on) ON else OFF)
    fun setFrontFog(on: Boolean) = device.call("setFrontFogLightSwitchState", if (on) ON else OFF)
    fun setRearFog(on: Boolean) = device.call("setRearFogLightSwitchState", if (on) ON else OFF)

    /** Vehicle slope from the sensor device; returned null so far on the Shark 6. */
    fun slope(): Int? = sensor.getInt("getSlope")

    companion object { const val LOW_BEAM = 2; const val ON = 1; const val OFF = 2 }
}
