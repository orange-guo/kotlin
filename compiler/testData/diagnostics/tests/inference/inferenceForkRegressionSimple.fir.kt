// SKIP_TXT
// FULL_JDK
// WITH_STDLIB

interface C : MutableMap<String, Int>

fun <K, V> foo(m: MutableMap<K, V>, c: C) {
    if (c === m) {
        c.flatMap { _ ->
            listOf("")
        }
    }
}
