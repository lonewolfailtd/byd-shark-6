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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Drive recorder: one H.264 encoder per camera, rolling clips, USB stick first.
 * Frames come from QCarCam as NV12 straight into the encoder's input buffer.
 */
class Recorder(private val context: Context) {
    data class Status(val recording: Boolean, val cameras: List<QCarCam.Cam>, val folder: String?, val clip: String?, val fps: Map<Int, Int>, val error: String?)

    private val _status = MutableStateFlow(Status(false, emptyList(), null, null, emptyMap(), null))
    val status: StateFlow<Status> = _status
    private val workers = mutableListOf<CamWorker>()
    @Volatile private var running = false

    var clipSeconds = 180
    var width = 1280
    var height = 864       // 1920x1300 scaled, kept a multiple of 16
    var bitrate = 6_000_000
    var frameRate = 15

    /** First mounted removable volume (USB stick), else the app's own external files folder. */
    fun storageRoot(): File {
        val sm = context.getSystemService(StorageManager::class.java)
        val usb = sm.storageVolumes.firstOrNull { it.isRemovable && it.state == "mounted" }?.let { v ->
            runCatching { v.directory }.getOrNull() ?: v.uuid?.let { File("/storage/$it") }
        }
        val root = usb?.takeIf { it.exists() && it.canWrite() } ?: context.getExternalFilesDir(null)!!
        return File(root, "LonewolfShark").apply { mkdirs() }
    }

    @Synchronized
    fun start(cams: List<QCarCam.Cam>) {
        if (running) return
        val root = storageRoot()
        running = true
        workers.clear()
        cams.forEach { cam ->
            val r = QCarCam.open(cam.id)
            if (!r.startsWith("STREAM_STARTED")) { Log.w(TAG, "open ${cam.label}: $r"); return@forEach }
            workers += CamWorker(cam, root).also { it.start() }
        }
        _status.value = Status(true, workers.map { it.cam }, root.path, null, emptyMap(), if (workers.isEmpty()) "no camera opened" else null)
        Log.w(TAG, "recording ${workers.size} cameras to $root")
    }

    @Volatile private var pausedCams: List<QCarCam.Cam>? = null

    /** Release the cameras so BYD's reversing or 360 view can use them; resume() restarts. */
    @Synchronized
    fun pause(reason: String) {
        if (!running) return
        pausedCams = workers.map { it.cam }
        stop()
        _status.value = _status.value.copy(error = "paused: $reason")
    }

    @Synchronized
    fun resume() {
        val cams = pausedCams ?: return
        pausedCams = null
        start(cams)
    }

    val isPaused: Boolean get() = pausedCams != null

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

    fun clips(): List<File> = storageRoot().listFiles { f -> f.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** Keep the newest clips inside a size budget. */
    fun prune(maxBytes: Long = 8L * 1024 * 1024 * 1024) {
        var total = 0L
        clips().forEach { f -> total += f.length(); if (total > maxBytes) f.delete() }
    }

    private inner class CamWorker(val cam: QCarCam.Cam, private val root: File) : Thread("rec-${cam.label}") {
        override fun run() {
            var codec: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var track = -1
            var clipStart = 0L
            var frames = 0
            var fpsWindowStart = System.currentTimeMillis(); var fpsCount = 0
            var grabNs = 0L; var waitNs = 0L
            val info = MediaCodec.BufferInfo()
            try {
                codec = newCodec()
                codec.start()
                val t0 = System.nanoTime()
                while (running && !isInterrupted) {
                    // New clip when the current one is old enough
                    if (muxer == null || System.currentTimeMillis() - clipStart > clipSeconds * 1000L) {
                        muxer?.let { runCatching { it.stop(); it.release() } }
                        muxer = null; track = -1
                        // Ask for a key frame at the clip boundary, then reopen the muxer on the next format
                        codec.setParameters(android.os.Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) })
                        clipStart = System.currentTimeMillis(); frames = 0
                        val name = "${cam.label.replace(' ', '_')}_${stamp.format(Date(clipStart))}.mp4"
                        muxer = MediaMuxer(File(root, name).path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                        _status.value = _status.value.copy(clip = name)
                        prune()
                    }
                    val tWait = System.nanoTime()
                    val inIndex = codec.dequeueInputBuffer(100_000)
                    waitNs += System.nanoTime() - tWait
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        buf.clear()
                        val tGrab = System.nanoTime()
                        val n = QCarCam.nv12(cam.id, buf, width, height)
                        grabNs += System.nanoTime() - tGrab
                        // Real wall clock timestamps so playback speed matches even when frames arrive slower than target.
                        val pts = (System.nanoTime() - t0) / 1000
                        if (n > 0) { buf.limit(n); codec.queueInputBuffer(inIndex, 0, n, pts, 0); frames++; fpsCount++ }
                        else codec.queueInputBuffer(inIndex, 0, 0, pts, 0)
                    }
                    // Drain everything the encoder has
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
                    if (now - fpsWindowStart >= 1000) {
                        _status.value = _status.value.copy(fps = _status.value.fps + (cam.id to fpsCount))
                        Log.w(TAG, "${cam.label}: $fpsCount fps, grab ${grabNs / 1_000_000} ms, codec wait ${waitNs / 1_000_000} ms per second")
                        fpsCount = 0; fpsWindowStart = now; grabNs = 0; waitNs = 0
                    }
                    // pace to the target frame rate so four encoders share the CPU fairly
                    val elapsed = System.currentTimeMillis() - clipStart
                    val expected = frames * 1000L / frameRate
                    if (expected > elapsed) sleep((expected - elapsed).coerceAtMost(60))
                }
            } catch (_: InterruptedException) {
            } catch (t: Throwable) {
                Log.e(TAG, "worker ${cam.label} failed", t)
                _status.value = _status.value.copy(error = "${cam.label}: ${t.message}")
            } finally {
                runCatching { codec?.stop(); codec?.release() }
                runCatching { muxer?.stop(); muxer?.release() }
            }
        }

        private fun newCodec(): MediaCodec {
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
    }
}
