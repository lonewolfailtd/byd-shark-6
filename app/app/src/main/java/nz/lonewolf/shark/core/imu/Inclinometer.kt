package nz.lonewolf.shark.core.imu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pitch and roll from the head unit's own IMU.
 *
 * BYD units expose two accelerometers: a stub (frozen values) that getDefaultSensor()
 * returns, and the real Bosch SMI130 whose name ends in "-iner". We pick by name.
 * A user "level here" calibration removes the mounting angle. Angles are in the
 * vehicle frame: pitch positive nose up, roll positive right side down.
 */
class Inclinometer(private val context: Context) : SensorEventListener {
    data class Reading(val pitch: Float, val roll: Float, val sensorName: String?, val raw: FloatArray)

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = context.getSharedPreferences("inclinometer", Context.MODE_PRIVATE)
    private var sensor: Sensor? = null
    private val filtered = FloatArray(3)
    private var first = true
    var smoothing = 0.12f   // 0.05 smooth, 0.3 twitchy

    private val _reading = MutableStateFlow(Reading(0f, 0f, null, FloatArray(3)))
    val reading: StateFlow<Reading> = _reading

    fun availableSensors(): List<String> = sm.getSensorList(Sensor.TYPE_ACCELEROMETER).map { "${it.name} (${it.vendor})" }

    fun start() {
        val all = sm.getSensorList(Sensor.TYPE_ACCELEROMETER)
        sensor = all.firstOrNull { it.name.contains("iner", true) }
            ?: all.firstOrNull { !it.name.contains("stub", true) }
            ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() = sm.unregisterListener(this)

    /** Remember the current attitude as level. */
    fun calibrate() {
        prefs.edit().putFloat("p0", rawPitch()).putFloat("r0", rawRoll()).apply()
        publish()
    }

    fun clearCalibration() { prefs.edit().clear().apply(); publish() }

    override fun onSensorChanged(e: SensorEvent) {
        val v = orient(e.values)
        if (first) { v.copyInto(filtered); first = false }
        else for (i in 0..2) filtered[i] += smoothing * (v[i] - filtered[i])
        publish()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun publish() {
        _reading.value = Reading(
            pitch = rawPitch() - prefs.getFloat("p0", 0f),
            roll = rawRoll() - prefs.getFloat("r0", 0f),
            sensorName = sensor?.name,
            raw = filtered.copyOf(),
        )
    }

    // Head unit lies in the dash: device Y points up the screen, Z out of the screen toward the driver.
    private fun rawPitch(): Float = Math.toDegrees(atan2(filtered[2].toDouble(), sqrt((filtered[0] * filtered[0] + filtered[1] * filtered[1]).toDouble()))).toFloat()
    private fun rawRoll(): Float = Math.toDegrees(atan2(filtered[0].toDouble(), filtered[1].toDouble())).toFloat()

    /** Fold the rotating screen's orientation back into a fixed vehicle frame. */
    private fun orient(v: FloatArray): FloatArray {
        val rot = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        return when (rot) {
            Surface.ROTATION_90 -> floatArrayOf(-v[1], v[0], v[2])
            Surface.ROTATION_180 -> floatArrayOf(-v[0], -v[1], v[2])
            Surface.ROTATION_270 -> floatArrayOf(v[1], -v[0], v[2])
            else -> floatArrayOf(v[0], v[1], v[2])
        }
    }
}
