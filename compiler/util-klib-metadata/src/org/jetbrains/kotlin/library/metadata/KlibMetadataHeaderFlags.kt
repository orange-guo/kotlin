/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

/**
 * Possible values for [KlibMetadataProtoBuf.Header] flags field.
 * Should match the comment in `KlibMetadataProtoBuf.proto`.
 */
object KlibMetadataHeaderFlags {
    const val PRE_RELEASE = 0x1
}
