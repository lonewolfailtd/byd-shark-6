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

    /** What we last set, shown until the ute reports a value (the passenger getter answers nothing on this ute). */
    private val lastSet = HashMap<String, Pair<Level, Long>>()

    private fun remembered(key: String, reported: Level?): Level? {
        val mine = lastSet[key] ?: return reported
        // Trust the ute once it answers; otherwise keep showing what was pressed.
        return if (reported != null && System.currentTimeMillis() - mine.second > 4_000) reported else mine.first
    }

    fun read(): State {
        val bound = device.bind()
        fun heat(id: Int) = Level.of(device.getInt("getSeatHeatingState", id)
            ?: (if (id == PASSENGER) device.getInt("getPassengerSeatHeating3caState") ?: device.getInt("getSeatHeatingState2", id) ?: device.getInt("getSeatHeatingState1", id) else null))
        fun vent(id: Int) = Level.of(device.getInt("getSeatVentilatingState", id)
            ?: (if (id == PASSENGER) device.getInt("getPassengerSeatVentilating3caState") ?: device.getInt("getSeatVentilatingState2", id) else null))
        fun seat(id: Int) = Seat(remembered("h$id", heat(id)), remembered("v$id", vent(id)))
        return State(bound, device.bindError, seat(DRIVER), seat(PASSENGER),
            hasFeature("driver_seat_heating"), hasFeature("driver_seat_ventilating"))
    }

    fun setHeat(seat: Int, level: Level): CommandResult {
        if (level != Level.OFF) { device.call("setSeatVentilatingState", seat, Level.OFF.sdk); lastSet["v$seat"] = Level.OFF to System.currentTimeMillis() }
        val r = device.firstOk(
            { device.call("setSeatHeatingState", seat, level.sdk) },
            { device.call("setSeatHeatingState", seat, level.sdk, 0) },
        )
        if (r.ok) lastSet["h$seat"] = level to System.currentTimeMillis()
        return r
    }

    fun setVent(seat: Int, level: Level): CommandResult {
        if (level != Level.OFF) { device.call("setSeatHeatingState", seat, Level.OFF.sdk); lastSet["h$seat"] = Level.OFF to System.currentTimeMillis() }
        val r = device.firstOk(
            { device.call("setSeatVentilatingState", seat, level.sdk) },
            { device.call("setSeatVentilatingState", seat, level.sdk, 0) },
        )
        if (r.ok) lastSet["v$seat"] = level to System.currentTimeMillis()
        return r
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
