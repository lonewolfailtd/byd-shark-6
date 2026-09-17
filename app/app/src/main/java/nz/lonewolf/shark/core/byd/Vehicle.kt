package nz.lonewolf.shark.core.byd

import android.content.Context

/** Process wide handle on the vehicle bridges. */
object Vehicle {
    lateinit var climate: ClimateBridge; private set
    lateinit var seats: SeatBridge; private set
    lateinit var lights: LightBridge; private set
    lateinit var telemetry: TelemetryBridge; private set
    @Volatile var ready = false; private set

    fun init(context: Context) {
        if (ready) return
        val app = context.applicationContext
        climate = ClimateBridge(app)
        seats = SeatBridge(app)
        lights = LightBridge(app)
        telemetry = TelemetryBridge(app)
        ready = true
    }
}
