package nz.lonewolf.shark.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The photographs behind each page, one set per paint colour. */
object Art {
    val colours = listOf("white" to "White", "grey" to "Grey", "black" to "Black", "blue" to "Blue", "orange" to "Orange")
    var colour by mutableStateOf("white")

    private val cache = LinkedHashMap<String, ImageBitmap>()

    /** Falls back to the white image, then to nothing, so a missing file never breaks a page. */
    fun load(context: Context, name: String, coloured: Boolean, colour: String): ImageBitmap? {
        val candidates = if (coloured) listOf("${name}_$colour", "${name}_white") else listOf(name)
        for (res in candidates) {
            synchronized(cache) { cache[res] }?.let { return it }
            val id = context.resources.getIdentifier(res, "drawable", context.packageName)
            if (id == 0) continue
            val bmp = runCatching { BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false }) }.getOrNull() ?: continue
            val img = bmp.asImageBitmap()
            synchronized(cache) { cache[res] = img; while (cache.size > 6) cache.remove(cache.keys.first()) }
            return img
        }
        return null
    }
}

@Composable
fun rememberArt(name: String, coloured: Boolean = true): ImageBitmap? {
    val ctx = LocalContext.current
    val colour = Art.colour
    val img by produceState<ImageBitmap?>(null, name, colour) { value = withContext(Dispatchers.IO) { Art.load(ctx, name, coloured, colour) } }
    return img
}

/** A page background: the photograph, then a dark wash so text stays readable. */
@Composable
fun Backdrop(name: String, coloured: Boolean = true, wash: Float = 0.35f, content: @Composable () -> Unit) {
    val img = rememberArt(name, coloured)
    Box(Modifier.fillMaxSize().background(Shark.bg)) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.Center)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = wash + 0.25f), Color.Black.copy(alpha = wash * 0.4f), Color.Black.copy(alpha = wash + 0.3f)))))
        content()
    }
}

object Gps {
    fun last(context: Context): Location? = runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }.getOrNull()

    fun point(bearing: Float): String = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[(((bearing % 360) + 360 + 22.5f) % 360 / 45f).toInt() % 8]
}
