package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Powered seat movement and memory, mirroring BYD CarSettings (CarSetSeatPresenter):
 *   jog:    SettingDevice.turnSeatHorization / turnSeatHeight / turnSeatbackrest / turnSeatCushion (seat, dir)
 *           dir 1 or 2 selects the direction, 0 stops. Send 0 when the finger lifts.
 *   memory: SettingDevice.saveSeatParamsAll(seat, slot) and resetSeatParams(seat, slot) (recall), slots 1..4.
 *   easy entry: SettingDevice.setDriverSeatAutoReturn(1 on / 2 off).
 *   positions: BodyworkDevice.getDriverSeat*Position() floats (read only here).
 */
class SeatPositionBridge(context: Context) {
    val setting = BydDevice(context, "android.hardware.bydauto.setting.BYDAutoSettingDevice")
    val bodywork = BydDevice(context, "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice")

    enum class Axis(val method: String) {
        FORE_AFT("turnSeatHorization"), HEIGHT("turnSeatHeight"), BACKREST("turnSeatbackrest"), CUSHION("turnSeatCushion")
    }

    data class Positions(val horizontal: Double?, val height: Double?, val backrest: Double?, val cushion: Double?, val easyEntry: Int?, val memoryByEcu: Int?)

    fun read(): Positions = Positions(
        horizontal = bodywork.getDouble("getDriverSeatHorizontalPosition"),
        height = bodywork.getDouble("getDriverSeatHeightPosition"),
        backrest = bodywork.getDouble("getDriverSeatBackrestPosition"),
        cushion = bodywork.getDouble("getDriverSeatSitpointPosition"),
        easyEntry = setting.getInt("getDriverSeatAutoReturn"),
        memoryByEcu = setting.getInt("getPassengerSeatMemoryFunctionCanByECU"),
    )

    /** Start moving. `direction` is 1 or 2 as BYD's own buttons send. */
    fun jog(seat: Int, axis: Axis, direction: Int) = setting.call(axis.method, seat, direction)
    fun stop(seat: Int, axis: Axis) = setting.call(axis.method, seat, 0)

    fun save(seat: Int, slot: Int) = setting.call("saveSeatParamsAll", seat, slot)
    fun recall(seat: Int, slot: Int) = setting.call("resetSeatParams", seat, slot)
    fun setEasyEntry(on: Boolean) = setting.call("setDriverSeatAutoReturn", if (on) 1 else 2)

    companion object { const val DRIVER = 1; const val PASSENGER = 2 }
}
