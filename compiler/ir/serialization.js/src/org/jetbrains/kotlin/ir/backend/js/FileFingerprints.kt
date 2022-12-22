/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.library.SerializedIrModule
import java.io.File
import java.security.MessageDigest
import java.util.*

private const val FILE_FINGERPRINTS_SEPARATOR = " "
private const val FILE_FINGERPRINTS_KEY_VALUE_SEPARATOR = "-"

internal fun SerializedIrModule.calculateSerializedIrFileSha256Fingerprints() = buildString {
    val fileDigest = MessageDigest.getInstance("SHA-256")

    fun flushAndAppendDigest() {
        append(Base64.getEncoder().encodeToString(fileDigest.digest()))
        fileDigest.reset()
    }

    for (file in files.withIndex()) {
        if (file.index != 0) {
            append(FILE_FINGERPRINTS_SEPARATOR)
        }

        fileDigest.update(file.value.path.toByteArray())
        flushAndAppendDigest()

        append(FILE_FINGERPRINTS_KEY_VALUE_SEPARATOR)

        fileDigest.update(file.value.fileData)
        fileDigest.update(file.value.fqName.toByteArray())
        fileDigest.update(file.value.types)
        fileDigest.update(file.value.signatures)
        fileDigest.update(file.value.strings)
        fileDigest.update(file.value.bodies)
        fileDigest.update(file.value.declarations)
        fileDigest.update(file.value.debugInfo ?: ByteArray(0))

        flushAndAppendDigest()
    }
}

class SerializedIrFileSha256Fingerprint(val filePathSha256: ByteArray, val fingerprintSha256: ByteArray)

internal fun String.parseSerializedIrFileSha256Fingerprints(): List<SerializedIrFileSha256Fingerprint> {
    return split(FILE_FINGERPRINTS_SEPARATOR).mapNotNull { fileFingerprintLine ->
        fileFingerprintLine.split(FILE_FINGERPRINTS_KEY_VALUE_SEPARATOR).takeIf { fingerprint ->
            fingerprint.size == 2
        }?.let { fingerprint ->
            SerializedIrFileSha256Fingerprint(Base64.getDecoder().decode(fingerprint[0]), Base64.getDecoder().decode(fingerprint[1]))
        }
    }
}
