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

    /** Litres of any extra tank fitted. The factory gauge only knows about the stock 60 L. */
    var extendedLitres: Int
        get() = prefs.getInt("extended", 0)
        set(v) = prefs.edit().putInt("extended", v).apply()
    var calibrationPct: Int
        get() = prefs.getInt("calibration", 0)
        set(v) = prefs.edit().putInt("calibration", v).apply()
    val totalLitres: Int get() = tankLitres.toInt() + extendedLitres
    /** True when litres are being counted down from a logged fill instead of read from the gauge. */
    val tracking: Boolean get() = extendedLitres > 0 && prefs.contains("trackLitres")

    private fun gaugeLitres(fuelPercent: Int?): Double? =
        fuelPercent?.let { (it / 100.0 * tankLitres * (1 + calibrationPct / 100.0)).coerceIn(0.0, tankLitres) }

    /** Litres left. Stock tank: from the gauge. Extra tank: counted down from the last fill by engine kilometres. */
    fun remaining(fuelPercent: Int?, odometer: Int?, evKm: Int?, consumption: Double): Double? {
        if (!tracking) return gaugeLitres(fuelPercent)
        val base = prefs.getFloat("trackLitres", 0f).toDouble()
        val km = ((odometer ?: return base) - prefs.getInt("trackOdo", odometer)).coerceAtLeast(0)
        val onBattery = if (evKm != null && prefs.contains("trackEv")) (evKm - prefs.getInt("trackEv", evKm)).coerceIn(0, km) else 0
        val counted = (base - (km - onBattery) * consumption / 100.0).coerceIn(0.0, totalLitres.toDouble())
        // Once the extra tank is empty the factory gauge is the truth again, so never report less than it shows.
        val gauge = gaugeLitres(fuelPercent)
        return if (gauge != null && fuelPercent != null && fuelPercent < 95) maxOf(gauge, minOf(counted, gauge + extendedLitres)) else counted
    }

    private fun track(litres: Double, odometer: Int?, evKm: Int?) {
        prefs.edit().putFloat("trackLitres", litres.toFloat()).putInt("trackOdo", odometer ?: 0).apply {
            if (evKm != null) putInt("trackEv", evKm) else remove("trackEv")
        }.apply()
    }

    fun fillTank(odometer: Int?, evKm: Int?, fuelPercent: Int?) {
        filledToFull(odometer, fuelPercent)
        track(totalLitres.toDouble(), odometer, evKm)
    }

    fun addToTank(litres: Double, leftNow: Double?, odometer: Int?, evKm: Int?, fuelPercent: Int?) {
        addFuel(litres, odometer, fuelPercent)
        track(((leftNow ?: 0.0) + litres).coerceAtMost(totalLitres.toDouble()), odometer, evKm)
    }

    fun reset() {
        prefs.edit().remove("trackLitres").remove("trackOdo").remove("trackEv").apply()
        _fills.value = emptyList(); persist()
    }

    private val samples = java.util.ArrayDeque<IntArray>()   // odometer km, engine km so far, fuel percent

    /** Called every second by the service; keeps one point per kilometre for the rolling figure. */
    fun sample(odometer: Int?, evKm: Int?, fuelPercent: Int?) {
        if (odometer == null || fuelPercent == null) return
        val last = samples.peekLast()
        if (last != null && odometer - last[0] < 1) return
        val engineKm = if (evKm != null) odometer - evKm else odometer
        if (last != null && fuelPercent > last[2] + 3) samples.clear()   // a fill up resets the window
        samples.addLast(intArrayOf(odometer, engineKm, fuelPercent))
        while (samples.size > 400) samples.pollFirst()
    }

    /** L/100 km over roughly the last 50 engine kilometres, null until the gauge has moved enough to trust. */
    fun rollingConsumption(): Double? {
        val newest = samples.peekLast() ?: return null
        var oldest: IntArray? = null
        for (p in samples) { if (newest[1] - p[1] <= 60) { oldest = p; break } }
        val o = oldest ?: samples.peekFirst() ?: return null
        val engineKm = newest[1] - o[1]
        val drop = o[2] - newest[2]
        if (engineKm < 40 || drop < 3) return null
        return drop / 100.0 * tankLitres * (1 + calibrationPct / 100.0) / engineKm * 100
    }

    fun rangeAt(consumption: Double, fuelPercent: Int?): Int? = litresLeft(fuelPercent)?.let { (it / consumption * 100).toInt() }
}
