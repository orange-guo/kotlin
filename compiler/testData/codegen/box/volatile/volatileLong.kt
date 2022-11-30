import kotlin.jvm.Volatile

class LongWrapper(@Volatile var x: Long)

val global = LongWrapper(1)

fun box() : String {
    val local = LongWrapper(2)
    if (global.x + local.x != 3L) return "FAIL"
    global.x = 5
    local.x = 6
    return if (global.x + local.x != 11L) return "FAIL" else "OK"
}