package nz.lonewolf.shark.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nz.lonewolf.shark.core.byd.TelemetryBridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Automatic trip log. A trip starts when the ute leaves Park and ends after a minute back in
 * Park. Odometer and EV kilometres come from the vehicle, so fuel kilometres are the difference.
 * Trips can be marked business for the IRD logbook and exported as CSV.
 */
class TripLog(private val context: Context) {
    data class Trip(
        val id: Long, val start: Long, var end: Long, val odoStart: Int, var odoEnd: Int, val evStart: Int, var evEnd: Int,
        val socStart: Int?, var socEnd: Int?, val fuelStart: Int?, var fuelEnd: Int?, var business: Boolean = false, var note: String = "",
    ) {
        val km: Int get() = (odoEnd - odoStart).coerceAtLeast(0)
        val evKm: Int get() = (evEnd - evStart).coerceAtLeast(0).coerceAtMost(km)
        val fuelKm: Int get() = (km - evKm).coerceAtLeast(0)
        val minutes: Long get() = (end - start) / 60_000
    }

    private val file = File(context.filesDir, "trips.json")
    private val _trips = MutableStateFlow(load())
    val trips: StateFlow<List<Trip>> = _trips
    @Volatile var current: Trip? = null; private set
    private var parkedTicks = 0

    private fun load(): List<Trip> = runCatching {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
            Trip(o.getLong("id"), o.getLong("start"), o.getLong("end"), o.getInt("odoStart"), o.getInt("odoEnd"), o.getInt("evStart"), o.getInt("evEnd"),
                if (o.has("socStart")) o.getInt("socStart") else null, if (o.has("socEnd")) o.getInt("socEnd") else null,
                if (o.has("fuelStart")) o.getInt("fuelStart") else null, if (o.has("fuelEnd")) o.getInt("fuelEnd") else null,
                o.optBoolean("business", false), o.optString("note", "")) }
    }.getOrDefault(emptyList())

    private fun persist() {
        file.writeText(JSONArray().apply { _trips.value.forEach { t -> put(JSONObject().apply {
            put("id", t.id); put("start", t.start); put("end", t.end); put("odoStart", t.odoStart); put("odoEnd", t.odoEnd); put("evStart", t.evStart); put("evEnd", t.evEnd)
            t.socStart?.let { put("socStart", it) }; t.socEnd?.let { put("socEnd", it) }; t.fuelStart?.let { put("fuelStart", it) }; t.fuelEnd?.let { put("fuelEnd", it) }
            put("business", t.business); put("note", t.note)
        }) } }.toString())
    }

    /** Call once a second from the service. */
    @Synchronized
    fun tick(t: TelemetryBridge.Snapshot?) {
        val gear = t?.gear ?: return
        val odo = t.odometerKm ?: return
        val ev = t.evMileageKm ?: 0
        val driving = gear != 1
        val cur = current
        if (cur == null) {
            if (driving) { current = Trip(System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(), odo, odo, ev, ev, t.soc, t.soc, t.fuelPercent, t.fuelPercent); parkedTicks = 0 }
            return
        }
        cur.odoEnd = odo; cur.evEnd = ev; cur.socEnd = t.soc; cur.fuelEnd = t.fuelPercent
        if (driving) { parkedTicks = 0; cur.end = System.currentTimeMillis(); return }
        parkedTicks++
        if (parkedTicks >= 60) finish()
    }

    @Synchronized
    fun finish() {
        val cur = current ?: return
        current = null
        parkedTicks = 0
        if (cur.km >= 1) { _trips.value = (_trips.value + cur).takeLast(1000); persist() }
    }

    fun setBusiness(id: Long, business: Boolean) { _trips.value.firstOrNull { it.id == id }?.let { it.business = business; _trips.value = _trips.value.toList(); persist() } }
    fun setNote(id: Long, note: String) { _trips.value.firstOrNull { it.id == id }?.let { it.note = note; _trips.value = _trips.value.toList(); persist() } }
    fun delete(id: Long) { _trips.value = _trips.value.filter { it.id != id }; persist() }

    fun totals(since: Long = 0): Triple<Int, Int, Int> {
        val l = _trips.value.filter { it.start >= since }
        return Triple(l.sumOf { it.km }, l.filter { it.business }.sumOf { it.km }, l.sumOf { it.evKm })
    }

    /** IRD friendly: date, start, end, odometer start and end, distance, business or private, note. */
    fun exportCsv(): File {
        val f = File(context.getExternalFilesDir(null), "trip-logbook.csv")
        f.writeText("Date,Start,End,Odometer start,Odometer end,Km,EV km,Fuel km,Purpose,Note\n" + _trips.value.joinToString("\n") { t ->
            "${day.format(Date(t.start))},${time.format(Date(t.start))},${time.format(Date(t.end))},${t.odoStart},${t.odoEnd},${t.km},${t.evKm},${t.fuelKm},${if (t.business) "Business" else "Private"},\"${t.note.replace('"', '\'')}\""
        })
        return f
    }

    companion object {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val time = SimpleDateFormat("HH:mm", Locale.US)
    }
}
