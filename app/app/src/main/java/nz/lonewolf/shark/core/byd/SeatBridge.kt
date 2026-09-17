package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Heated and ventilated seats through BYDAutoSettingDevice (the class that owns them on
 * DiLink 5). Levels are 1 off, 2 low, 3 high. Heat and vent are exclusive in the HAL.
 */
class SeatBridge(context: Context) {
    val device = BydDevice(context, "android.hardware.bydauto.setting.BYDAutoSettingDevice")

    enum class Level(val sdk: Int) { OFF(1), LOW(2), HIGH(3);
        companion object { fun of(v: Int?) = entries.firstOrNull { it.sdk == v } }
    }

    data class Seat(val heat: Level?, val vent: Level?)
    data class State(val bound: Boolean, val error: String?, val driver: Seat, val passenger: Seat,
                     val hasHeat: Boolean?, val hasVent: Boolean?)

    fun read(): State {
        val bound = device.bind()
        fun seat(id: Int) = Seat(Level.of(device.getInt("getSeatHeatingState", id)), Level.of(device.getInt("getSeatVentilatingState", id)))
        return State(bound, device.bindError, seat(DRIVER), seat(PASSENGER),
            hasFeature("driver_seat_heating"), hasFeature("driver_seat_ventilating"))
    }

    fun setHeat(seat: Int, level: Level): CommandResult {
        if (level != Level.OFF) device.call("setSeatVentilatingState", seat, Level.OFF.sdk)
        return device.firstOk(
            { device.call("setSeatHeatingState", seat, level.sdk) },
            { device.call("setSeatHeatingState", seat, level.sdk, 0) },
        )
    }

    fun setVent(seat: Int, level: Level): CommandResult {
        if (level != Level.OFF) device.call("setSeatHeatingState", seat, Level.OFF.sdk)
        return device.firstOk(
            { device.call("setSeatVentilatingState", seat, level.sdk) },
            { device.call("setSeatVentilatingState", seat, level.sdk, 0) },
        )
    }

    private fun hasFeature(name: String): Boolean? {
        val dev = device.instance ?: if (device.bind()) device.instance else return null
        return runCatching {
            val r = dev!!.javaClass.getMethod("hasFeature", String::class.java).invoke(dev, name)
            when (r) { is Boolean -> r; is Number -> r.toInt() != 0; else -> null }
        }.getOrNull()
    }

    companion object { const val DRIVER = 1; const val PASSENGER = 2 }
}
