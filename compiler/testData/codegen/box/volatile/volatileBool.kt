// WITH_STDLIB
// IGNORE_BACKEND: WASM

import kotlin.jvm.Volatile

class BoolWrapper(@Volatile var x: Boolean)

val global = BoolWrapper(false)

fun box() : String {
    val local = BoolWrapper(false)
    if (global.x || local.x) return "FAIL"
    global.x = true
    local.x = true
    return if (global.x && local.x) "OK" else "FAIL"
}