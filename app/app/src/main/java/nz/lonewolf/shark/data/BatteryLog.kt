package nz.lonewolf.shark.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One row per start (and every 30 minutes while the ute is on): 12 V, traction battery, odometer.
 * From consecutive rows with the same odometer it works out what was lost while parked, which is
 * the number Shark owners argue about most.
 */
class BatteryLog(context: Context) {
    data class Row(val time: Long, val v12: Double?, val soc: Int?, val odometer: Int?, val fuel: Int?)
    data class ParkedLoss(val hours: Double, val socDrop: Int, val perDay: Double, val start: Long, val end: Long)

    private val file = File(context.filesDir, "battery-log.json")
    private val _rows = MutableStateFlow(load())
    val rows: StateFlow<List<Row>> = _rows

    private fun load(): List<Row> = runCatching {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
            Row(o.getLong("t"), if (o.has("v")) o.getDouble("v") else null, if (o.has("s")) o.getInt("s") else null, if (o.has("o")) o.getInt("o") else null, if (o.has("f")) o.getInt("f") else null) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun record(v12: Double?, soc: Int?, odometer: Int?, fuel: Int?) {
        if (v12 == null && soc == null) return
        val last = _rows.value.lastOrNull()
        // Skip near duplicates: same readings inside 25 minutes.
        if (last != null && System.currentTimeMillis() - last.time < 25 * 60_000L && last.soc == soc && last.odometer == odometer) return
        val rows = (_rows.value + Row(System.currentTimeMillis(), v12, soc, odometer, fuel)).takeLast(2000)
        _rows.value = rows
        file.writeText(JSONArray().apply { rows.forEach { r -> put(JSONObject().apply { put("t", r.time); r.v12?.let { put("v", it) }; r.soc?.let { put("s", it) }; r.odometer?.let { put("o", it) }; r.fuel?.let { put("f", it) } }) } }.toString())
    }

    /** Parked periods: consecutive rows, same odometer, at least 3 hours apart, traction battery dropped. */
    fun parkedLosses(): List<ParkedLoss> {
        val r = _rows.value
        val out = mutableListOf<ParkedLoss>()
        for (i in 1 until r.size) {
            val a = r[i - 1]; val b = r[i]
            if (a.odometer == null || a.odometer != b.odometer || a.soc == null || b.soc == null) continue
            val hours = (b.time - a.time) / 3_600_000.0
            if (hours < 3) continue
            val drop = a.soc - b.soc
            if (drop < 0) continue          // charged while parked
            out += ParkedLoss(hours, drop, drop / hours * 24, a.time, b.time)
        }
        return out.takeLast(30)
    }

    fun averagePerDay(): Double? = parkedLosses().takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.socDrop }.toDouble() / l.sumOf { it.hours } * 24 }

    fun exportCsv(context: Context): File {
        val f = File(context.getExternalFilesDir(null), "battery-log.csv")
        f.writeText("time,v12,soc,odometer,fuel\n" + _rows.value.joinToString("\n") { "${stamp.format(Date(it.time))},${it.v12 ?: ""},${it.soc ?: ""},${it.odometer ?: ""},${it.fuel ?: ""}" })
        return f
    }

    companion object { val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
}
