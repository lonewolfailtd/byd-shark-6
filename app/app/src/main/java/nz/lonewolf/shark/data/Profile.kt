package nz.lonewolf.shark.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nz.lonewolf.shark.core.byd.ClimateBridge
import nz.lonewolf.shark.core.byd.SeatBridge
import nz.lonewolf.shark.core.byd.Vehicle
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A named snapshot of everything the app controls. Null fields are left as they are. */
data class Profile(
    val name: String,
    val climateOn: Boolean? = null,
    val driverTemp: Int? = null,
    val passengerTemp: Int? = null,
    val fan: Int? = null,
    val auto: Boolean? = null,
    val recirc: Boolean? = null,
    val compressor: Boolean? = null,
    val frontDemist: Boolean? = null,
    val rearHeat: Boolean? = null,
    val synced: Boolean? = null,
    val driverHeat: SeatBridge.Level? = null,
    val driverVent: SeatBridge.Level? = null,
    val passengerHeat: SeatBridge.Level? = null,
    val passengerVent: SeatBridge.Level? = null,
    val ambientOn: Boolean? = null,
    val ambientColour: Int? = null,
    val ambientBrightness: Int? = null,
    val builtIn: Boolean = false,
    val applyOnStart: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name); put("builtIn", builtIn); put("applyOnStart", applyOnStart)
        climateOn?.let { put("climateOn", it) }; driverTemp?.let { put("driverTemp", it) }
        passengerTemp?.let { put("passengerTemp", it) }; fan?.let { put("fan", it) }
        auto?.let { put("auto", it) }; recirc?.let { put("recirc", it) }; compressor?.let { put("compressor", it) }
        frontDemist?.let { put("frontDemist", it) }; rearHeat?.let { put("rearHeat", it) }; synced?.let { put("synced", it) }
        driverHeat?.let { put("driverHeat", it.name) }; driverVent?.let { put("driverVent", it.name) }
        passengerHeat?.let { put("passengerHeat", it.name) }; passengerVent?.let { put("passengerVent", it.name) }
        ambientOn?.let { put("ambientOn", it) }; ambientColour?.let { put("ambientColour", it) }; ambientBrightness?.let { put("ambientBrightness", it) }
    }

    companion object {
        fun fromJson(o: JSONObject): Profile {
            fun b(k: String) = if (o.has(k)) o.getBoolean(k) else null
            fun i(k: String) = if (o.has(k)) o.getInt(k) else null
            fun l(k: String) = if (o.has(k)) runCatching { SeatBridge.Level.valueOf(o.getString(k)) }.getOrNull() else null
            return Profile(
                name = o.getString("name"), climateOn = b("climateOn"), driverTemp = i("driverTemp"), passengerTemp = i("passengerTemp"),
                fan = i("fan"), auto = b("auto"), recirc = b("recirc"), compressor = b("compressor"), frontDemist = b("frontDemist"),
                rearHeat = b("rearHeat"), synced = b("synced"), driverHeat = l("driverHeat"), driverVent = l("driverVent"),
                passengerHeat = l("passengerHeat"), passengerVent = l("passengerVent"), ambientOn = b("ambientOn"),
                ambientColour = i("ambientColour"), ambientBrightness = i("ambientBrightness"),
                builtIn = o.optBoolean("builtIn", false), applyOnStart = o.optBoolean("applyOnStart", false),
            )
        }

        /** Capture the current vehicle state as a profile. */
        fun capture(name: String, c: ClimateBridge.State?, s: SeatBridge.State?): Profile = Profile(
            name = name,
            climateOn = c?.powerOn, driverTemp = c?.driverTemp, passengerTemp = c?.passengerTemp, fan = c?.fan,
            auto = c?.auto, recirc = c?.recirc, compressor = c?.compressorOn, frontDemist = c?.frontDemist,
            rearHeat = c?.rearHeat, synced = c?.synced,
            driverHeat = s?.driver?.heat, driverVent = s?.driver?.vent, passengerHeat = s?.passenger?.heat, passengerVent = s?.passenger?.vent,
        )

        val builtIns = listOf(
            Profile("Winter", climateOn = true, driverTemp = 24, passengerTemp = 24, auto = true, recirc = false, rearHeat = true,
                driverHeat = SeatBridge.Level.HIGH, driverVent = SeatBridge.Level.OFF, builtIn = true),
            Profile("Summer", climateOn = true, driverTemp = 20, passengerTemp = 20, auto = true, compressor = true, recirc = true,
                driverVent = SeatBridge.Level.HIGH, driverHeat = SeatBridge.Level.OFF, builtIn = true),
            Profile("Boost", climateOn = true, fan = 7, compressor = true, recirc = true, auto = false, builtIn = true),
            Profile("Demist", climateOn = true, frontDemist = true, rearHeat = true, compressor = true, recirc = false, fan = 5, builtIn = true),
            Profile("Camp", climateOn = true, driverTemp = 21, passengerTemp = 21, fan = 1, auto = false, recirc = true, builtIn = true),
        )
    }
}

/** Profiles live in a JSON file in the app's private storage; export copies it to the shared files dir. */
class ProfileStore(private val context: Context) {
    private val file = File(context.filesDir, "profiles.json")
    private val _profiles = MutableStateFlow(load())
    val profiles: StateFlow<List<Profile>> = _profiles

    private fun load(): List<Profile> {
        val saved = runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { Profile.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
        val names = saved.map { it.name }.toSet()
        return Profile.builtIns.filter { it.name !in names } + saved
    }

    private fun persist(list: List<Profile>) {
        _profiles.value = list
        file.writeText(JSONArray().apply { list.forEach { put(it.toJson()) } }.toString(2))
    }

    fun save(p: Profile) = persist(_profiles.value.filter { it.name != p.name } + p)
    fun delete(name: String) = persist(_profiles.value.filter { it.name != name })
    fun setApplyOnStart(name: String) = persist(_profiles.value.map { it.copy(applyOnStart = it.name == name) })
    fun startProfile(): Profile? = _profiles.value.firstOrNull { it.applyOnStart }

    fun exportTo(): File {
        val out = File(context.getExternalFilesDir(null), "profiles-export.json")
        out.writeText(file.readText())
        return out
    }

    /** Push a profile to the vehicle. Each write is independent so one refusal does not stop the rest. */
    fun apply(p: Profile): List<String> {
        val c = Vehicle.climate; val s = Vehicle.seats; val l = Vehicle.lights
        val results = mutableListOf<String>()
        fun run(label: String, block: () -> nz.lonewolf.shark.core.byd.CommandResult) {
            val r = runCatching(block).getOrElse { nz.lonewolf.shark.core.byd.CommandResult(false, label, null, it.message ?: "error") }
            results += "$label: ${if (r.ok) "ok" else r.detail}"
        }
        p.climateOn?.let { run("climate power") { c.power(it) } }
        if (p.climateOn != false) {
            p.auto?.let { run("auto") { c.setAuto(it) } }
            p.driverTemp?.let { run("driver temp") { c.setDriverTemp(it) } }
            p.passengerTemp?.let { run("passenger temp") { c.setPassengerTemp(it) } }
            p.synced?.let { run("sync") { c.setSynced(it) } }
            p.fan?.let { run("fan") { c.setFan(it) } }
            p.compressor?.let { run("A/C") { c.setCompressor(it) } }
            p.recirc?.let { run("recirc") { c.setRecirc(it) } }
            p.frontDemist?.let { run("demist") { c.setFrontDemist(it) } }
            p.rearHeat?.let { run("rear heat") { c.setRearHeat(it) } }
        }
        p.driverHeat?.let { run("driver seat heat") { s.setHeat(SeatBridge.DRIVER, it) } }
        p.driverVent?.let { run("driver seat vent") { s.setVent(SeatBridge.DRIVER, it) } }
        p.passengerHeat?.let { run("passenger seat heat") { s.setHeat(SeatBridge.PASSENGER, it) } }
        p.passengerVent?.let { run("passenger seat vent") { s.setVent(SeatBridge.PASSENGER, it) } }
        p.ambientOn?.let { run("ambient") { l.setAmbientOn(it) } }
        p.ambientColour?.let { run("ambient colour") { l.setColour(it) } }
        p.ambientBrightness?.let { run("ambient brightness") { l.setBrightness(it) } }
        return results
    }
}
