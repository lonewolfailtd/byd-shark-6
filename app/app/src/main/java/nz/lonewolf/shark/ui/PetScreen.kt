package nz.lonewolf.shark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.service.VehicleService

/** Pet mode state shared by the climate panel and the app shell, so the notice can cover everything. */
object PetMode {
    var active by androidx.compose.runtime.mutableStateOf(false)
    var temp = 21
    var message = ""
    var phone = ""
}

/** The full screen notice for people walking past. Lives on the climate page under Pet mode. */
@Composable
fun PetNotice(temp: Int, message: String, phone: String, onOff: () -> Unit) {
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    // While it is showing, put the climate back if something turns it off.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (VehicleService.climate.value?.powerOn == false) withContext(Dispatchers.IO) { runCatching { Vehicle.climate.power(true) } }
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF05080F)).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("MY PET IS SAFE", color = Shark.accent, fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("The air conditioning is on and set to ${temp}°C", color = Shark.text, fontSize = 32.sp, textAlign = TextAlign.Center)
            if (message.isNotBlank()) Text(message, color = Shark.text, fontSize = 26.sp, textAlign = TextAlign.Center)
            if (phone.isNotBlank()) Text("If you are worried please call $phone", color = Shark.warm, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text("Climate ${if (c?.powerOn == true) "running" else "OFF"}   Outside ${fmt(c?.outsideTemp, "°C")}   Battery ${fmt(t?.soc, "%")}", color = if (c?.powerOn == true) Shark.muted else Shark.bad, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Tile("Turn pet mode off", false, Modifier.width(280.dp), height = 60.dp) { onOff() }
        }
    }
}
