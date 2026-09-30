package nz.lonewolf.shark.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Two and a half seconds when the app opens: the ute fades up out of black with a slow push in,
 * the name lands letter group by letter group, a light sweeps the accent line, then it all
 * dissolves into the home page. Tap anywhere to skip.
 */
@Composable
fun Intro(onDone: () -> Unit) {
    val ute = rememberArt("bg_home")
    val photo = remember { Animatable(0f) }
    val zoom = remember { Animatable(1.12f) }
    val wolf = remember { Animatable(0f) }
    val shark = remember { Animatable(0f) }
    val line = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val whole = remember { Animatable(1f) }
    val accent = Shark.accent

    LaunchedEffect(Unit) {
        launch { photo.animateTo(1f, tween(900, easing = LinearEasing)) }
        launch { zoom.animateTo(1f, tween(2600, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { wolf.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        delay(250)
        launch { shark.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        delay(300)
        launch { line.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        delay(250)
        launch { tag.animateTo(1f, tween(400)) }
        delay(900)
        whole.animateTo(0f, tween(450))
        onDone()
    }

    Box(Modifier.fillMaxSize().alpha(whole.value).background(Color.Black).clickable { onDone() }) {
        if (ute != null) Image(ute, null, Modifier.fillMaxSize().scale(zoom.value).alpha(photo.value * 0.75f), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 72.dp)) {
            Row {
                Text("LONEWOLF", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp,
                    modifier = Modifier.alpha(wolf.value).padding(top = ((1f - wolf.value) * 24).dp))
                Spacer(Modifier.width(18.dp))
                Text("SHARK", color = accent, fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp,
                    modifier = Modifier.alpha(shark.value).padding(top = ((1f - shark.value) * 24).dp))
            }
            Box(Modifier.padding(top = 6.dp).height(3.dp).width((line.value * 520).dp).background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.1f)))))
            Text("BUILT FOR THE BYD SHARK 6", color = Shark.muted, fontSize = 14.sp, letterSpacing = 4.sp, modifier = Modifier.padding(top = 12.dp).alpha(tag.value))
        }
    }
}
