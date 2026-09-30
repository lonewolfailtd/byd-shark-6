package nz.lonewolf.shark.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nz.lonewolf.shark.core.byd.BydSdkLoader
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.core.imu.Inclinometer
import nz.lonewolf.shark.service.VehicleService

class MainActivity : ComponentActivity() {
    private lateinit var inclinometer: Inclinometer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Vehicle.init(this)
        Art.colour = Vehicle.prefs.uteColour
        Art.plain = Vehicle.prefs.plainSkin
        Art.large = Vehicle.prefs.largeText
        Shark.theme = runCatching { Shark.Theme.valueOf(Vehicle.prefs.theme) }.getOrDefault(Shark.Theme.CYAN)
        inclinometer = Inclinometer(this)
        Thread {
            runCatching { BydSdkLoader.ensure(applicationContext) }
            runCatching { VehicleService.start(applicationContext) }
        }.start()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Shark.accent, background = Shark.bg, surface = Shark.panel)) {
                // Large text scales every font in the app by a fifth and nothing else.
                val base = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(base.density, if (Art.large) base.fontScale * 1.2f else base.fontScale)) {
                    Shell(inclinometer)
                }
            }
        }
    }

    override fun onResume() { super.onResume(); inclinometer.start() }
    override fun onPause() {
        inclinometer.stop()
        // Give the cameras back to BYD's own apps unless the recorder owns them.
        if (!Vehicle.recorder.status.value.recording) Thread { nz.lonewolf.shark.camera.QCarCam.stopAll() }.start()
        super.onPause()
    }
}

private val tabs = listOf("Home", "Climate", "Drive", "Gauges", "Energy", "Towing", "Cameras", "Trips", "Settings")

@Composable
private fun Shell(inclinometer: Inclinometer) {
    var tab by remember { mutableIntStateOf(0) }
    var intro by remember { mutableStateOf(Vehicle.prefs.intro) }
    val running by VehicleService.running.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val rec by Vehicle.recorder.status.collectAsStateWithLifecycle()
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { val f = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US); while (true) { clock = f.format(java.util.Date()); kotlinx.coroutines.delay(5000) } }
    if (intro) { Intro { intro = false }; return }
    if (PetMode.active) { PetNotice(PetMode.temp, PetMode.message, PetMode.phone) { PetMode.active = false }; return }
    Column(Modifier.fillMaxSize().background(Shark.bg)) {
        Row(Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF070B14)).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LONEWOLF", color = Shark.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("SHARK", color = Shark.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
            TopShortcuts()
            if (rec.recording) Text(if (rec.mode == nz.lonewolf.shark.camera.Recorder.Mode.SENTRY) "● Sentry armed" else "● Recording", color = Shark.bad, fontSize = 13.sp, modifier = Modifier.padding(end = 18.dp))
            val linked = running && t != null
            Text(if (linked) "linked" else if (running) "waiting" else "no link", color = if (linked) Shark.accent else Shark.bad, fontSize = 13.sp, modifier = Modifier.padding(end = 18.dp))
            Text(fmt(c?.outsideTemp, "°C"), color = Shark.text, fontSize = 15.sp, modifier = Modifier.padding(end = 18.dp))
            Text(clock, color = Shark.text, fontSize = 15.sp)
        }
        Box(Modifier.weight(1f)) {
            when (tabs[tab]) {
                "Home" -> HomeScreen { name -> tabs.indexOf(name).takeIf { it >= 0 }?.let { tab = it } }
                "Climate" -> ClimateScreen()
                "Drive" -> OffRoadScreen(inclinometer)
                "Gauges" -> GaugesScreen()
                "Energy" -> EnergyScreen()
                "Towing" -> TowingScreen()
                "Cameras" -> CamerasScreen()
                "Trips" -> TripsScreen()
                else -> SettingsScreen(inclinometer)
            }
        }
        Row(Modifier.fillMaxWidth().height(66.dp).background(Color(0xFF070B14)), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, name ->
                val sel = i == tab
                Box(Modifier.weight(1f).fillMaxSize().clickable { tab = i }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(3.dp).fillMaxWidth(0.5f).background(if (sel) Shark.accent else Color.Transparent))
                        TabIcon(name, if (sel) Shark.accent else Shark.text, Modifier.padding(top = 5.dp).size(28.dp))
                        Text(name, color = if (sel) Shark.accent else Shark.text, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

/** The home page shortcuts, small, on every page. */
@Composable
private fun TopShortcuts() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var icons by remember { mutableStateOf<List<Pair<String, androidx.compose.ui.graphics.ImageBitmap>>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            val pkgs = Vehicle.prefs.shortcuts.filter { it.isNotBlank() }
            if (pkgs != icons.map { it.first }) icons = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                pkgs.mapNotNull { p: String ->
                    runCatching {
                        val d = ctx.packageManager.getApplicationIcon(p)
                        val b = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
                        d.setBounds(0, 0, 64, 64); d.draw(android.graphics.Canvas(b))
                        Pair<String, androidx.compose.ui.graphics.ImageBitmap>(p, b.asImageBitmap())
                    }.getOrNull()
                }
            }
            kotlinx.coroutines.delay(3000)
        }
    }
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 18.dp)) {
        icons.forEach { (p, img) ->
            androidx.compose.foundation.Image(img, null, Modifier.size(26.dp).clickable {
                runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage(p)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
    }
}
