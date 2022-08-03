/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

internal fun generateApiVersion(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val apiVersionFqName = FqName("org.jetbrains.kotlin.gradle.dsl.ApiVersion")
    filePrinter(file(apiDir, apiVersionFqName)) {
        generateDeclaration("enum class", apiVersionFqName, afterType = "(val version: String)") {
            val languageVersions = LanguageVersion.values()

            val lastIndex = languageVersions.size - 1
            languageVersions.forEachIndexed { index, languageVersion ->
                val lastChar = if (index == lastIndex) ";" else ","
                println("KOTLIN_${languageVersion.major}_${languageVersion.minor}(\"${languageVersion.versionString}\")$lastChar")
            }

            println()
            println("companion object {")
            withIndent {
                println("fun fromVersion(version: String): ApiVersion =")
                println("    ApiVersion.values().firstOrNull { it.version == version }")
                println("        ?: throw IllegalArgumentException(\"Unknown Kotlin api version: ${'$'}version\")")
            }
            println("}")
        }
    }
}
