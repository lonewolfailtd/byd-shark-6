package nz.lonewolf.shark

import android.app.Application
import nz.lonewolf.shark.core.byd.Vehicle

class Shark : Application() {
    override fun onCreate() {
        super.onCreate()
        Vehicle.init(this)
    }
}
