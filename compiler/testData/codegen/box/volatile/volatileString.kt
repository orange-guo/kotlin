// WITH_STDLIB
// IGNORE_BACKEND: WASM

import kotlin.jvm.Volatile

class StringWrapper(@Volatile var x: String)

val global = StringWrapper("FA")

fun box() : String {
    val local = StringWrapper("IL")
    if (global.x + local.x != "FAIL") return "FAIL"
    global.x = "O"
    local.x = "K"
    return global.x + local.x
}