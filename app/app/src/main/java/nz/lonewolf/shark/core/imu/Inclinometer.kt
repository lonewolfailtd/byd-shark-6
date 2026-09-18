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
 * Pitch and roll from the head unit's IMU (STMicro ASM330LHH on the Shark 6).
 *
 * Vehicle frame for a dash mounted screen in landscape: device X points to the
 * passenger side (right), device Y up, device Z out of the glass toward the driver,
 * so vehicle forward is -Z. Pitch is positive nose up, roll positive right side down.
 * "Level here" stores the current angles as zero so the mounting angle cancels out.
 */
class Inclinometer(private val context: Context) : SensorEventListener {
    data class Reading(val pitch: Float, val roll: Float, val sensorName: String?, val raw: FloatArray)

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = context.getSharedPreferences("inclinometer", Context.MODE_PRIVATE)
    private var sensor: Sensor? = null
    private val filtered = FloatArray(3)
    private var first = true
    /** 0.05 smooth, 0.3 twitchy. */
    var smoothing = prefs.getFloat("smoothing", 0.12f)
        set(v) { field = v; prefs.edit().putFloat("smoothing", v).apply() }

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

    fun calibrate() {
        prefs.edit().putFloat("p0", rawPitch()).putFloat("r0", rawRoll()).apply()
        publish()
    }

    fun clearCalibration() { prefs.edit().remove("p0").remove("r0").apply(); publish() }

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

    // The Shark's IMU sits flat inside the unit: gravity reads on Z when level. Nose up tips the
    // body so gravity gains a Y component; right side down gains an X component. Level here removes
    // the mounting offset so only the signs matter, and those are checked on a real slope.
    // Signs confirmed on the ute 18 Sep 2026: nose up positive, driver side down positive.
    private fun rawPitch(): Float = -Math.toDegrees(atan2(filtered[1].toDouble(), filtered[2].toDouble())).toFloat()
    private fun rawRoll(): Float = Math.toDegrees(atan2(filtered[0].toDouble(), filtered[2].toDouble())).toFloat()

    /** Fold the rotating screen's orientation back into the landscape device frame. */
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
