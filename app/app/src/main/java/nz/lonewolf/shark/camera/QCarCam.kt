package nz.lonewolf.shark.camera

/**
 * JNI facade over Qualcomm's AIS / QCarCam client (/vendor/lib64/libais_client.so), the only
 * way a normal app can see the Shark 6 cameras. Native side: src/main/cpp/qcarcam_bridge.cpp.
 * One stream at a time. Always stop before BYD's own 360 view is opened.
 */
object QCarCam {
    @Volatile var loaded: Boolean = false; private set
    @Volatile var loadError: String? = null; private set

    init {
        try { System.loadLibrary("sharkcam"); loaded = true } catch (t: Throwable) { loadError = t.message }
    }

    @JvmStatic external fun nativeProbe(): String
    @JvmStatic external fun nativeOpen(cameraId: Int): String
    @JvmStatic external fun nativeReadFrame(outputWidth: Int, outputHeight: Int): IntArray?
    @JvmStatic external fun nativeStop(): String
    @JvmStatic external fun nativeProbeIds(firstId: Int, lastId: Int): String

    fun probe(): String = if (loaded) runCatching { nativeProbe() }.getOrElse { "probe failed: $it" } else "native library not loaded: $loadError"
    fun scanIds(): String = if (loaded) runCatching { nativeProbeIds(0, 24) }.getOrElse { "scan failed: $it" } else "native library not loaded"
    fun open(id: Int): String = if (loaded) runCatching { nativeOpen(id) }.getOrElse { "open failed: $it" } else "native library not loaded"
    fun stop(): String = if (loaded) runCatching { nativeStop() }.getOrElse { "stop failed: $it" } else "native library not loaded"
    fun frame(w: Int, h: Int): IntArray? = if (loaded) runCatching { nativeReadFrame(w, h) }.getOrNull() else null
}
