/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.ir.backend.js.jsOutputName
import org.jetbrains.kotlin.ir.backend.js.serializedIrFileFingerprints
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import java.io.File

internal interface KotlinLibraryHeader {
    val libraryFile: KotlinLibraryFile

    val libraryFingerprint: ICHash?

    val sourceFileDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer>
    val sourceFileFingerprints: Map<KotlinSourceFile, ICHash>

    val jsOutputName: String?
}

internal class KotlinLoadedLibraryHeader(private val library: KotlinLibrary, private val icHasher: ICHasher) : KotlinLibraryHeader {
    private fun ByteArray.toICHash(): ICHash? {
        return takeIf { size == ICHash.HASH_BYTES }?.let { ICHash(it) }
    }

    private fun parseFingerprintsFromManifest(): Map<KotlinSourceFile, ICHash>? {
        val manifestFingerprints = library.serializedIrFileFingerprints ?: return null
        if (manifestFingerprints.size != sourceFiles.size) {
            return null
        }
        val sourceFilePathHashes = sourceFiles.associateBy {
            icHasher.calculateByteArrayHash(it.path.toByteArray())
        }
        val fingerprints = HashMap<KotlinSourceFile, ICHash>(sourceFiles.size)
        for (manifestFingerprint in manifestFingerprints) {
            val filePathHash = manifestFingerprint.filePathSha256.toICHash() ?: return null
            val sourceFile = sourceFilePathHashes[filePathHash] ?: return null
            fingerprints[sourceFile] = manifestFingerprint.fingerprintSha256.toICHash() ?: return null
        }
        return fingerprints
    }

    private val fingerprintsFromManifest: Map<KotlinSourceFile, ICHash>? by lazy { parseFingerprintsFromManifest() }

    override val libraryFile: KotlinLibraryFile = KotlinLibraryFile(library)

    override val libraryFingerprint: ICHash by lazy {
        fingerprintsFromManifest?.entries?.sortedBy { it.key.path }?.let { fingerprints ->
            ICHash().also { hashByFingerprints ->
                fingerprints.forEach { hashByFingerprints.combineWithAndUpdate(it.value) }
            }
        } ?: icHasher.calculateFileHash(File(libraryFile.path))
    }

    override val sourceFileDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer> by lazy {
        buildMapUntil(sourceFiles.size) {
            val deserializer = IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
                private fun err(): Nothing = icError("Not supported")
                override fun irDeclaration(index: Int): ByteArray = err()
                override fun type(index: Int): ByteArray = err()
                override fun signature(index: Int): ByteArray = library.signature(index, it)
                override fun string(index: Int): ByteArray = library.string(index, it)
                override fun body(index: Int): ByteArray = err()
                override fun debugInfo(index: Int): ByteArray? = null
            }), null)

            put(sourceFiles[it], deserializer)
        }
    }

    override val sourceFileFingerprints: Map<KotlinSourceFile, ICHash> by lazy {
        fingerprintsFromManifest ?: buildMapUntil(sourceFiles.size) {
            put(sourceFiles[it], icHasher.calculateLibrarySrcFileHash(library, it))
        }
    }

    override val jsOutputName: String?
        get() = library.jsOutputName

    private val sourceFiles by lazy {
        val extReg = ExtensionRegistryLite.newInstance()
        Array(library.fileCount()) {
            val fileProto = IrFile.parseFrom(library.file(it).codedInputStream, extReg)
            KotlinSourceFile(fileProto.fileEntry.name)
        }
    }
}

internal class KotlinRemovedLibraryHeader(private val libCacheDir: File) : KotlinLibraryHeader {
    override val libraryFile: KotlinLibraryFile
        get() = icError("removed library name is unavailable; cache dir: ${libCacheDir.absolutePath}")

    override val libraryFingerprint: ICHash? get() = null

    override val sourceFileDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer> get() = emptyMap()
    override val sourceFileFingerprints: Map<KotlinSourceFile, ICHash> get() = emptyMap()

    override val jsOutputName: String? get() = null
}
