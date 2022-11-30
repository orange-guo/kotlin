import kotlin.jvm.Volatile

class IntWrapper(@Volatile var x: Int)

val global = IntWrapper(1)

fun box() : String {
    val local = IntWrapper(2)
    if (global.x + local.x != 3) return "FAIL"
    global.x = 5
    local.x = 6
    return if (global.x + local.x != 11) return "FAIL" else "OK"
}