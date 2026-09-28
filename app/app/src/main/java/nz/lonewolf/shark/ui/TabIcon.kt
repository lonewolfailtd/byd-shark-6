package nz.lonewolf.shark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/** Line icons for the tab bar, drawn on a 24 unit grid so they stay sharp at any size. */
@Composable
fun TabIcon(name: String, colour: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val pen = Stroke(1.9f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(colour, p(x1, y1), p(x2, y2), 1.9f * u, StrokeCap.Round)
        fun poly(vararg pts: Float, close: Boolean = false) {
            val path = Path().apply { moveTo(pts[0] * u, pts[1] * u); for (i in 2 until pts.size step 2) lineTo(pts[i] * u, pts[i + 1] * u); if (close) close() }
            drawPath(path, colour, style = pen)
        }
        fun ring(x: Float, y: Float, r: Float) = drawCircle(colour, r * u, p(x, y), style = pen)
        fun dot(x: Float, y: Float, r: Float) = drawCircle(colour, r * u, p(x, y))
        fun box(x: Float, y: Float, w: Float, h: Float, r: Float = 1.5f) = drawRoundRect(colour, p(x, y), Size(w * u, h * u), CornerRadius(r * u), style = pen)
        when (name) {
            "Home" -> { poly(3f, 11f, 12f, 3.5f, 21f, 11f); poly(5.5f, 9.5f, 5.5f, 20f, 18.5f, 20f, 18.5f, 9.5f); poly(10f, 20f, 10f, 14f, 14f, 14f, 14f, 20f) }
            "Climate" -> { ring(12f, 12f, 2f); for (a in 0 until 4) rotate(a * 90f, p(12f, 12f)) { drawArc(colour, 200f, 140f, false, p(8f, 2.5f), Size(8f * u, 8f * u), style = pen) } }
            "Off Road" -> { poly(2f, 20f, 9f, 7f, 13f, 14f, 16f, 10f, 22f, 20f, close = true) }
            "Gauges" -> { drawArc(colour, 150f, 240f, false, p(3f, 4f), Size(18f * u, 18f * u), style = pen); line(12f, 13f, 16.5f, 8.5f); dot(12f, 13f, 1.4f) }
            "Fuel" -> { box(4f, 3.5f, 10f, 17f); line(4f, 10f, 14f, 10f); poly(14f, 8f, 18f, 8f, 19.5f, 10f, 19.5f, 17f); ring(19.5f, 18.2f, 1.2f); line(2.5f, 20.5f, 15.5f, 20.5f) }
            "Towing" -> { poly(2f, 16f, 2f, 11f, 5f, 11f, 7f, 7.5f, 12f, 7.5f, 12f, 11f, 17f, 11f, 17f, 16f); ring(6f, 16.5f, 2f); ring(14f, 16.5f, 2f); line(17f, 14.5f, 21f, 14.5f); dot(21.5f, 14.5f, 1.2f) }
            "Cameras" -> { box(2.5f, 7f, 13f, 10.5f, 2f); poly(15.5f, 10.5f, 21.5f, 7.5f, 21.5f, 17f, 15.5f, 14f) }
            "Trips" -> { ring(6f, 18f, 2.2f); ring(18f, 6f, 2.2f); drawPath(Path().apply { moveTo(8f * u, 17f * u); cubicTo(16f * u, 17f * u, 6f * u, 8f * u, 15.8f * u, 6.6f * u) }, colour, style = pen) }
            "Pet" -> { drawOval(colour, p(7.5f, 12.5f), Size(9f * u, 7.5f * u)); dot(5.5f, 10.5f, 1.9f); dot(9.5f, 6.5f, 1.9f); dot(14.5f, 6.5f, 1.9f); dot(18.5f, 10.5f, 1.9f) }
            else -> cog(colour, u, pen)
        }
    }
}

private fun DrawScope.cog(colour: Color, u: Float, pen: Stroke) {
    val c = Offset(12f * u, 12f * u)
    drawCircle(colour, 3f * u, c, style = pen)
    drawCircle(colour, 6.5f * u, c, style = pen)
    for (a in 0 until 8) rotate(a * 45f, c) { drawLine(colour, Offset(12f * u, 2.5f * u), Offset(12f * u, 5.5f * u), 2.6f * u, StrokeCap.Round) }
}
