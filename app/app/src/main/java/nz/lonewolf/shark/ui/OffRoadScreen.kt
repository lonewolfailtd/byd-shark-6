package nz.lonewolf.shark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.core.imu.Inclinometer
import nz.lonewolf.shark.service.VehicleService
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OffRoadScreen(inclinometer: Inclinometer) {
    val r by inclinometer.reading.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val m by VehicleService.modes.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var peakPitch by remember { mutableFloatStateOf(0f) }
    var peakRoll by remember { mutableFloatStateOf(0f) }
    peakPitch = max(peakPitch, abs(r.pitch)); peakRoll = max(peakRoll, abs(r.roll))

    var gps by remember { androidx.compose.runtime.mutableStateOf<android.location.Location?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { gps = withContext(Dispatchers.IO) { Gps.last(ctx) }; kotlinx.coroutines.delay(3000) } }
    Backdrop("bg_offroad", wash = 0.2f) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by remember { androidx.compose.runtime.mutableStateOf("") }
    fun write(label: String, block: () -> nz.lonewolf.shark.core.byd.CommandResult) {
        scope.launch { status = withContext(Dispatchers.IO) { val res = block(); runCatching { VehicleService.modes.value = Vehicle.modes.read() }; if (res.ok) "$label done" else "$label: ${res.detail}" } }
    }
    var sens by remember { androidx.compose.runtime.mutableStateOf(when { inclinometer.smoothing <= 0.07f -> "Smooth"; inclinometer.smoothing >= 0.2f -> "Quick"; else -> "Normal" }) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        // Top: sensitivity and level on the left, page name, BYD shortcuts on the right
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("Sensitivity: $sens", false, Modifier.width(200.dp), height = 52.dp, sub = "tap to change") {
                sens = when (sens) { "Smooth" -> "Normal"; "Normal" -> "Quick"; else -> "Smooth" }
                inclinometer.smoothing = when (sens) { "Smooth" -> 0.05f; "Normal" -> 0.12f; else -> 0.28f }
            }
            Tile("Level here", false, Modifier.width(130.dp), height = 52.dp, sub = "sets zero") { inclinometer.calibrate(); peakPitch = 0f; peakRoll = 0f }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DRIVE", color = Shark.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("ANGLES, MODES AND TERRAIN", color = Shark.muted, fontSize = 11.sp, letterSpacing = 3.sp)
            }
            Tile(when (m?.hillDescentOn) { true -> "Hill descent ON"; false -> "Hill descent"; null -> "Hill descent" }, m?.hillDescentOn == true, Modifier.width(170.dp), height = 52.dp,
                sub = if (m?.hillDescent == null) "not answering" else null) { write("Hill descent") { Vehicle.modes.setHillDescent(m?.hillDescentOn != true) } }
            Tile("Wading", (m?.wadingState ?: 0) > 0, Modifier.width(120.dp), height = 52.dp, sub = "BYD screen") {
                runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage("com.byd.carsettings")?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            Tile("Rage mode", false, Modifier.width(140.dp), height = 52.dp, sub = "BYD screen") {
                runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage("com.byd.dlc.drivingmode")?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        // Middle: the two dials with the photograph showing between them
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TiltDial("Pitch", r.pitch, side = true)
            TiltDial("Roll", r.roll, side = false)
        }
        // Bottom: drive mode on the left, readings in the middle, tyres on the right
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glass(pad = 6) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Drive", color = Shark.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    Tile("Eco", m?.operationMode == 1, Modifier.width(90.dp), height = 52.dp) { write("Eco") { Vehicle.modes.setDrive(1) } }
                    Tile("Normal", m?.operationMode == 3, Modifier.width(100.dp), height = 52.dp) { write("Normal") { Vehicle.modes.setDrive(3) } }
                    Tile("Sport", m?.operationMode == 2, Modifier.width(90.dp), height = 52.dp) { write("Sport") { Vehicle.modes.setDrive(2) } }
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Peaks this trip %.0f° / %.0f°   Speed ${fmt(t?.speedKmh, " km/h")}   Wading limit 700 mm".format(peakPitch, peakRoll), color = Shark.text, fontSize = 13.sp)
                Text(gps?.let { "GPS %.5f, %.5f   %.0f m".format(it.latitude, it.longitude, it.altitude) } ?: "GPS waiting for a fix", color = Shark.text, fontSize = 13.sp)
                Text(if (status.isNotBlank()) status else if (r.sensorName == null) "No accelerometer found" else "Park on flat ground and tap Level here once after fitting", color = if (status.isNotBlank()) androidx.compose.ui.graphics.Color(0xFFFFD54F) else Shark.muted, fontSize = 12.sp)
            }
            Glass(pad = 10) {
                Text("FL ${fmt(t?.tyres?.fl, " psi", 1)}  FR ${fmt(t?.tyres?.fr, " psi", 1)}", color = Shark.text, fontSize = 15.sp)
                Text("RL ${fmt(t?.tyres?.rl, " psi", 1)}  RR ${fmt(t?.tyres?.rr, " psi", 1)}", color = Shark.text, fontSize = 15.sp)
            }
        }
    }
    }
}

/** Round dial with a ute silhouette that tilts with the angle. */
@Composable
private fun TiltDial(label: String, angle: Float, side: Boolean) {
    val warn = abs(angle) >= 25f
    val ute = rememberArt(if (side) "dial_side" else "dial_front")
    val accent = Shark.accent; val bad = Shark.bad
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = if (warn) Shark.bad else Shark.accent, fontSize = 14.sp, letterSpacing = 2.sp)
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(300.dp)) {
                val cx = size.width / 2; val cy = size.height / 2; val rad = size.minDimension / 2 - 8.dp.toPx()
                drawCircle(Color(0xE6000000), rad)
                drawCircle(Color(0xFF223047), rad, style = Stroke(4.dp.toPx()))
                // tick marks every 15 degrees
                for (deg in -45..45 step 15) {
                    rotate(deg.toFloat(), Offset(cx, cy)) {
                        drawLine(Color(0xFF3A4B66), Offset(cx, cy - rad + 4.dp.toPx()), Offset(cx, cy - rad + (if (deg % 45 == 0) 22 else 12).dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                    }
                }
                // level reference stays put; the ute tilts with the angle
                drawLine(Color(0xFF3A4B66), Offset(cx - rad * 0.85f, cy + 34.dp.toPx()), Offset(cx + rad * 0.85f, cy + 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                if (ute != null) {
                    val iw = rad * (if (side) 1.5f else 0.95f); val ih = iw * ute.height / ute.width
                    rotate(if (side) -angle else angle, Offset(cx, cy)) {
                        drawImage(ute, dstOffset = androidx.compose.ui.unit.IntOffset((cx - iw / 2).toInt(), (cy + 34.dp.toPx() - ih).toInt()), dstSize = androidx.compose.ui.unit.IntSize(iw.toInt(), ih.toInt()))
                        drawLine(if (warn) bad else accent, Offset(cx - rad * 0.8f, cy + 34.dp.toPx()), Offset(cx + rad * 0.8f, cy + 34.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                    }
                    return@Canvas
                }
                rotate(-angle, Offset(cx, cy)) {
                    drawLine(if (warn) bad else accent, Offset(cx - rad * 0.8f, cy + 30.dp.toPx()), Offset(cx + rad * 0.8f, cy + 30.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                }
                // ute silhouette
                val w = rad * 0.9f; val h = rad * 0.32f
                val body = Color(0xFFD9E2EF)
                if (side) {
                    drawRoundRect(body, Offset(cx - w / 2, cy - h / 2), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                    drawRoundRect(body, Offset(cx - w / 2 + w * 0.15f, cy - h), androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.55f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                    drawCircle(Color(0xFF0B1220), h * 0.35f, Offset(cx - w * 0.3f, cy + h / 2))
                    drawCircle(Color(0xFF0B1220), h * 0.35f, Offset(cx + w * 0.3f, cy + h / 2))
                } else {
                    val ww = w * 0.6f
                    drawRoundRect(body, Offset(cx - ww / 2, cy - h / 2), androidx.compose.ui.geometry.Size(ww, h), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                    drawRoundRect(body, Offset(cx - ww * 0.35f, cy - h), androidx.compose.ui.geometry.Size(ww * 0.7f, h * 0.55f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
                    drawCircle(Color(0xFF0B1220), h * 0.35f, Offset(cx - ww * 0.4f, cy + h / 2))
                    drawCircle(Color(0xFF0B1220), h * 0.35f, Offset(cx + ww * 0.4f, cy + h / 2))
                }
            }
            Text("%.1f°".format(angle), color = if (warn) Shark.bad else Shark.accent, fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 190.dp))
        }
    }
}
