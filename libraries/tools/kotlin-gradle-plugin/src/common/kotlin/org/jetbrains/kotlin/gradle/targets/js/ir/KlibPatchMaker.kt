/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.incremental.createDirectory
import java.io.BufferedInputStream
import java.io.File

class KlibPatchMaker(private val linkTask: KotlinJsIrLink, private val threshold: Int) {
    private val klibDiffDir = linkTask.project.buildDir.resolve("klib-diff")

    private var currentGeneration: Int = -1

    private val patchExtension: String
        get() {
            if (currentGeneration == -1) {
                val genFile = klibDiffDir.resolve("_generation")
                if (genFile.exists()) {
                    currentGeneration = genFile.readText().toInt() + 1
                } else {
                    currentGeneration = 1
                }
                genFile.writeText(currentGeneration.toString())
            }
            return "${currentGeneration}.bspatch"
        }

    private val bspatchRegex = Regex("^.+\\.(\\d+)\\.bspatch$")

    private fun isKLibFilesEqual(firstFile: File, secondFile: File): Boolean {
        if (firstFile.length() != secondFile.length()) {
            return false
        }

        BufferedInputStream(firstFile.inputStream()).use { bf1 ->
            BufferedInputStream(secondFile.inputStream()).use { bf2 ->
                while (true) {
                    val c = bf1.read()
                    when {
                        c == -1 -> return true
                        c != bf2.read() -> return false
                    }
                }
            }
        }
    }

    private fun removeOutdatedPatches() {
        if (currentGeneration <= threshold) {
            return
        }

        for (diffFile in klibDiffDir.walk()) {
            val m = bspatchRegex.matchEntire(diffFile.path) ?: continue
            val fileGeneration = m.groups[1]?.value?.toInt() ?: 0
            if (currentGeneration - fileGeneration >= threshold) {
                diffFile.delete()
            }
        }
    }

    fun saveKLibPatches() {
        println("Saving klibs patches...")
        klibDiffDir.createDirectory()

        for (newKLibFile in linkTask.libraries) {
            if (!newKLibFile.path.endsWith(".klib")) {
                continue
            }
            val prevKLibFile = klibDiffDir.resolve(newKLibFile.name)
            if (prevKLibFile.exists()) {
                if (isKLibFilesEqual(prevKLibFile, newKLibFile)) {
                    continue
                }
                val diffFile = klibDiffDir.resolve("${newKLibFile.name}.${patchExtension}")
                linkTask.project.exec { spec ->
                    spec.workingDir = klibDiffDir
                    spec.executable = "bsdiff"
                    // Usage: bsdiff OLD_FILE NEW_FILE OUT_PATCH_FILE
                    // Intentionally make a patch newKLibFile -> prevKLibFile
                    // So to restore we need to apply the patches to stored klib file
                    spec.args = listOf(newKLibFile.absolutePath, prevKLibFile.absolutePath, diffFile.absolutePath)
                }
            }
            newKLibFile.copyTo(prevKLibFile, true)
        }

        removeOutdatedPatches()
    }
}
