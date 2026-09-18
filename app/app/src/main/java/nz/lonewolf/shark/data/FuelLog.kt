package nz.lonewolf.shark.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Fuel estimator and fill up log. Litres left come from the vehicle's fuel percentage and
 * the 60 L tank. Range uses a consumption basis you set, or the real figure measured between
 * two full tank fills. Nothing here writes to the vehicle.
 */
class FuelLog(context: Context) {
    data class Fill(val time: Long, val odometer: Int, val litres: Double?, val toFull: Boolean, val fuelPercentAfter: Int?)

    private val prefs = context.getSharedPreferences("fuel", Context.MODE_PRIVATE)
    private val file = File(context.filesDir, "fuel-log.json")
    private val _fills = MutableStateFlow(load())
    val fills: StateFlow<List<Fill>> = _fills

    val tankLitres = 60.0
    var basis: Double
        get() = prefs.getFloat("basis", 8.5f).toDouble()
        set(v) = prefs.edit().putFloat("basis", v.toFloat()).apply()

    private fun load(): List<Fill> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { i -> val o = a.getJSONObject(i); Fill(o.getLong("t"), o.getInt("o"), if (o.has("l")) o.getDouble("l") else null, o.getBoolean("f"), if (o.has("p")) o.getInt("p") else null) }
    }.getOrDefault(emptyList())

    private fun persist() = file.writeText(JSONArray().apply { _fills.value.forEach { f -> put(JSONObject().apply { put("t", f.time); put("o", f.odometer); f.litres?.let { put("l", it) }; put("f", f.toFull); f.fuelPercentAfter?.let { put("p", it) } }) } }.toString())

    fun filledToFull(odometer: Int?, fuelPercent: Int?) { add(Fill(System.currentTimeMillis(), odometer ?: 0, null, true, fuelPercent)) }
    fun addFuel(litres: Double, odometer: Int?, fuelPercent: Int?) { add(Fill(System.currentTimeMillis(), odometer ?: 0, litres, false, fuelPercent)) }
    fun deleteLast() { _fills.value = _fills.value.dropLast(1); persist() }
    private fun add(f: Fill) { _fills.value = (_fills.value + f).takeLast(200); persist() }

    fun litresLeft(fuelPercent: Int?): Double? = fuelPercent?.let { it / 100.0 * tankLitres }

    /** Real consumption from the last two full tank fills, using the odometer between them and litres added at the second. */
    fun measuredConsumption(): Double? {
        val full = _fills.value.filter { it.toFull }
        if (full.size < 2) return null
        val a = full[full.size - 2]; val b = full.last()
        val km = b.odometer - a.odometer
        // Litres burnt between fills: what the gauge said after the first fill minus before the second, plus any top ups in between.
        val topUps = _fills.value.filter { it.time > a.time && it.time < b.time && !it.toFull }.sumOf { it.litres ?: 0.0 }
        val burnt = if (a.fuelPercentAfter != null && b.fuelPercentAfter != null) (a.fuelPercentAfter - percentBefore(b)) / 100.0 * tankLitres + topUps else return null
        return if (km > 20 && burnt > 0) burnt / km * 100 else null
    }

    /** Fuel percent just before a fill: stored on the previous log row if any, else unknown. */
    private fun percentBefore(fill: Fill): Int = prefs.getInt("before_${fill.time}", fill.fuelPercentAfter ?: 100)
    fun notePercentBefore(time: Long, percent: Int?) { percent?.let { prefs.edit().putInt("before_$time", it).apply() } }

    fun rangeAt(consumption: Double, fuelPercent: Int?): Int? = litresLeft(fuelPercent)?.let { (it / consumption * 100).toInt() }
}
