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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
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
    /** Accent colour the owner picks in Settings. Red matches the Shark cabin trim. */
    enum class Theme(val label: String, val accent: Color, val dim: Color) {
        CYAN("Electric", Color(0xFF19E3FF), Color(0xFF0B5F78)),
        GREEN("Green", Color(0xFF2BFF9A), Color(0xFF14704A)),
        RED("Red", Color(0xFFFF2D3F), Color(0xFF7A1420)),
        BLUE("Blue", Color(0xFF2FA8FF), Color(0xFF15507A)),
        AMBER("Amber", Color(0xFFFFB020), Color(0xFF7A5410)),
        ICE("White", Color(0xFFE8EEF6), Color(0xFF4A5A70)),
    }
    var theme by mutableStateOf(Theme.CYAN)
    /** Glowing edge and dark glass fill shared by every card. */
    val edge get() = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(accent.copy(alpha = 0.75f), accent.copy(alpha = 0.18f)))
    val glass get() = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xD9122038), Color(0xEB060B16)))
    val accent get() = theme.accent
    val accentDim get() = theme.dim
    val warm = Color(0xFFFF7043)
    val cool = Color(0xFF42A5F5)
    val text = Color(0xFFF2F5F9)
    val muted = Color(0xFFA9BCD6)
    /** Warnings go yellow on the red theme so they still stand out. */
    val bad get() = if (theme == Theme.RED) Color(0xFFFFD21F) else Color(0xFFFF5252)
}

@Composable
fun Panel(title: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Shark.glass)
            .border(1.2.dp, Shark.edge, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        if (title != null) {
            Text(title.uppercase(), color = Shark.accent, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/** A big toggle or action tile sized for a finger while driving. */
@Composable
fun Tile(label: String, active: Boolean = false, modifier: Modifier = Modifier, sub: String? = null, height: Dp = 72.dp, onClick: () -> Unit) {
    val bgc = if (active) androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Shark.accent.copy(alpha = 0.85f), Shark.accentDim)) else androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xE61A2A47), Color(0xE60C1424)))
    val line = if (active) Shark.accent else Shark.accent.copy(alpha = 0.35f)
    Column(
        modifier
            .height(if (height < 48.dp) 48.dp else height)   // nothing smaller than a fingertip at arm's length
            .clip(RoundedCornerShape(14.dp))
            .background(bgc)
            .border(1.5.dp, line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = if (active) Color.White else Shark.text, fontSize = 17.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (sub != null) Text(sub, color = if (active) Color.White.copy(alpha = 0.8f) else Shark.muted, fontSize = 12.sp)
    }
}

/** A value with minus and plus buttons either side. */
@Composable
fun Stepper(label: String, value: String, modifier: Modifier = Modifier, valueColour: Color = Shark.text, valueWidth: Dp = 150.dp, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Shark.muted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            RoundButton("−", onMinus)
            Text(value, color = valueColour, fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(valueWidth), textAlign = TextAlign.Center)
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
fun Gauge(label: String, value: String, fraction: Float?, modifier: Modifier = Modifier, colour: Color = Shark.accent, unit: String = "", scale: List<String> = emptyList()) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                val inset = 10.dp.toPx()
                val rect = Size(size.width - inset * 2, size.height - inset * 2)
                drawCircle(Color(0xB3080D18), size.minDimension / 2)
                val mid = Offset(size.width / 2, size.height / 2)
                for (i in 0..18) rotate(-135f + i * 15f, mid) {
                    drawLine(Color(0xFF4A5C7A), Offset(mid.x, inset + 10.dp.toPx()), Offset(mid.x, inset + (if (i % 3 == 0) 20 else 15).dp.toPx()), (if (i % 3 == 0) 2 else 1).dp.toPx())
                }
                drawArc(Color(0xFF223047), 135f, 270f, false, Offset(inset, inset), rect, style = stroke)
                if (fraction != null) {
                    drawArc(colour.copy(alpha = 0.22f), 135f, 270f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), rect, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(colour, 135f, 270f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), rect, style = stroke)
                }
                drawCircle(colour.copy(alpha = 0.5f), size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                // Scale numbers sit just inside the ring; a bright dot marks where the value is.
                val r = size.minDimension / 2 - inset - 26.dp.toPx()
                if (scale.isNotEmpty()) {
                    val paint = android.graphics.Paint().apply { color = 0xFFC9D6E8.toInt(); textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
                    scale.forEachIndexed { i, s ->
                        val a = Math.toRadians((135.0 + 270.0 * i / (scale.size - 1)))
                        drawContext.canvas.nativeCanvas.drawText(s, mid.x + (r * Math.cos(a)).toFloat(), mid.y + (r * Math.sin(a)).toFloat() + 4.dp.toPx(), paint)
                    }
                }
                if (fraction != null) {
                    val a = Math.toRadians(135.0 + 270.0 * fraction.coerceIn(0f, 1f))
                    val rr = size.minDimension / 2 - inset
                    val p = Offset(mid.x + (rr * Math.cos(a)).toFloat(), mid.y + (rr * Math.sin(a)).toFloat())
                    drawCircle(Color.White.copy(alpha = 0.35f), 9.dp.toPx(), p)
                    drawCircle(Color.White, 5.dp.toPx(), p)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = if (fraction == null) Shark.muted else Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) Text(unit, color = Shark.muted, fontSize = 12.sp)
            }
        }
        Text(label.uppercase(), color = Shark.text, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
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
