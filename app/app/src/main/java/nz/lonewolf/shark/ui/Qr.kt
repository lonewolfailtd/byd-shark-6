package nz.lonewolf.shark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** A QR code drawn as squares, white on dark so it scans off the head unit screen. */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) { runCatching { QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 33, 33, mapOf(EncodeHintType.MARGIN to 1)) }.getOrNull() }
    Canvas(modifier) {
        drawRect(Color.White)
        val m = matrix ?: return@Canvas
        val cell = size.minDimension / m.width
        for (y in 0 until m.height) for (x in 0 until m.width) if (m.get(x, y)) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
    }
}
