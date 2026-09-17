package nz.lonewolf.shark.core.byd

import android.content.Context
import android.util.Log
import java.lang.reflect.InvocationTargetException

/** Outcome of one SDK write. `raw` is the SDK return code when it returned one. */
data class CommandResult(val ok: Boolean, val call: String, val raw: Int?, val detail: String) {
    companion object {
        fun missing(call: String) = CommandResult(false, call, null, "no such method")
    }
}

/**
 * Reflective handle on one android.hardware.bydauto.*Device singleton.
 * Every bridge is built on this so the firmware to firmware method drift lives in one place.
 */
class BydDevice(private val context: Context, val className: String) {
    private val tag = "Byd." + className.substringAfterLast('.')
    private val permContext = BydPermissionContext(context.applicationContext)

    @Volatile var instance: Any? = null; private set
    @Volatile var bindError: String? = null; private set

    val bound: Boolean get() = instance != null

    @Synchronized
    fun bind(): Boolean {
        if (instance != null) return true
        if (!BydSdkLoader.ensure(context)) { bindError = BydSdkLoader.lastError ?: "SDK not loadable"; return false }
        return try {
            val cls = Class.forName(className)
            val get = cls.getMethod("getInstance", Context::class.java)
            val inst = runCatching { get.invoke(null, permContext) }.getOrNull()
                ?: runCatching { get.invoke(null, context.applicationContext) }.getOrNull()
            if (inst == null) { bindError = "getInstance returned null"; false } else { instance = inst; bindError = null; true }
        } catch (t: Throwable) {
            val c = (t as? InvocationTargetException)?.cause ?: t
            bindError = "${c.javaClass.simpleName}: ${c.message}"
            Log.w(tag, "bind failed: $bindError")
            false
        }
    }

    /** Invoke `name(args)`; a numeric return of 0 or more counts as success. */
    fun call(name: String, vararg args: Any): CommandResult {
        val dev = instance ?: if (bind()) instance!! else return CommandResult(false, name, null, bindError ?: "not bound")
        val types = args.map(::argType).toTypedArray()
        return try {
            val m = dev.javaClass.getMethod(name, *types)
            val raw = (m.invoke(dev, *args) as? Number)?.toInt()
            CommandResult(raw == null || raw >= 0, format(name, args), raw, describe(raw))
        } catch (_: NoSuchMethodException) {
            CommandResult.missing(format(name, args))
        } catch (t: Throwable) {
            val c = (t as? InvocationTargetException)?.cause ?: t
            CommandResult(false, format(name, args), null, "${c.javaClass.simpleName}: ${c.message}")
        }
    }

    /** Try each write in order; return the first success, else the last real failure. */
    fun firstOk(vararg attempts: () -> CommandResult): CommandResult {
        var last: CommandResult? = null
        for (a in attempts) {
            val r = a()
            if (r.ok) return r
            if (r.detail != "no such method") last = r
        }
        return last ?: attempts.first()()
    }

    /** Read an int getter; sentinel values the SDK uses for "unknown" become null. */
    fun getInt(name: String, vararg args: Int): Int? {
        val dev = instance ?: if (bind()) instance!! else return null
        return runCatching {
            val types = Array<Class<*>>(args.size) { Int::class.javaPrimitiveType!! }
            (dev.javaClass.getMethod(name, *types).invoke(dev, *args.toTypedArray()) as? Number)?.toInt()
        }.getOrNull()?.takeUnless { it in sentinels }
    }

    fun getDouble(name: String, vararg args: Int): Double? {
        val dev = instance ?: if (bind()) instance!! else return null
        return runCatching {
            val types = Array<Class<*>>(args.size) { Int::class.javaPrimitiveType!! }
            (dev.javaClass.getMethod(name, *types).invoke(dev, *args.toTypedArray()) as? Number)?.toDouble()
        }.getOrNull()?.takeUnless { it.toInt() in sentinels }
    }

    fun getString(name: String): String? {
        val dev = instance ?: if (bind()) instance!! else return null
        return runCatching { dev.javaClass.getMethod(name).invoke(dev) as? String }.getOrNull()
    }

    fun hasMethod(name: String): Boolean {
        val dev = instance ?: if (bind()) instance!! else return false
        return dev.javaClass.methods.any { it.name == name }
    }

    /** Static int constant by any of several candidate names, walking up the class chain. */
    fun constant(vararg names: String): Int? {
        var cls: Class<*>? = instance?.javaClass ?: return null
        while (cls != null) {
            for (n in names) {
                runCatching { cls!!.getField(n).getInt(null) }.getOrNull()?.let { return it }
                runCatching { cls!!.getDeclaredField(n).apply { isAccessible = true }.getInt(null) }.getOrNull()?.let { return it }
            }
            cls = cls.superclass
        }
        return null
    }

    /** All public getters and setters, for the Diagnostics screen and probe reports. */
    fun methodDump(): List<String> {
        val dev = instance ?: if (bind()) instance!! else return listOf(bindError ?: "not bound")
        return dev.javaClass.methods
            .filter { it.declaringClass.name.contains("bydauto") }
            .map { m -> "${m.name}(${m.parameterTypes.joinToString(",") { it.simpleName }}): ${m.returnType.simpleName}" }
            .sorted()
    }

    private fun argType(a: Any): Class<*> = when (a) {
        is Int -> Int::class.javaPrimitiveType!!
        is IntArray -> IntArray::class.java
        is String -> String::class.java
        else -> a.javaClass
    }

    private fun format(name: String, args: Array<out Any>) = "$name(${args.joinToString()})"

    companion object {
        /** Values the SDK returns for "no data" on various firmwares. */
        val sentinels = setOf(-1, -2147482645, -2147482646, -2147482647, -2147482648, 65535, 255 shl 24)

        fun describe(raw: Int?) = when (raw) {
            null -> "invoked"
            0 -> "success"
            -2147482648 -> "failed"
            -2147482647 -> "busy"
            -2147482646 -> "timeout"
            -2147482645 -> "invalid value"
            else -> "code $raw"
        }
    }
}
