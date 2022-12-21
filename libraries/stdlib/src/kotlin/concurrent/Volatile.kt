/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind


/**
 * Marks the backing field of the annotated property as `volatile`, meaning that writes to this field
 * are always made visible to other threads. If another thread reads the value of this field (e.g. through its accessor),
 * it sees not only that value, but all side effects that led to writing that value.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@OptionalExpectation
@SinceKotlin("1.8")
@RequireKotlin(version = "1.8.20", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@ExperimentalStdlibApi
public expect annotation class Volatile()