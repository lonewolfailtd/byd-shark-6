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

private val tabs = listOf("Climate", "Gauges", "Off Road", "Towing", "Cameras", "Settings")

@Composable
private fun Shell(inclinometer: Inclinometer) {
    var tab by remember { mutableIntStateOf(0) }
    val running by VehicleService.running.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Shark.bg)) {
        Row(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFF0F182A)), verticalAlignment = Alignment.CenterVertically) {
            Text("LONEWOLF SHARK", color = Shark.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 20.dp))
            tabs.forEachIndexed { i, name ->
                val sel = i == tab
                Box(Modifier.weight(1f).fillMaxSize().clickable { tab = i }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(name, color = if (sel) Shark.accent else Shark.muted, fontSize = 18.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        Box(Modifier.height(3.dp).padding(top = 1.dp).fillMaxWidth(0.6f).background(if (sel) Shark.accent else Color.Transparent))
                    }
                }
            }
            val linked = running && t != null
            Text(if (linked) "linked" else if (running) "waiting" else "no link", color = if (linked) Shark.accent else Shark.bad, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ClimateScreen()
                1 -> GaugesScreen()
                2 -> OffRoadScreen(inclinometer)
                3 -> TowingScreen()
                4 -> CamerasScreen()
                else -> SettingsScreen(inclinometer)
            }
        }
    }
}
