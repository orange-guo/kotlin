// IGNORE_BACKEND_K1: JVM_IR
// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

import kotlinx.serialization.*

@Serializable
class Bar<T>(val t: T)

fun box(): String {
    return Bar("OK").t
}