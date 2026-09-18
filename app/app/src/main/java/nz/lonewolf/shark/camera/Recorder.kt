package nz.lonewolf.shark.camera

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.storage.StorageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Drive recorder and sentry. One H.264 encoder per camera, rolling clips, USB stick first.
 * DRIVE: 3 minute clips, newest kept within the size budget, events protected.
 * SENTRY: 1 minute clips at lower bitrate; a clip is kept only if a camera saw movement.
 */
class Recorder(private val context: Context) {
    enum class Mode { DRIVE, SENTRY }

    data class Status(
        val recording: Boolean = false, val mode: Mode = Mode.DRIVE, val cameras: List<QCarCam.Cam> = emptyList(),
        val folder: String? = null, val clip: String? = null, val fps: Map<Int, Int> = emptyMap(), val error: String? = null,
        val motion: Map<Int, Long> = emptyMap(), val lastAlert: String? = null, val eventsKept: Int = 0,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status
    private val workers = mutableListOf<CamWorker>()
    @Volatile private var running = false
    @Volatile private var mode = Mode.DRIVE
    @Volatile private var eventRequested = false

    var driveClipSeconds = 180
    var sentryClipSeconds = 60
    var width = 1280
    var height = 864
    var frameRate = 15
    /** Fraction of the frame that must change between samples to count as movement. */
    var motionThreshold = 0.04f

    fun storageRoot(): File {
        val sm = context.getSystemService(StorageManager::class.java)
        val usb = sm.storageVolumes.firstOrNull { it.isRemovable && it.state == "mounted" }?.let { v ->
            runCatching { v.directory }.getOrNull() ?: v.uuid?.let { File("/storage/$it") }
        }
        val root = usb?.takeIf { it.exists() && it.canWrite() } ?: context.getExternalFilesDir(null)!!
        return File(root, "LonewolfShark").apply { mkdirs() }
    }
    fun eventsDir(): File = File(storageRoot(), "events").apply { mkdirs() }

    @Synchronized
    fun start(cams: List<QCarCam.Cam>, mode: Mode = Mode.DRIVE) {
        if (running) return
        this.mode = mode
        val root = storageRoot()
        running = true
        workers.clear()
        cams.forEach { cam ->
            val r = QCarCam.open(cam.id)
            if (!r.startsWith("STREAM_STARTED")) { Log.w(TAG, "open ${cam.label}: $r"); return@forEach }
            workers += CamWorker(cam, root).also { it.start() }
        }
        _status.value = Status(true, mode, workers.map { it.cam }, root.path, null, emptyMap(), if (workers.isEmpty()) "no camera opened" else null,
            eventsKept = eventsDir().listFiles()?.size ?: 0)
        Log.w(TAG, "$mode recording ${workers.size} cameras to $root")
    }

    @Volatile private var paused: Pair<List<QCarCam.Cam>, Mode>? = null
    val isPaused: Boolean get() = paused != null

    @Synchronized fun pause(reason: String) { if (!running) return; paused = workers.map { it.cam } to mode; stop(); _status.value = _status.value.copy(error = "paused: $reason") }
    @Synchronized fun resume() { val p = paused ?: return; paused = null; start(p.first, p.second) }

    @Synchronized
    fun stop() {
        running = false
        workers.forEach { it.interrupt() }
        workers.forEach { runCatching { it.join(4000) } }
        workers.forEach { QCarCam.stopOne(it.cam.id) }
        workers.clear()
        _status.value = _status.value.copy(recording = false, clip = null, fps = emptyMap())
        Log.w(TAG, "recording stopped")
    }

    /** Protect what just happened: the previous clip is copied now, the current one when it closes. */
    fun markEvent() {
        eventRequested = true
        workers.forEach { w -> w.previousClip?.let { keep(it) } }
        _status.value = _status.value.copy(lastAlert = "Event saved ${time.format(Date())}")
    }

    private fun keep(f: File) {
        runCatching { val dst = File(eventsDir(), f.name); if (!dst.exists()) f.copyTo(dst); _status.value = _status.value.copy(eventsKept = eventsDir().listFiles()?.size ?: 0) }
    }

    fun clips(): List<File> = storageRoot().listFiles { f -> f.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun events(): List<File> = eventsDir().listFiles { f -> f.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun prune(maxBytes: Long = 8L * 1024 * 1024 * 1024) {
        var total = 0L
        clips().forEach { f -> total += f.length(); if (total > maxBytes) f.delete() }
    }

    private inner class CamWorker(val cam: QCarCam.Cam, private val root: File) : Thread("rec-${cam.label}") {
        @Volatile var previousClip: File? = null
        private var currentClip: File? = null
        private var clipHadMotion = false
        private var clipHadEvent = false
        private var prevSample: ByteArray? = null
        private var motionStreak = 0

        override fun run() {
            var codec: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var track = -1
            var clipStart = 0L
            var frames = 0
            var fpsWindowStart = System.currentTimeMillis(); var fpsCount = 0
            val info = MediaCodec.BufferInfo()
            val clipMs = (if (mode == Mode.SENTRY) sentryClipSeconds else driveClipSeconds) * 1000L
            try {
                codec = newCodec(if (mode == Mode.SENTRY) 4_000_000 else 10_000_000)
                codec.start()
                val t0 = System.nanoTime()
                while (running && !isInterrupted) {
                    if (muxer == null || System.currentTimeMillis() - clipStart > clipMs) {
                        muxer?.let { runCatching { it.stop(); it.release() } }
                        muxer = null; track = -1
                        closeClip()
                        codec.setParameters(android.os.Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) })
                        clipStart = System.currentTimeMillis(); frames = 0
                        val name = "${cam.label.replace(' ', '_')}_${stamp.format(Date(clipStart))}.mp4"
                        currentClip = File(root, name); clipHadMotion = false; clipHadEvent = false
                        muxer = MediaMuxer(currentClip!!.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                        _status.value = _status.value.copy(clip = name)
                        prune()
                    }
                    if (eventRequested) clipHadEvent = true
                    val inIndex = codec.dequeueInputBuffer(100_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        buf.clear()
                        val n = QCarCam.nv12(cam.id, buf, width, height)
                        val pts = (System.nanoTime() - t0) / 1000
                        if (n > 0) {
                            if (mode == Mode.SENTRY && frames % 3 == 0) detectMotion(buf)
                            buf.limit(n); codec.queueInputBuffer(inIndex, 0, n, pts, 0); frames++; fpsCount++
                        } else codec.queueInputBuffer(inIndex, 0, 0, pts, 0)
                    }
                    while (true) {
                        val out = codec.dequeueOutputBuffer(info, 0)
                        when {
                            out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer!!.addTrack(codec.outputFormat); muxer!!.start() }
                            out >= 0 -> {
                                val ob = codec.getOutputBuffer(out)!!
                                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                                if (info.size > 0) {
                                    if (track < 0) { track = muxer!!.addTrack(codec.outputFormat); muxer!!.start() }
                                    ob.position(info.offset); ob.limit(info.offset + info.size)
                                    muxer!!.writeSampleData(track, ob, info)
                                }
                                codec.releaseOutputBuffer(out, false)
                            }
                            else -> break
                        }
                    }
                    val now = System.currentTimeMillis()
                    if (now - fpsWindowStart >= 1000) { _status.value = _status.value.copy(fps = _status.value.fps + (cam.id to fpsCount)); fpsCount = 0; fpsWindowStart = now }
                    val expected = frames * 1000L / frameRate
                    val elapsed = System.currentTimeMillis() - clipStart
                    if (expected > elapsed) sleep((expected - elapsed).coerceAtMost(60))
                }
            } catch (_: InterruptedException) {
            } catch (t: Throwable) {
                Log.e(TAG, "worker ${cam.label} failed", t)
                _status.value = _status.value.copy(error = "${cam.label}: ${t.message}")
            } finally {
                runCatching { codec?.stop(); codec?.release() }
                runCatching { muxer?.stop(); muxer?.release() }
                closeClip()
            }
        }

        /** Called when a clip file is finished: keep it, or in sentry mode drop it if nothing moved. */
        private fun closeClip() {
            val f = currentClip ?: return
            currentClip = null
            when {
                clipHadEvent -> { keep(f); eventRequested = false }
                mode == Mode.SENTRY && !clipHadMotion -> f.delete()
                mode == Mode.SENTRY && clipHadMotion -> keep(f)
            }
            previousClip = if (f.exists()) f else previousClip
        }

        /** Compare a coarse grid of luma samples against the previous sample. */
        private fun detectMotion(nv12: ByteBuffer) {
            val step = 16
            val cols = width / step; val rows = height / step
            val sample = ByteArray(cols * rows)
            var i = 0
            for (y in 0 until rows) { val base = y * step * width; for (x in 0 until cols) sample[i++] = nv12.get(base + x * step) }
            val prev = prevSample
            prevSample = sample
            if (prev == null) return
            var changed = 0
            for (k in sample.indices) if (abs((sample[k].toInt() and 0xFF) - (prev[k].toInt() and 0xFF)) > 28) changed++
            val fraction = changed.toFloat() / sample.size
            if (fraction > motionThreshold) {
                motionStreak++
                if (motionStreak >= 2) {
                    clipHadMotion = true
                    val now = System.currentTimeMillis()
                    _status.value = _status.value.copy(motion = _status.value.motion + (cam.id to now), lastAlert = "Movement at ${cam.label} ${time.format(Date(now))}")
                }
            } else motionStreak = 0
        }

        private fun newCodec(bitrate: Int): MediaCodec {
            val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }
            return MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply { configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
        }
    }

    companion object {
        private const val TAG = "SharkRec"
        private val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private val time = SimpleDateFormat("HH:mm:ss", Locale.US)
    }
}
