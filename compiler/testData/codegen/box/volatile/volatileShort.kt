import kotlin.jvm.Volatile

class ShortWrapper(@Volatile var x: Short)

val global = ShortWrapper(1)

fun box() : String {
    val local = ShortWrapper(2)
    if (global.x + local.x != 3) return "FAIL"
    global.x = 5
    local.x = 6
    return if (global.x + local.x != 11) return "FAIL" else "OK"
}