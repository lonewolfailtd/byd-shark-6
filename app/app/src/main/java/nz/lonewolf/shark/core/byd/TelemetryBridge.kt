package nz.lonewolf.shark.core.byd

import android.content.Context

/**
 * Read only vehicle data. Method names come from the BYD Auto API v1.0.5 and the DiLink 5
 * client in byd-trip-stats. Anything the firmware does not answer is simply null, and the
 * Diagnostics screen shows which is which.
 */
class TelemetryBridge(context: Context) {
    val statistic = BydDevice(context, "android.hardware.bydauto.statistic.BYDAutoStatisticDevice")
    val speed = BydDevice(context, "android.hardware.bydauto.speed.BYDAutoSpeedDevice")
    val engine = BydDevice(context, "android.hardware.bydauto.engine.BYDAutoEngineDevice")
    val energy = BydDevice(context, "android.hardware.bydauto.energy.BYDAutoEnergyDevice")
    val gearbox = BydDevice(context, "android.hardware.bydauto.gearbox.BYDAutoGearboxDevice")
    val bodywork = BydDevice(context, "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice")
    val tyre = BydDevice(context, "android.hardware.bydauto.tyre.BYDAutoTyreDevice")
    val instrument = BydDevice(context, "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice")
    val health = BydDevice(context, "android.hardware.bydauto.vehiclehealth.BYDAutoVehicleHealthDevice")
    val charging = BydDevice(context, "android.hardware.bydauto.charging.BYDAutoChargingDevice")
    val ota = BydDevice(context, "android.hardware.bydauto.ota.BYDAutoOtaDevice")

    val devices: Map<String, BydDevice> get() = mapOf(
        "statistic" to statistic, "speed" to speed, "engine" to engine, "energy" to energy, "gearbox" to gearbox,
        "bodywork" to bodywork, "tyre" to tyre, "instrument" to instrument, "health" to health, "charging" to charging, "ota" to ota,
    )

    data class Tyres(val fl: Double?, val fr: Double?, val rl: Double?, val rr: Double?, val unit: String)

    data class Snapshot(
        val soc: Int?, val usableKwh: Double?, val evRangeKm: Int?, val fuelPercent: Int?, val fuelRangeKm: Int?,
        val combinedRangeKm: Int?, val odometerKm: Int?, val evMileageKm: Int?,
        val speedKmh: Int?, val accelerator: Int?, val brake: Int?, val gear: Int?,
        val engineRpm: Int?, val enginePowerKw: Int?, val coolantC: Int?, val oilLevel: Int?,
        val energyMode: Int?, val operationMode: Int?, val sportMode: Int?,
        val battery12v: Double?, val soh: Int?, val chargingState: Int?, val powerLevel: Int?,
        val steeringAngle: Double?, val vin: String?, val tyres: Tyres,
    )

    fun read(): Snapshot {
        // On the Shark 6 the raw tyre value is psi x 10 (377 = 37.7 psi, spec is 36 psi cold).
        val pressureUnit = "psi"
        fun tyreP(area: Int) = (tyre.getDouble("getTyrePressureValueByType", area) ?: tyre.getInt("getTyrePressureValue", area)?.toDouble())?.let { if (it > 100) it / 10.0 else it }
        return Snapshot(
            soc = statistic.getInt("getElecPercentageValue"),
            usableKwh = statistic.getDouble("getEVRemainingBatteryPower"),
            evRangeKm = statistic.getInt("getElecDrivingRangeValue"),
            fuelPercent = statistic.getInt("getFuelPercentageValue"),
            fuelRangeKm = statistic.getInt("getFuelDrivingRangeValue"),
            combinedRangeKm = statistic.getInt("getDrivingRangeAll"),
            odometerKm = statistic.getInt("getTotalMileageValue"),
            evMileageKm = statistic.getInt("getEVMileageValue"),
            speedKmh = speed.getInt("getCurrentSpeed"),
            accelerator = speed.getInt("getAccelerateDeepness"),
            brake = speed.getInt("getBrakeDeepness"),
            gear = gearbox.getInt("getGear") ?: gearbox.getInt("getGearboxAutoModeType"),
            engineRpm = engine.getInt("getEngineSpeed"),
            enginePowerKw = engine.getInt("getEnginePower"),
            coolantC = statistic.getInt("getWaterTemperature") ?: engine.getInt("getWaterTemperature"),
            oilLevel = engine.getInt("getOilLevel"),
            energyMode = energy.getInt("getEnergyMode"),
            operationMode = energy.getInt("getOperationMode"),
            sportMode = instrument.getInt("getSportModeState"),
            battery12v = ota.getDouble("getBatteryVoltage", 0) ?: bodywork.getDouble("getBatteryVoltageLevel"),
            soh = health.getInt("getBatteryHealthStatus"),
            chargingState = charging.getInt("getChargingState") ?: charging.getInt("getChargeState"),
            powerLevel = bodywork.getInt("getPowerLevel"),
            steeringAngle = bodywork.getDouble("getSteeringWheelValue", 0),
            vin = bodywork.getString("getAutoVIN"),
            // Wheels are numbered from 1 on this ute: 1 front left, 2 front right, 3 rear left, 4 rear right.
            tyres = Tyres(tyreP(1), tyreP(2), tyreP(3), tyreP(4), pressureUnit),
        )
    }
}
