package nz.lonewolf.shark.core.byd

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.ZipFile

/**
 * Makes android.hardware.bydauto.* loadable in our process.
 *
 * DiLink 3 (Android 10): the classes are in the boot classpath and nothing is needed.
 * DiLink 5 (Android 11, the Shark 6): they live inside the OEM apps, so we append the
 * dex files of com.byd.carsettings and com.byd.data.collect to our class loader. The
 * OEM code is never copied to disk. Needs hidden_api_policy relaxed (see LocalAdb).
 */
object BydSdkLoader {
    private const val TAG = "BydSdkLoader"

    val probeClasses = listOf(
        "android.hardware.bydauto.ac.BYDAutoAcDevice",
        "android.hardware.bydauto.setting.BYDAutoSettingDevice",
        "android.hardware.bydauto.light.BYDAutoLightDevice",
        "android.hardware.bydauto.statistic.BYDAutoStatisticDevice",
        "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice",
        "android.hardware.bydauto.tyre.BYDAutoTyreDevice",
        "android.hardware.bydauto.speed.BYDAutoSpeedDevice",
        "android.hardware.bydauto.engine.BYDAutoEngineDevice",
        "android.hardware.bydauto.energy.BYDAutoEnergyDevice",
        "android.hardware.bydauto.gearbox.BYDAutoGearboxDevice",
        "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice",
        "android.hardware.bydauto.charging.BYDAutoChargingDevice",
        "android.hardware.bydauto.sensor.BYDAutoSensorDevice",
        "android.hardware.bydauto.vehiclehealth.BYDAutoVehicleHealthDevice",
        "android.hardware.bydauto.ota.BYDAutoOtaDevice",
    )

    private val oemPackages = listOf("com.byd.carsettings", "com.byd.data.collect")

    enum class Mode { BOOT_CLASSPATH, INJECTED, UNAVAILABLE }

    @Volatile var mode: Mode = Mode.UNAVAILABLE; private set
    @Volatile var lastError: String? = null; private set
    private var pristine: Array<*>? = null
    private var pristineLoader: ClassLoader? = null
    private val injected = linkedSetOf<String>()

    @Synchronized
    fun ensure(context: Context): Boolean {
        val loader = context.classLoader
        if (loadable(loader)) {
            if (mode == Mode.UNAVAILABLE) mode = if (injected.isEmpty()) Mode.BOOT_CLASSPATH else Mode.INJECTED
            return true
        }
        val paths = oemPackages.filter { it !in injected }.flatMap { apkPaths(context, it) }.distinct()
        if (paths.isEmpty()) {
            lastError = "no OEM SDK apk found (${oemPackages.joinToString()})"
            mode = Mode.UNAVAILABLE
            return false
        }
        return try {
            val pathListField = Class.forName("dalvik.system.BaseDexClassLoader")
                .getDeclaredField("pathList").apply { isAccessible = true }
            val pathList = pathListField.get(loader)
            val elementsField = pathList.javaClass.getDeclaredField("dexElements").apply { isAccessible = true }
            val current = elementsField.get(pathList) as Array<*>
            val base = if (pristineLoader === loader) pristine!! else current.also {
                pristineLoader = loader; pristine = it
            }
            val suppressed = ArrayList<IOException>()
            val extra = inMemoryElements(pathList.javaClass, paths, suppressed)
                ?: fileElements(pathList.javaClass, paths.map(::File), File(context.codeCacheDir, "byd-sdk").apply { mkdirs() }, suppressed)
                ?: run { lastError = "no dex element factory on this runtime"; return false }
            suppressed.forEach { Log.w(TAG, "suppressed: $it") }
            val type = base.javaClass.componentType!!
            val merged = java.lang.reflect.Array.newInstance(type, base.size + extra.size)
            System.arraycopy(base, 0, merged, 0, base.size)
            System.arraycopy(extra, 0, merged, base.size, extra.size)
            elementsField.set(pathList, merged)
            oemPackages.filter { apkPaths(context, it).isNotEmpty() }.forEach(injected::add)
            val ok = loadable(loader)
            mode = if (ok) Mode.INJECTED else Mode.UNAVAILABLE
            if (!ok) lastError = "injected ${extra.size} dex element(s) but no bydauto class loads (hidden API policy?)"
            Log.i(TAG, "inject ${injected.joinToString()} ok=$ok")
            ok
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            mode = Mode.UNAVAILABLE
            Log.w(TAG, "inject failed", t)
            false
        }
    }

    fun isLoadable(context: Context) = loadable(context.classLoader)

    private fun loadable(loader: ClassLoader) =
        probeClasses.any { runCatching { Class.forName(it, false, loader) }.isSuccess }

    private fun apkPaths(context: Context, pkg: String): List<String> = runCatching {
        val ai = context.packageManager.getApplicationInfo(pkg, 0)
        listOfNotNull(ai.sourceDir) + (ai.splitSourceDirs?.toList() ?: emptyList())
    }.getOrDefault(emptyList())

    private fun inMemoryElements(cls: Class<*>, paths: List<String>, suppressed: MutableList<IOException>): Array<*>? {
        val m = runCatching {
            cls.getDeclaredMethod("makeInMemoryDexElements", Array<ByteBuffer>::class.java, List::class.java)
                .apply { isAccessible = true }
        }.getOrNull() ?: return null
        val buffers = paths.flatMap { path ->
            runCatching {
                ZipFile(path).use { zip ->
                    zip.entries().asSequence()
                        .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                        .map { ByteBuffer.wrap(zip.getInputStream(it).readBytes()) }
                        .toList()
                }
            }.getOrElse { e -> suppressed += IOException("read $path: ${e.message}", e); emptyList() }
        }
        if (buffers.isEmpty()) return null
        return m.invoke(null, buffers.toTypedArray(), suppressed) as Array<*>
    }

    private fun fileElements(cls: Class<*>, files: List<File>, optDir: File, suppressed: MutableList<IOException>): Array<*>? {
        runCatching {
            val m = cls.getDeclaredMethod("makePathElements", List::class.java, File::class.java, List::class.java)
                .apply { isAccessible = true }
            return m.invoke(null, files, optDir, suppressed) as Array<*>
        }
        runCatching {
            val m = cls.getDeclaredMethod("makeDexElements", List::class.java, File::class.java, List::class.java, ClassLoader::class.java)
                .apply { isAccessible = true }
            return m.invoke(null, files, optDir, suppressed, BydSdkLoader::class.java.classLoader) as Array<*>
        }
        return null
    }
}
