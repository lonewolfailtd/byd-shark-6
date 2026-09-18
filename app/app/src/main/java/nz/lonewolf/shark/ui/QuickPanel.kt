package nz.lonewolf.shark.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import nz.lonewolf.shark.core.byd.CommandResult
import nz.lonewolf.shark.core.byd.SeatBridge
import nz.lonewolf.shark.core.byd.Vehicle
import nz.lonewolf.shark.service.VehicleService
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Floating quick panel drawn over every other app: a draggable bubble that expands into
 * climate and seat controls. Plain Views on purpose so it needs no Compose lifecycle.
 * Requires SYSTEM_ALERT_WINDOW (granted by the installer) and BYD's byd_float_app_list entry.
 */
class QuickPanel(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("quick_panel", Context.MODE_PRIVATE)
    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var panel: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private val labels = mutableMapOf<String, TextView>()

    val showing: Boolean get() = bubble != null

    private var shade: View? = null
    val shaded: Boolean get() = shade != null

    /** Dark translucent layer over the whole screen. Touches pass through, so BYD's screens still work under it. */
    fun setShade(on: Boolean, alpha: Float = 0.7f) {
        if (!on) { shade?.let { runCatching { wm.removeView(it) } }; shade = null; return }
        if (shade != null) return
        val v = View(context).apply { setBackgroundColor(Color.BLACK); this.alpha = alpha }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { wm.addView(v, lp) }.onSuccess { shade = v }
        // Keep the bubble above the shade so it stays usable.
        bubble?.let { b -> params?.let { runCatching { wm.removeView(b); wm.addView(b, it) } } }
    }

    fun show() {
        if (bubble != null) return
        if (!Settings.canDrawOverlays(context)) return
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 1700); y = prefs.getInt("y", 500)
        }
        params = lp
        val b = makeBubble()
        runCatching { wm.addView(b, lp) }.onSuccess { bubble = b; ui.post(refresh) }
    }

    fun hide() {
        ui.removeCallbacks(refresh)
        setShade(false)
        panel?.let { runCatching { wm.removeView(it) } }; panel = null
        bubble?.let { runCatching { wm.removeView(it) } }; bubble = null
        expanded = false
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()

    private fun makeBubble(): View {
        val shark = TextView(context).apply {
            text = "🦈"; textSize = 26f; gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xEE131C2E.toInt()); setStroke(dp(2), 0xFF3DDC84.toInt()) }
            layoutParams = android.widget.FrameLayout.LayoutParams(dp(64), dp(64)).apply { topMargin = dp(10) }
        }
        // Small close badge in the corner: hides the bubble until the app is opened again.
        val close = TextView(context).apply {
            text = "✕"; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF37474F.toInt()); setStroke(dp(1), 0xFF8FA3BF.toInt()) }
            layoutParams = android.widget.FrameLayout.LayoutParams(dp(24), dp(24)).apply { gravity = Gravity.TOP or Gravity.END }
            setOnClickListener { hide() }
        }
        val root = android.widget.FrameLayout(context).apply { addView(shark); addView(close) }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        shark.setOnTouchListener { _, e ->
            val lp = params ?: return@setOnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) { lp.x = startX + dx.toInt(); lp.y = startY + dy.toInt(); wm.updateViewLayout(root, lp); panel?.let { positionPanel() } }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply() else toggle()
                    true
                }
                else -> false
            }
        }
        return root
    }

    private fun toggle() { if (expanded) collapse() else expand() }

    private fun collapse() {
        panel?.let { runCatching { wm.removeView(it) } }; panel = null; expanded = false
    }

    private fun expand() {
        val p = makePanel()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        p.tag = lp
        // Tap anywhere outside the panel to fold it away.
        p.setOnTouchListener { _, e -> if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) { collapse(); true } else false }
        runCatching { wm.addView(p, lp) }.onSuccess { panel = p; expanded = true; positionPanel(); updateLabels(); p.post { positionPanel() } }
    }

    /** Keep the panel beside the bubble, flipping to the left when near the right edge. */
    private fun positionPanel() {
        val p = panel ?: return; val bp = params ?: return
        val lp = p.tag as WindowManager.LayoutParams
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val panelW = if (p.width > 0) p.width else dp(600)
        val panelH = if (p.height > 0) p.height else dp(240)
        var x = if (bp.x + dp(64) + panelW > screenW) bp.x - panelW - dp(8) else bp.x + dp(72)
        var y = bp.y - dp(60)
        // Never let the panel hang off the screen edges.
        x = x.coerceIn(dp(8), (screenW - panelW - dp(8)).coerceAtLeast(dp(8)))
        y = y.coerceIn(dp(70), (screenH - panelH - dp(90)).coerceAtLeast(dp(70)))
        lp.x = x; lp.y = y
        runCatching { wm.updateViewLayout(p, lp) }
    }

    private fun button(text: String, wide: Boolean = false, onClick: () -> Unit): TextView = TextView(context).apply {
        this.text = text; textSize = 18f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(0xFF1B2638.toInt()); setStroke(dp(1), 0xFF223047.toInt()) }
        layoutParams = LinearLayout.LayoutParams(if (wide) dp(150) else dp(64), dp(64)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
        setOnClickListener { onClick() }
    }

    private fun label(key: String, text: String): TextView = TextView(context).apply {
        this.text = text; textSize = 26f; gravity = Gravity.CENTER; setTextColor(0xFFF2F5F9.toInt())
        layoutParams = LinearLayout.LayoutParams(dp(96), dp(64))
        labels[key] = this
    }

    private fun write(block: () -> CommandResult) {
        io.execute {
            runCatching(block)
            runCatching { VehicleService.climate.value = Vehicle.climate.read(); VehicleService.seats.value = Vehicle.seats.read() }
            ui.post { updateLabels() }
        }
    }

    private fun makePanel(): View {
        val c = Vehicle.climate; val s = Vehicle.seats
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0xF2131C2E.toInt()); setStroke(dp(2), 0xFF3DDC84.toInt()) }
        }
        fun row(vararg views: View) = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; views.forEach { addView(it) }; root.addView(this) }
        row(
            button("−") { write { c.nudgePassengerTemp(-1) } }, label("pass", "--°"), button("+") { write { c.nudgePassengerTemp(+1) } },
            button("−") { write { c.nudgeFan(-1) } }, label("fan", "fan --"), button("+") { write { c.nudgeFan(+1) } },
            button("−") { write { c.nudgeDriverTemp(-1) } }, label("drv", "--°"), button("+") { write { c.nudgeDriverTemp(+1) } },
        )
        row(
            button("Power", wide = true) { write { c.power(VehicleService.climate.value?.powerOn != true) } },
            button("Auto", wide = true) { write { c.setAuto(VehicleService.climate.value?.auto != true) } },
            button("Recirc", wide = true) { write { c.setRecirc(VehicleService.climate.value?.recirc != true) } },
            button("Demist", wide = true) { write { c.setFrontDemist(VehicleService.climate.value?.frontDemist != true) } },
        )
        row(
            button("Pass seat heat", wide = true) { write { s.setHeat(SeatBridge.PASSENGER, nextLevel(VehicleService.seats.value?.passenger?.heat)) } },
            button("Driver seat heat", wide = true) { write { s.setHeat(SeatBridge.DRIVER, nextLevel(VehicleService.seats.value?.driver?.heat)) } },
            button("Driver seat vent", wide = true) { write { s.setVent(SeatBridge.DRIVER, nextLevel(VehicleService.seats.value?.driver?.vent)) } },
            button("Open app", wide = true) {
                collapse()
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            button("Night", wide = true) { setShade(!shaded); collapse() },
            button("✕") { collapse() },
        )
        return root
    }

    /** Off, low, high, off. */
    private fun nextLevel(l: SeatBridge.Level?) = when (l) { SeatBridge.Level.OFF, null -> SeatBridge.Level.LOW; SeatBridge.Level.LOW -> SeatBridge.Level.HIGH; SeatBridge.Level.HIGH -> SeatBridge.Level.OFF }

    private fun updateLabels() {
        val c = VehicleService.climate.value ?: return
        labels["pass"]?.text = c.passengerTemp?.let { "$it°" } ?: "--°"
        labels["drv"]?.text = c.driverTemp?.let { "$it°" } ?: "--°"
        labels["fan"]?.text = "fan " + (c.fan?.toString() ?: "--") + if (c.powerOn == false) " off" else ""
    }

    private val refresh = object : Runnable {
        override fun run() { if (expanded) updateLabels(); if (bubble != null) ui.postDelayed(this, 1000) }
    }
}
