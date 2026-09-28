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

/**
 * Pet mode: sets the cabin climate and shows a notice on the screen for people walking past.
 * It only works while the ute is switched on. The app never holds the ute awake.
 */
@Composable
fun PetScreen() {
    val scope = rememberCoroutineScope()
    val c by VehicleService.climate.collectAsStateWithLifecycle()
    val t by VehicleService.telemetry.collectAsStateWithLifecycle()
    val prefs = Vehicle.prefs
    var temp by remember { mutableIntStateOf(prefs.petTemp) }
    var message by remember { mutableStateOf(prefs.petMessage) }
    var phone by remember { mutableStateOf(prefs.petPhone) }
    var active by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    // While it is active, put the climate back if something turns it off.
    LaunchedEffect(active) {
        while (active) {
            delay(30_000)
            if (VehicleService.climate.value?.powerOn == false) withContext(Dispatchers.IO) { runCatching { Vehicle.climate.power(true) } }
        }
    }

    if (active) {
        Box(Modifier.fillMaxSize().background(Color(0xFF05080F)).padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MY PET IS SAFE", color = Shark.accent, fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("The air conditioning is on and set to ${temp}°C", color = Shark.text, fontSize = 32.sp, textAlign = TextAlign.Center)
                if (message.isNotBlank()) Text(message, color = Shark.text, fontSize = 26.sp, textAlign = TextAlign.Center)
                if (phone.isNotBlank()) Text("If you are worried please call $phone", color = Shark.warm, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text("Climate ${if (c?.powerOn == true) "running" else "OFF"}   Outside ${fmt(c?.outsideTemp, "°C")}   Battery ${fmt(t?.soc, "%")}", color = if (c?.powerOn == true) Shark.muted else Shark.bad, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Tile("Turn pet mode off", false, Modifier.width(280.dp), height = 60.dp) { active = false }
            }
        }
        return
    }

    Backdrop("bg_pet", wash = 0.3f) {
        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(0.8f))
            Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PET MODE", color = Shark.text, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Glass(Modifier.weight(1f)) {
                        Text("CABIN TEMPERATURE", color = Shark.muted, fontSize = 12.sp, letterSpacing = 1.2.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tile("−", false, Modifier.width(64.dp), height = 56.dp) { temp = (temp - 1).coerceAtLeast(17); prefs.petTemp = temp }
                            Text("$temp°", color = Shark.text, fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                            Tile("+", false, Modifier.width(64.dp), height = 56.dp) { temp = (temp + 1).coerceAtMost(26); prefs.petTemp = temp }
                        }
                    }
                    Glass(Modifier.weight(1.4f)) {
                        OutlinedTextField(message, { message = it.take(80); prefs.petMessage = message }, label = { Text("Message for people walking past") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(phone, { phone = it.take(20); prefs.petPhone = phone }, label = { Text("Your phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                }
                Tile("Turn pet mode on", true, Modifier.fillMaxWidth(), height = 64.dp) {
                    scope.launch {
                        status = withContext(Dispatchers.IO) {
                            val results = listOf(
                                Vehicle.climate.power(true), Vehicle.climate.setSynced(true), Vehicle.climate.setDriverTemp(temp),
                                Vehicle.climate.setCompressor(true), Vehicle.climate.setRecirc(false),
                            )
                            runCatching { VehicleService.climate.value = Vehicle.climate.read() }
                            if (results.first().ok) "" else "Climate would not switch on: ${results.first().detail}"
                        }
                        if (status.isBlank()) active = true
                    }
                }
                if (status.isNotBlank()) Text(status, color = Shark.bad, fontSize = 14.sp)
                Glass(Modifier.fillMaxWidth()) {
                    Text("Read this first", color = Shark.warm, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pet mode only works while the ute is switched on and in Park. If the ute switches itself off the air conditioning stops and this app will not keep it awake. " +
                            "Use it for short stops, stay close and check the ute yourself. Never rely on it on a hot day.",
                        color = Shark.text, fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
