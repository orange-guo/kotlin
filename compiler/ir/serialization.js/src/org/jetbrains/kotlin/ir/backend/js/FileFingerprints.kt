/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.library.SerializedIrModule
import java.util.*

private const val FILE_FINGERPRINTS_SEPARATOR = " "
private const val FILE_FINGERPRINTS_HASH_SEPARATOR = "-"

private fun StringBuilder.appendHashed(data: ByteArray, separator: String) {
    append(cityHash64(data).toString(Character.MAX_RADIX))
    append(separator)
}

internal fun SerializedIrModule.calculateSerializedIrFileSha256Fingerprints() = buildString {
    for (file in files.withIndex()) {
        if (file.index != 0) {
            append(FILE_FINGERPRINTS_SEPARATOR)
        }

        appendHashed(file.value.path.toByteArray(), FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.fileData, FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.fqName.toByteArray(), FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.types, FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.signatures, FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.strings, FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.bodies, FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.declarations, FILE_FINGERPRINTS_HASH_SEPARATOR)
        appendHashed(file.value.debugInfo ?: ByteArray(0), "")
    }
}

class SerializedIrFileSha256Fingerprint(
    val pathHash: ULong,
    val fileDataHash: ULong,
    val fqNameHash: ULong,
    val typesHash: ULong,
    val signaturesHash: ULong,
    val stringsHash: ULong,
    val bodiesHash: ULong,
    val declarationsHash: ULong,
    val debugInfoHash: ULong
) {
    companion object {
        const val HASHES_COUNT = 9
    }
}

internal fun String.parseSerializedIrFileSha256Fingerprints(): List<SerializedIrFileSha256Fingerprint> {
    return split(FILE_FINGERPRINTS_SEPARATOR).mapNotNull { fileFingerprintLine ->
        fileFingerprintLine.split(FILE_FINGERPRINTS_HASH_SEPARATOR).takeIf { fingerprint ->
            fingerprint.size == SerializedIrFileSha256Fingerprint.HASHES_COUNT
        }?.let { fingerprint ->
            SerializedIrFileSha256Fingerprint(
                fingerprint[0].toULong(Character.MAX_RADIX),
                fingerprint[1].toULong(Character.MAX_RADIX),
                fingerprint[2].toULong(Character.MAX_RADIX),
                fingerprint[3].toULong(Character.MAX_RADIX),
                fingerprint[4].toULong(Character.MAX_RADIX),
                fingerprint[5].toULong(Character.MAX_RADIX),
                fingerprint[6].toULong(Character.MAX_RADIX),
                fingerprint[7].toULong(Character.MAX_RADIX),
                fingerprint[8].toULong(Character.MAX_RADIX),
            )
        }
    }
}
