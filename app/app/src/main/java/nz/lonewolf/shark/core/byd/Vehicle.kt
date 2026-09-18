package nz.lonewolf.shark.core.byd

import android.content.Context

/** Process wide handle on the vehicle bridges. */
object Vehicle {
    lateinit var climate: ClimateBridge; private set
    lateinit var seats: SeatBridge; private set
    lateinit var lights: LightBridge; private set
    lateinit var telemetry: TelemetryBridge; private set
    lateinit var seatPosition: SeatPositionBridge; private set
    lateinit var recorder: nz.lonewolf.shark.camera.Recorder; private set
    lateinit var prefs: nz.lonewolf.shark.data.Prefs; private set
    lateinit var batteryLog: nz.lonewolf.shark.data.BatteryLog; private set
    lateinit var trips: nz.lonewolf.shark.data.TripLog; private set
    lateinit var profiles: nz.lonewolf.shark.data.ProfileStore; private set
    @Volatile var ready = false; private set

    fun init(context: Context) {
        if (ready) return
        val app = context.applicationContext
        climate = ClimateBridge(app)
        seats = SeatBridge(app)
        lights = LightBridge(app)
        telemetry = TelemetryBridge(app)
        seatPosition = SeatPositionBridge(app)
        recorder = nz.lonewolf.shark.camera.Recorder(app)
        prefs = nz.lonewolf.shark.data.Prefs(app)
        batteryLog = nz.lonewolf.shark.data.BatteryLog(app)
        trips = nz.lonewolf.shark.data.TripLog(app)
        profiles = nz.lonewolf.shark.data.ProfileStore(app)
        ready = true
    }
}
