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
        Shark.theme = runCatching { Shark.Theme.valueOf(Vehicle.prefs.theme) }.getOrDefault(Shark.Theme.GREEN)
        inclinometer = Inclinometer(this)
        Thread {
            runCatching { BydSdkLoader.ensure(applicationContext) }
            runCatching { VehicleService.start(applicationContext) }
        }.start()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Shark.accent, background = Shark.bg, surface = Shark.panel)) {
                Shell(inclinometer)
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

private val tabs = listOf("Home", "Climate", "Off Road", "Gauges", "Fuel", "Towing", "Cameras", "Trips", "Pet", "Settings")

@Composable
private fun Shell(inclinometer: Inclinometer) {
    var tab by remember { mutableIntStateOf(0) }
    val running by VehicleService.running.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val rec by Vehicle.recorder.status.collectAsStateWithLifecycle()
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { val f = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US); while (true) { clock = f.format(java.util.Date()); kotlinx.coroutines.delay(5000) } }
    Column(Modifier.fillMaxSize().background(Shark.bg)) {
        Row(Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF070B14)).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LONEWOLF", color = Shark.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("SHARK", color = Shark.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
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
                "Off Road" -> OffRoadScreen(inclinometer)
                "Gauges" -> GaugesScreen()
                "Fuel" -> FuelScreen()
                "Towing" -> TowingScreen()
                "Cameras" -> CamerasScreen()
                "Trips" -> TripsScreen()
                "Pet" -> PetScreen()
                else -> SettingsScreen(inclinometer)
            }
        }
        Row(Modifier.fillMaxWidth().height(66.dp).background(Color(0xFF070B14)), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, name ->
                val sel = i == tab
                Box(Modifier.weight(1f).fillMaxSize().clickable { tab = i }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(3.dp).fillMaxWidth(0.5f).background(if (sel) Shark.accent else Color.Transparent))
                        TabIcon(name, if (sel) Shark.accent else Shark.muted, Modifier.padding(top = 6.dp).size(26.dp))
                        Text(name, color = if (sel) Shark.accent else Shark.muted, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
