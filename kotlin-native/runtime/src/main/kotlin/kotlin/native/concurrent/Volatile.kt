/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

// This is a hack to be compatible with jvm annoation, which by historical reasons
// located in a very strange package for common modules.
actual typealias Volatile = kotlin.native.internal.Volatile