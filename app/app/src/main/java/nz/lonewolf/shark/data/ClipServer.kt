package nz.lonewolf.shark.data

import android.util.Log
import nz.lonewolf.shark.camera.Recorder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A small web page on the ute so a phone on the same wifi can watch and save clips.
 * Plain HTTP, read only, only while the owner has it switched on. Stops when the app stops.
 */
class ClipServer(private val recorder: Recorder) {
    @Volatile private var socket: ServerSocket? = null
    val running: Boolean get() = socket != null
    val port = 8188

    fun address(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filter { it.address.size == 4 && !it.isLoopbackAddress }
            .sortedBy { if (it.hostAddress?.startsWith("192.168.43") == true || it.hostAddress?.startsWith("10.") == true) 0 else 1 }
            .firstOrNull()?.hostAddress
    }.getOrNull()

    fun url(): String? = address()?.let { "http://$it:$port/" }

    @Synchronized
    fun start() {
        if (socket != null) return
        val s = ServerSocket(port, 4, InetAddress.getByName("0.0.0.0"))
        socket = s
        Thread({
            while (socket === s) {
                val c = runCatching { s.accept() }.getOrNull() ?: break
                Thread { runCatching { serve(c) }.onFailure { Log.w(TAG, "request failed: $it") }; runCatching { c.close() } }.start()
            }
        }, "clip-server").start()
        Log.w(TAG, "clip server on ${url()}")
    }

    @Synchronized
    fun stop() { runCatching { socket?.close() }; socket = null }

    private fun serve(c: Socket) {
        c.soTimeout = 15_000
        val reader = BufferedReader(InputStreamReader(c.getInputStream()))
        val line = reader.readLine() ?: return
        var range: String? = null
        while (true) { val h = reader.readLine() ?: break; if (h.isEmpty()) break; if (h.startsWith("Range:", true)) range = h.substringAfter(':').trim() }
        val path = line.split(' ').getOrNull(1) ?: "/"
        val out = c.getOutputStream()
        when {
            path == "/" -> page(out)
            path.startsWith("/clip/") -> file(out, URLDecoder.decode(path.removePrefix("/clip/"), "UTF-8"), range)
            else -> text(out, "404 Not Found", "Not here")
        }
        out.flush()
    }

    private fun page(out: OutputStream) {
        val clips = recorder.saved()
        val rows = clips.joinToString("") { s ->
            val name = URLEncoder.encode(s.name, "UTF-8")
            "<li><a href=\"/clip/$name\">${s.camera}  ${java.text.SimpleDateFormat("EEE d MMM HH:mm:ss", java.util.Locale.US).format(java.util.Date(s.startedAt))}</a> <span>${s.bytes / 1_000_000} MB${if (s.protectedClip) " protected" else ""}</span> <a class=\"dl\" href=\"/clip/$name\" download>save</a></li>"
        }
        val html = """<!doctype html><html lang="en-NZ"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Shark clips</title>
<style>body{font-family:system-ui,sans-serif;background:#0B1220;color:#F2F5F9;margin:0;padding:16px}h1{font-size:20px;letter-spacing:2px}ul{list-style:none;padding:0}li{background:#131C2E;border:1px solid #223047;border-radius:12px;padding:12px;margin:8px 0;display:flex;gap:10px;align-items:center;flex-wrap:wrap}a{color:#19E3FF;text-decoration:none;flex:1}span{color:#A9BCD6;font-size:13px}.dl{flex:0;background:#19E3FF;color:#000;padding:8px 14px;border-radius:8px;font-weight:600}</style></head>
<body><h1>LONEWOLF SHARK</h1><p>Tap a clip to watch it, or save to keep it on your phone. ${clips.size} clips.</p><ul>$rows</ul></body></html>"""
        val body = html.toByteArray()
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray()); out.write(body)
    }

    private fun file(out: OutputStream, name: String, range: String?) {
        val f = listOf(File(recorder.storageRoot(), name), File(recorder.eventsDir(), name)).firstOrNull { it.exists() && it.name == name && !name.contains('/') }
            ?: return text(out, "404 Not Found", "No such clip")
        val len = f.length()
        var start = 0L; var end = len - 1
        val m = range?.let { Regex("bytes=(\\d*)-(\\d*)").find(it) }
        if (m != null) { start = m.groupValues[1].toLongOrNull() ?: 0L; end = m.groupValues[2].toLongOrNull() ?: (len - 1) }
        val partial = m != null
        out.write(("HTTP/1.1 ${if (partial) "206 Partial Content" else "200 OK"}\r\nContent-Type: video/mp4\r\nAccept-Ranges: bytes\r\nContent-Length: ${end - start + 1}\r\n" +
            (if (partial) "Content-Range: bytes $start-$end/$len\r\n" else "") + "Connection: close\r\n\r\n").toByteArray())
        f.inputStream().use { input ->
            input.skip(start)
            val buf = ByteArray(64 * 1024); var left = end - start + 1
            while (left > 0) { val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt()); if (n < 0) break; out.write(buf, 0, n); left -= n }
        }
    }

    private fun text(out: OutputStream, status: String, body: String) {
        val b = body.toByteArray()
        out.write("HTTP/1.1 $status\r\nContent-Type: text/plain\r\nContent-Length: ${b.size}\r\nConnection: close\r\n\r\n".toByteArray()); out.write(b)
    }

    companion object { private const val TAG = "SharkShare" }
}
