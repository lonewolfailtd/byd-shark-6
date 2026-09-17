package nz.lonewolf.shark.core.byd

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager

/**
 * The BYD SDK checks its signature level GET and SET permissions in our own process
 * through the Context we hand to getInstance(). Answering "granted" for every
 * android.permission.BYDAUTO_* permission is enough on the Shark 6 for climate, seats,
 * bodywork and lights. Subsystems enforced inside the OEM service (camera, instrument
 * SET) still refuse, and we do not use those.
 */
class BydPermissionContext(base: Context) : ContextWrapper(base) {

    /** DiPilot and ADAS keep getApplicationContext(); hand back a wrapped one too. */
    override fun getApplicationContext(): Context {
        val app = baseContext.applicationContext
        return if (app === this || app === baseContext) this else BydPermissionContext(app)
    }

    override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
        if (isByd(permission)) PackageManager.PERMISSION_GRANTED else super.checkPermission(permission, pid, uid)

    override fun checkCallingPermission(permission: String): Int =
        if (isByd(permission)) PackageManager.PERMISSION_GRANTED else super.checkCallingPermission(permission)

    override fun checkCallingOrSelfPermission(permission: String): Int =
        if (isByd(permission)) PackageManager.PERMISSION_GRANTED else super.checkCallingOrSelfPermission(permission)

    override fun checkSelfPermission(permission: String): Int =
        if (isByd(permission)) PackageManager.PERMISSION_GRANTED else super.checkSelfPermission(permission)

    override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) {
        if (!isByd(permission)) super.enforcePermission(permission, pid, uid, message)
    }

    override fun enforceCallingPermission(permission: String, message: String?) {
        if (!isByd(permission)) super.enforceCallingPermission(permission, message)
    }

    override fun enforceCallingOrSelfPermission(permission: String, message: String?) {
        if (!isByd(permission)) super.enforceCallingOrSelfPermission(permission, message)
    }

    private fun isByd(permission: String) = permission.startsWith("android.permission.BYDAUTO_")
}
