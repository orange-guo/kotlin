// SKIP_KLIB_TEST
// WITH_STDLIB
// LANGUAGE: +ValueClasses

import kotlin.jvm.JvmInline

@JvmInline
value class Z(val s: String)

val equals = Z::equals
