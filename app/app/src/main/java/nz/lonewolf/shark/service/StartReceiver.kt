package nz.lonewolf.shark.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import nz.lonewolf.shark.data.Prefs
import nz.lonewolf.shark.ui.MainActivity

/**
 * Opens the app when the head unit starts, if the owner has asked for that in Settings.
 * This reacts to the ute coming on. It never holds the ute awake: no wake lock, no keep alive.
 */
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.w("SharkStart", "woken by ${intent.action}")
        open(context)
    }

    companion object {
        /** BYD's ignition on broadcast, spelt this way by BYD. Seen on the ute 1 Oct 2026. */
        const val ACC_ON = "andoirdauto.acc.on"
        @Volatile private var lastOpen = 0L

        fun open(context: Context) {
            if (!Prefs(context).openAtStart) return
            val now = System.currentTimeMillis()
            if (now - lastOpen < 60_000) return
            lastOpen = now
            runCatching { VehicleService.start(context.applicationContext) }
            runCatching {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            }.onFailure { Log.w("SharkStart", "could not open: $it") }
        }
    }
}
