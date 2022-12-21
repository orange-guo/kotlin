/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

/**
 * Marks the JVM backing field of the annotated property as `volatile`, meaning that writes to this field
 * are always made visible to other threads. If another thread reads the value of this field (e.g. through its accessor),
 * it sees not only that value, but all side effects that led to writing that value.
 */
@SinceKotlin("1.8")
@ExperimentalStdlibApi
public actual typealias Volatile = kotlin.jvm.Volatile