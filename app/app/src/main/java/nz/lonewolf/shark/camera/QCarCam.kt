package nz.lonewolf.shark.camera

import java.nio.ByteBuffer

/**
 * JNI facade over Qualcomm's AIS / QCarCam client, the only way a normal app can see the
 * Shark 6 cameras. Native side: src/main/cpp/qcarcam_bridge.cpp. Streams are per input id.
 * Never open ids the probe did not list: the head unit restarted when we did.
 */
object QCarCam {
    @Volatile var loaded: Boolean = false; private set
    @Volatile var loadError: String? = null; private set

    /** Camera inputs on the Shark 6 Premium, mapped by Tane on 18 Sep 2026. */
    enum class Cam(val id: Int, val label: String, val exterior: Boolean) {
        FRONT(8, "Front", true), REAR(5, "Rear", true), LEFT(4, "Passenger side", true), RIGHT(9, "Driver side", true), CABIN(0, "Cabin", false);
        companion object { fun byId(id: Int) = entries.firstOrNull { it.id == id } }
    }

    init {
        try { System.loadLibrary("sharkcam"); loaded = true } catch (t: Throwable) { loadError = t.message }
    }

    @JvmStatic private external fun nativeProbe(): String
    @JvmStatic private external fun nativeOpen(id: Int): String
    @JvmStatic private external fun nativeStopOne(id: Int): String
    @JvmStatic private external fun nativeStop(): String
    @JvmStatic private external fun nativeStreamSize(id: Int): IntArray
    @JvmStatic private external fun nativeReadFrame(id: Int, outputWidth: Int, outputHeight: Int, flatten: Boolean, fishFocal: Float, outFovDeg: Float): IntArray?
    @JvmStatic private external fun nativeReadNv12(id: Int, buffer: ByteBuffer, outputWidth: Int, outputHeight: Int): Int

    private val open = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    val openIds: Set<Int> get() = open.toSet()

    fun probe(): String = guard { nativeProbe() }
    fun open(id: Int): String = guard { nativeOpen(id).also { if (it.startsWith("STREAM_STARTED")) open += id } }
    fun stopOne(id: Int): String = guard { nativeStopOne(id).also { open -= id } }
    fun stopAll(): String = guard { nativeStop().also { open.clear() } }
    fun size(id: Int): Pair<Int, Int> = if (loaded) nativeStreamSize(id).let { it[0] to it[1] } else 0 to 0
    /** fishFocal: source pixels per radian of the lens (about 580 for the 1920 wide exterior cameras). outFov: horizontal degrees shown when flattened. */
    fun frame(id: Int, w: Int, h: Int, flatten: Boolean = false, fishFocal: Float = 580f, outFov: Float = 110f): IntArray? =
        if (loaded) runCatching { nativeReadFrame(id, w, h, flatten, fishFocal, outFov) }.getOrNull() else null
    fun nv12(id: Int, buffer: ByteBuffer, w: Int, h: Int): Int = if (loaded) runCatching { nativeReadNv12(id, buffer, w, h) }.getOrDefault(0) else 0

    private fun guard(block: () -> String) = if (loaded) runCatching(block).getOrElse { "failed: $it" } else "native library not loaded: $loadError"
}
