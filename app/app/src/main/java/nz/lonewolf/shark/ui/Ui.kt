package nz.lonewolf.shark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared look for every page: dark, large touch targets, one accent. */
object Shark {
    val bg = Color(0xFF0B1220)
    val panel = Color(0xFF131C2E)
    val panelLine = Color(0xFF223047)
    val accent = Color(0xFF3DDC84)
    val accentDim = Color(0xFF1F6B45)
    val warm = Color(0xFFFF7043)
    val cool = Color(0xFF42A5F5)
    val text = Color(0xFFF2F5F9)
    val muted = Color(0xFF8FA3BF)
    val bad = Color(0xFFFF5252)
}

@Composable
fun Panel(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Shark.panel)
            .border(1.dp, Shark.panelLine, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(title.uppercase(), color = Shark.muted, fontSize = 13.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/** A big toggle or action tile sized for a finger while driving. */
@Composable
fun Tile(label: String, active: Boolean = false, modifier: Modifier = Modifier, sub: String? = null, height: Dp = 72.dp, onClick: () -> Unit) {
    val bgc = if (active) Shark.accentDim else Color(0xFF1B2638)
    val line = if (active) Shark.accent else Shark.panelLine
    Column(
        modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(bgc)
            .border(1.5.dp, line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (active) Shark.text else Shark.muted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (sub != null) Text(sub, color = Shark.muted, fontSize = 12.sp)
    }
}

/** A value with minus and plus buttons either side. */
@Composable
fun Stepper(label: String, value: String, modifier: Modifier = Modifier, valueColour: Color = Shark.text, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Shark.muted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            RoundButton("−", onMinus)
            Text(value, color = valueColour, fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp), textAlign = TextAlign.Center)
            RoundButton("+", onPlus)
        }
    }
}

@Composable
fun RoundButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF1B2638))
            .border(1.5.dp, Shark.panelLine, RoundedCornerShape(32.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Shark.text, fontSize = 30.sp) }
}

/** Arc gauge. `fraction` in 0..1; null draws an empty arc with a dash. */
@Composable
fun Gauge(label: String, value: String, fraction: Float?, modifier: Modifier = Modifier, colour: Color = Shark.accent, unit: String = "") {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                val inset = 10.dp.toPx()
                val rect = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(Color(0xFF223047), 135f, 270f, false, Offset(inset, inset), rect, style = stroke)
                if (fraction != null) drawArc(colour, 135f, 270f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), rect, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = if (fraction == null) Shark.muted else Shark.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) Text(unit, color = Shark.muted, fontSize = 12.sp)
            }
        }
        Text(label, color = Shark.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatRow(label: String, value: String, ok: Boolean? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ok != null) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (ok) Shark.accent else Shark.bad))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, color = Shark.muted, fontSize = 15.sp, modifier = Modifier.width(220.dp))
        Text(value, color = Shark.text, fontSize = 15.sp)
    }
}

fun fmt(v: Int?, unit: String = "") = if (v == null) "--" else "$v$unit"
fun fmt(v: Double?, unit: String = "", digits: Int = 1) = if (v == null) "--" else "%.${digits}f%s".format(v, unit)
