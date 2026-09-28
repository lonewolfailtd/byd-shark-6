package nz.lonewolf.shark.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.LocationManager
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burns a line of text (date, time, speed, position) into the bottom left of an NV12 frame.
 * Only the brightness plane is touched. The text is redrawn once a second, then copied row by row.
 */
class ClipStamp(private val context: Context, private val frameWidth: Int, private val frameHeight: Int, private val label: String) {
    private val stripH = 40
    private val stripW = (frameWidth - 32).coerceAtMost(1100)
    private val bitmap = Bitmap.createBitmap(stripW, stripH, Bitmap.Config.ALPHA_8)
    private val canvas = Canvas(bitmap)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 28f; typeface = Typeface.MONOSPACE; color = 0xFFFFFFFF.toInt() }
    private val mask = ByteArray(stripW * stripH)
    private val maskBuf = ByteBuffer.wrap(mask)
    private val row = ByteArray(stripW)
    private var textW = 0
    private var lastSecond = 0L

    private fun redraw() {
        val text = buildString {
            append(stampFormat.format(Date()))
            speed()?.let { append("  $it km/h") }
            position()?.let { append("  $it") }
            append("  $label")
        }
        bitmap.eraseColor(0)
        canvas.drawText(text, 8f, 30f, paint)
        maskBuf.clear(); bitmap.copyPixelsToBuffer(maskBuf)
        textW = (paint.measureText(text).toInt() + 16).coerceAtMost(stripW)
    }

    private fun speed(): Int? = runCatching { nz.lonewolf.shark.service.VehicleService.telemetry.value?.speedKmh }.getOrNull()

    private fun position(): String? = runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: return null
        if (System.currentTimeMillis() - loc.time > 60_000) return null
        "%.5f, %.5f".format(Locale.US, loc.latitude, loc.longitude)
    }.getOrNull()

    /** `nv12` holds one frame starting at index 0 with no row padding. */
    fun apply(nv12: ByteBuffer) {
        val second = System.currentTimeMillis() / 1000
        if (second != lastSecond) { lastSecond = second; redraw() }
        if (textW <= 0) return
        val x0 = 16
        val y0 = frameHeight - stripH - 12
        val savedPos = nv12.position(); val savedLimit = nv12.limit()
        nv12.limit(nv12.capacity())
        for (y in 0 until stripH) {
            val at = (y0 + y) * frameWidth + x0
            nv12.position(at); nv12.get(row, 0, textW)
            val m = y * stripW
            for (x in 0 until textW) {
                row[x] = if ((mask[m + x].toInt() and 0xFF) > 110) 235.toByte() else ((row[x].toInt() and 0xFF) shr 1).toByte()
            }
            nv12.position(at); nv12.put(row, 0, textW)
        }
        nv12.limit(savedLimit); nv12.position(savedPos)
    }

    companion object {
        private val stampFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
    }
}
