// WITH_STDLIB
// IGNORE_BACKEND: WASM

import kotlin.jvm.Volatile

class ByteWrapper(@Volatile var x: Byte)

val global = ByteWrapper(1)

fun box() : String {
    val local = ByteWrapper(2)
    if (global.x + local.x != 3) return "FAIL"
    global.x = 5
    local.x = 6
    return if (global.x + local.x != 11) return "FAIL" else "OK"
}