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

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            TiltDial("Pitch", r.pitch, side = true)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 40.dp)) {
                Text("Peaks this trip", color = Shark.muted, fontSize = 13.sp)
                Text("%.0f° / %.0f°".format(peakPitch, peakRoll), color = Shark.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Tile("Level here", false, Modifier.width(160.dp), sub = "sets zero") { inclinometer.calibrate(); peakPitch = 0f; peakRoll = 0f }
                Spacer(Modifier.height(8.dp))
                var sens by remember { androidx.compose.runtime.mutableStateOf(when { inclinometer.smoothing <= 0.07f -> "Smooth"; inclinometer.smoothing >= 0.2f -> "Quick"; else -> "Normal" }) }
                Tile("Sensitivity: $sens", false, Modifier.width(200.dp), sub = "tap to change") {
                    sens = when (sens) { "Smooth" -> "Normal"; "Normal" -> "Quick"; else -> "Smooth" }
                    inclinometer.smoothing = when (sens) { "Smooth" -> 0.05f; "Normal" -> 0.12f; else -> 0.28f }
                }
                Spacer(Modifier.height(20.dp))
                Text("Speed ${fmt(t?.speedKmh, " km/h")}   Wading limit 700 mm", color = Shark.muted, fontSize = 13.sp)
                Text("Approach 31°  Ramp 17°  Departure 19°", color = Shark.muted, fontSize = 13.sp)
            }
            TiltDial("Roll", r.roll, side = false)
        }
        Panel("Modes") {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            var status by remember { androidx.compose.runtime.mutableStateOf("") }
            fun write(label: String, block: () -> nz.lonewolf.shark.core.byd.CommandResult) {
                scope.launch { status = withContext(Dispatchers.IO) { val res = block(); runCatching { VehicleService.modes.value = Vehicle.modes.read() }; if (res.ok) "$label done" else "$label: ${res.detail}" } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1.4f)) {
                    Text("Drive: ${m?.driveName ?: "--"}   Power: ${m?.energyName ?: "--"}   Terrain: ${m?.terrainName ?: "--"}   Crawl: ${m?.crawlName ?: "--"}", color = Shark.text, fontSize = 15.sp)
                    Text("raw: operation ${m?.operationMode} energy ${m?.energyMode} terrain ${m?.roadSurface} sport ${m?.sportState} drive ${m?.driveMode} crawl ${m?.creepState}", color = Shark.muted, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Tile("Eco", m?.operationMode == 2, Modifier.width(100.dp), height = 56.dp) { write("Eco") { Vehicle.modes.setDrive(2) } }
                        Tile("Normal", m?.operationMode == 3, Modifier.width(100.dp), height = 56.dp) { write("Normal") { Vehicle.modes.setDrive(3) } }
                        Tile("Sport", m?.operationMode == 1, Modifier.width(100.dp), height = 56.dp) { write("Sport") { Vehicle.modes.setDrive(1) } }
                    }
                    if (status.isNotBlank()) Text(status, color = androidx.compose.ui.graphics.Color(0xFFFFD54F), fontSize = 13.sp)
                    Text("EV/HEV and Crawl stay on the physical buttons and BYD's screen until their numbering is confirmed. Press the EV/HEV button and read the energy number above.", color = Shark.muted, fontSize = 11.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tile("Rage mode", false, Modifier.width(200.dp), height = 56.dp, sub = "BYD screen") {
                        runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage("com.byd.dlc.drivingmode")?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                    Tile("Terrain and wading", false, Modifier.width(200.dp), height = 56.dp, sub = "BYD vehicle settings") {
                        runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage("com.byd.carsettings")?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
            }
        }
        Text(
            if (r.sensorName == null) "No accelerometer found" else "Sensor ${r.sensorName}. Angles read from the head unit, so park on flat ground and tap Level here once after fitting.",
            color = Shark.muted, fontSize = 12.sp,
        )
    }
}

/** Round dial with a ute silhouette that tilts with the angle. */
@Composable
private fun TiltDial(label: String, angle: Float, side: Boolean) {
    val warn = abs(angle) >= 25f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = if (warn) Shark.bad else Shark.accent, fontSize = 14.sp, letterSpacing = 2.sp)
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(300.dp)) {
                val cx = size.width / 2; val cy = size.height / 2; val rad = size.minDimension / 2 - 8.dp.toPx()
                drawCircle(Color(0xFF131C2E), rad)
                drawCircle(Color(0xFF223047), rad, style = Stroke(4.dp.toPx()))
                // tick marks every 15 degrees
                for (deg in -45..45 step 15) {
                    rotate(deg.toFloat(), Offset(cx, cy)) {
                        drawLine(Color(0xFF3A4B66), Offset(cx, cy - rad + 4.dp.toPx()), Offset(cx, cy - rad + (if (deg % 45 == 0) 22 else 12).dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
                    }
                }
                // horizon line rotates with the angle; the ute stays put
                rotate(-angle, Offset(cx, cy)) {
                    drawLine(if (warn) Shark.bad else Shark.accent, Offset(cx - rad * 0.8f, cy + 30.dp.toPx()), Offset(cx + rad * 0.8f, cy + 30.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
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
