package nz.lonewolf.shark.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.CommandResult
import nz.lonewolf.shark.core.byd.SeatBridge
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.service.VehicleService

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

private fun launcherApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.distinct().filter { it != ctx.packageName }
        .mapNotNull { appEntry(ctx, it) }.sortedBy { it.label.lowercase() }
}

private fun appEntry(ctx: Context, pkg: String): AppEntry? = runCatching {
    val pm = ctx.packageManager
    val info = pm.getApplicationInfo(pkg, 0)
    val d = pm.getApplicationIcon(info)
    val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    d.setBounds(0, 0, 96, 96); d.draw(AndroidCanvas(bmp))
    AppEntry(pkg, pm.getApplicationLabel(info).toString(), bmp.asImageBitmap())
}.getOrNull()

/** Everything at a glance, with the climate strip and your own app shortcuts. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onOpenTab: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val s by VehicleService.seats.collectAsStateWithLifecycle()
    val m by VehicleService.modes.collectAsStateWithLifecycle()
    val e by VehicleService.energy.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf("") }
    var bearing by remember { mutableStateOf<Float?>(null) }
    var fixAge by remember { mutableStateOf<Long?>(null) }
    var slots by remember { mutableStateOf(Vehicle.prefs.shortcuts) }
    var entries by remember { mutableStateOf<Map<String, AppEntry>>(emptyMap()) }
    var picking by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val loc = withContext(Dispatchers.IO) { Gps.last(ctx) }
            if (loc != null) { if (loc.hasBearing() && loc.speed > 1f) bearing = loc.bearing; fixAge = System.currentTimeMillis() - loc.time }
            delay(2000)
        }
    }
    LaunchedEffect(slots) {
        entries = withContext(Dispatchers.IO) { slots.filter { it.isNotBlank() }.mapNotNull { p -> appEntry(ctx, p)?.let { p to it } }.toMap() }
    }

    fun write(label: String, block: () -> CommandResult) {
        scope.launch {
            status = withContext(Dispatchers.IO) {
                val r = block()
                runCatching {
                    VehicleService.climate.value = Vehicle.climate.read(); VehicleService.seats.value = Vehicle.seats.read(); VehicleService.modes.value = Vehicle.modes.read()
                }
                if (r.ok) "" else "$label refused: ${r.detail}"
            }
        }
    }
    fun next(level: SeatBridge.Level?) = when (level) { SeatBridge.Level.OFF, null -> SeatBridge.Level.LOW; SeatBridge.Level.LOW -> SeatBridge.Level.HIGH; SeatBridge.Level.HIGH -> SeatBridge.Level.OFF }

    Backdrop("bg_home", wash = 0.2f) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.width(270.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Glass(Modifier.weight(1f).fillMaxHeight()) {
                            Label("Battery"); Big(fmt(t?.soc, "%")); Bar(t?.soc?.let { it / 100f }, Shark.accent)
                            Small("${fmt(t?.evRangeKm)} km on battery")
                        }
                        Glass(Modifier.weight(1f).fillMaxHeight()) {
                            Label("Fuel"); Big(fmt(t?.fuelPercent, "%")); Bar(t?.fuelPercent?.let { it / 100f }, Shark.warm)
                            Small("${fmt(t?.fuelRangeKm)} km of fuel")
                        }
                    }
                    Glass(Modifier.weight(1f).fillMaxWidth()) {
                        Label("Total range"); Big("${fmt(t?.combinedRangeKm)} km", 38); Bar(t?.combinedRangeKm?.let { it / 900f }, Shark.cool)
                        Small("Odometer ${fmt(t?.odometerKm, " km")}")
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Text(if (status.isNotBlank()) status else "", color = Shark.bad, fontSize = 13.sp)
                }
                Column(Modifier.width(300.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1.2f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Glass(Modifier.weight(1f).fillMaxHeight()) { Label("Outside"); Big(fmt(c?.outsideTemp, "°")) }
                        Glass(Modifier.weight(1f).fillMaxHeight()) {
                            Label("Engine")
                            val rpm = t?.engineRpm
                            Big(if (rpm == null) "--" else if (rpm <= 0) "Off" else "$rpm", 26)
                            Small(if (rpm != null && rpm > 0) "rpm" else "running on battery")
                        }
                        Glass(Modifier.weight(1f).fillMaxHeight()) {
                            Compass(bearing, Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                            Small(when { bearing == null -> "drive to set"; (fixAge ?: 0) > 30_000 -> "${Gps.point(bearing!!)} last known"; else -> "${Gps.point(bearing!!)} ${bearing!!.toInt()}°" })
                        }
                    }
                    Glass(Modifier.weight(0.7f).fillMaxWidth().clickable { write("Drive mode") { Vehicle.modes.nextDrive() } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Label("Drive mode"); Small("tap to change") }
                            Text(m?.driveName ?: "--", color = Shark.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Glass(Modifier.weight(1f).fillMaxHeight()) { Label("Power"); Big(m?.energyName ?: "--", 24) }
                        Glass(Modifier.weight(1f).fillMaxHeight()) { Label("Terrain"); Big(m?.terrainName ?: "--", 24) }
                        Glass(Modifier.weight(1f).fillMaxHeight()) { Label("Tow mode"); Big(when (e?.trailer?.active) { true -> "On"; false -> "Off"; null -> "--" }, 24) }
                    }
                }
            }
            // Climate strip: passenger on the left, driver on the right.
            Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TempBox("Passenger", c?.passengerTemp, Modifier.width(200.dp), { write("Passenger temp") { Vehicle.climate.nudgePassengerTemp(+1) } }, { write("Passenger temp") { Vehicle.climate.nudgePassengerTemp(-1) } })
                SeatBox("Heat", s?.passenger?.heat, Shark.warm) { write("Passenger heat") { Vehicle.seats.setHeat(SeatBridge.PASSENGER, next(s?.passenger?.heat)) } }
                SeatBox("Vent", s?.passenger?.vent, Shark.cool) { write("Passenger vent") { Vehicle.seats.setVent(SeatBridge.PASSENGER, next(s?.passenger?.vent)) } }
                Glass(Modifier.weight(1f).fillMaxHeight(), pad = 6) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val on = c?.powerOn == true
                        Tile(if (on) "On" else "Off", on, Modifier.weight(1f), height = 64.dp) { write("Climate power") { Vehicle.climate.power(!on) } }
                        Tile("−", false, Modifier.width(56.dp), height = 64.dp) { write("Fan") { Vehicle.climate.nudgeFan(-1) } }
                        Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) { Small("Fan"); Text(fmt(c?.fan), color = Shark.text, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                        Tile("+", false, Modifier.width(56.dp), height = 64.dp) { write("Fan") { Vehicle.climate.nudgeFan(+1) } }
                        Tile("A/C", c?.compressorOn == true, Modifier.weight(1f), height = 64.dp) { write("A/C") { Vehicle.climate.setCompressor(c?.compressorOn != true) } }
                        Tile("Dual", c?.synced == false, Modifier.weight(1f), height = 64.dp) { write("Dual") { Vehicle.climate.setSynced(c?.synced == false) } }
                        Tile("Recirc", c?.recirc == true, Modifier.weight(1f), height = 64.dp) { write("Recirc") { Vehicle.climate.setRecirc(c?.recirc != true) } }
                    }
                }
                SeatBox("Vent", s?.driver?.vent, Shark.cool) { write("Driver vent") { Vehicle.seats.setVent(SeatBridge.DRIVER, next(s?.driver?.vent)) } }
                SeatBox("Heat", s?.driver?.heat, Shark.warm) { write("Driver heat") { Vehicle.seats.setHeat(SeatBridge.DRIVER, next(s?.driver?.heat)) } }
                TempBox("Driver", c?.driverTemp, Modifier.width(200.dp), { write("Driver temp") { Vehicle.climate.nudgeDriverTemp(+1) } }, { write("Driver temp") { Vehicle.climate.nudgeDriverTemp(-1) } })
            }
            // Shortcuts: tap an empty slot to choose an app, hold a filled one to clear it.
            Row(Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEachIndexed { i, pkg ->
                    val e = entries[pkg]
                    Row(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(Shark.glass).border(1.2.dp, Shark.edge, RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = {
                                    if (e == null) picking = i
                                    else runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                },
                                onLongClick = { slots = slots.toMutableList().also { it[i] = "" }; Vehicle.prefs.shortcuts = slots },
                            ).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        if (e?.icon != null) { Image(e.icon, null, Modifier.size(40.dp)); Spacer(Modifier.width(8.dp)) }
                        Text(e?.label ?: "+ Add app", color = if (e == null) Shark.muted else Shark.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        picking?.let { slot ->
            var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
            LaunchedEffect(slot) { apps = withContext(Dispatchers.IO) { launcherApps(ctx) } }
            Box(Modifier.fillMaxSize().background(Color(0xE6060A12)).clickable { picking = null }.padding(40.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Choose an app for this shortcut", color = Shark.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Tile("Cancel", false, Modifier.width(120.dp), height = 48.dp) { picking = null }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(GridCells.Fixed(5), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(apps, key = { it.pkg }) { a ->
                            Row(
                                Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B2638)).clickable {
                                    slots = slots.toMutableList().also { it[slot] = a.pkg }; Vehicle.prefs.shortcuts = slots; picking = null
                                }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (a.icon != null) Image(a.icon, null, Modifier.size(40.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(a.label, color = Shark.text, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** See through card that lets the photograph show behind the numbers. */
@Composable
fun Glass(modifier: Modifier = Modifier, pad: Int = 12, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Shark.glass).border(1.2.dp, Shark.edge, RoundedCornerShape(16.dp)).padding(pad.dp),
        verticalArrangement = Arrangement.Center, content = content,
    )
}

@Composable private fun Label(text: String) = Text(text.uppercase(), color = Shark.accent, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
@Composable private fun Big(text: String, size: Int = 32, colour: Color = Color.White) = Text(text, color = colour, fontSize = size.sp, fontWeight = FontWeight.Bold, maxLines = 1)
@Composable private fun Small(text: String) = Text(text, color = Shark.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
fun Bar(fraction: Float?, colour: Color, modifier: Modifier = Modifier) {
    Box(modifier.padding(vertical = 5.dp).fillMaxWidth().height(9.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF223047))) {
        if (fraction != null) Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(colour))
    }
}

@Composable
private fun TempBox(label: String, temp: Int?, modifier: Modifier, onUp: () -> Unit, onDown: () -> Unit) {
    Glass(modifier.fillMaxHeight(), pad = 6) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Tile("−", false, Modifier.width(52.dp), height = 56.dp, onClick = onDown)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Small(label)
                Text(fmt(temp, "°"), color = when { temp == null -> Shark.muted; temp <= 19 -> Shark.cool; temp >= 25 -> Shark.warm; else -> Shark.text }, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Tile("+", false, Modifier.width(52.dp), height = 56.dp, onClick = onUp)
        }
    }
}

@Composable
private fun SeatBox(label: String, level: SeatBridge.Level?, colour: Color, onClick: () -> Unit) {
    val n = when (level) { SeatBridge.Level.LOW -> 1; SeatBridge.Level.HIGH -> 2; else -> 0 }
    Column(
        Modifier.width(84.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(if (n > 0) colour.copy(alpha = 0.28f) else Color(0xC20B1220))
            .border(1.5.dp, if (n > 0) colour else Shark.panelLine, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text("Seat", color = Shark.muted, fontSize = 11.sp)
        Text(label, color = if (n > 0) Shark.text else Shark.muted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            repeat(2) { i -> Box(Modifier.width(18.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < n) colour else Color(0xFF223047))) }
        }
    }
}

@Composable
fun Compass(bearing: Float?, modifier: Modifier = Modifier) {
    val accent = Shark.accent
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2
        drawCircle(Color(0xFF3A4B66), r - 2f, c, style = Stroke(3f))
        for (d in 0 until 360 step 30) rotate(d.toFloat(), c) { drawLine(Color(0xFF5A6E8C), Offset(c.x, c.y - r + 4f), Offset(c.x, c.y - r + (if (d % 90 == 0) 14f else 8f)), 2f) }
        // The needle always points to north, so it turns against the way the ute is facing.
        rotate(-(bearing ?: 0f), c) {
            drawPath(Path().apply { moveTo(c.x, c.y - r * 0.7f); lineTo(c.x - r * 0.16f, c.y); lineTo(c.x + r * 0.16f, c.y); close() }, if (bearing == null) Color(0xFF5A6E8C) else accent)
            drawPath(Path().apply { moveTo(c.x, c.y + r * 0.7f); lineTo(c.x - r * 0.16f, c.y); lineTo(c.x + r * 0.16f, c.y); close() }, Color(0xFF3A4B66))
        }
    }
}
