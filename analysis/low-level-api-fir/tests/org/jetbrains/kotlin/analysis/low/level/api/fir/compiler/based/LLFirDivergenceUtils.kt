/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import java.io.File

private const val LL_FIR_DIVERGENCE_DIRECTIVE = "LL_FIR_DIVERGENCE"
private const val LL_FIR_DIVERGENCE_DIRECTIVE_COMMENT = "// $LL_FIR_DIVERGENCE_DIRECTIVE"

fun File.hasLlFirDivergenceDirective(): Boolean = useLines { findDirectiveInLines(it.iterator()) }

fun File.readContentIgnoringLlFirDivergenceDirective(trimLines: Boolean): String =
    useLines { lines ->
        // To ignore `LL_FIR_DIVERGENCE`, we advance `iterator` with `findDirectiveInLines` and then concatenate the rest of the lines.
        val iterator = lines.iterator()
        if (findDirectiveInLines(iterator)) {
            // `trimStart` ensures that the `LL_FIR_DIVERGENCE` directive can be separated from the rest of the file by blank lines.
            iterator.asSequence().concatLines(trimLines).trimStart()
        } else null
    } ?: useLines { lines ->
        // If the directive isn't found, we must read the whole file instead. We have to start another `useLines` because the previous
        // `findDirectiveInLines` might have advanced the iterator past the first line.
        lines.concatLines(trimLines)
    }

private fun Sequence<String>.concatLines(trimLines: Boolean): String =
    if (trimLines) joinToString("\n") { it.trimEnd() }.trimEnd()
    else joinToString("\n")

/**
 * Tries to find the `LL_FIR_DIVERGENCE` directive in the lines given by [iterator] and returns whether this is the case. If the directive
 * was found, [iterator] is guaranteed to be advanced exactly past the `LL_FIR_DIVERGENCE` directive.
 *
 * The format of the directive is as such:
 *
 * ```
 * // LL_FIR_DIVERGENCE
 * // lorem ipsum
 * // dolor sit amet
 * // LL_FIR_DIVERGENCE
 * ```
 *
 * Blank lines before the directive or inside the directive region are ignored.
 */
private fun findDirectiveInLines(iterator: Iterator<String>): Boolean {
    val firstNonBlankLine = iterator.nextNonBlankLineTrimmed()
    if (firstNonBlankLine != LL_FIR_DIVERGENCE_DIRECTIVE_COMMENT) return false

    // Ignore line comments and blank lines until the second (closing) `LL_FIR_DIVERGENCE` is found. Any other text, such as uncommented
    // code inside the directive region, is illegal.
    while (iterator.hasNext()) {
        val line = iterator.nextNonBlankLineTrimmed() ?: return false
        if (line.startsWith("//")) {
            if (line == LL_FIR_DIVERGENCE_DIRECTIVE_COMMENT) return true
        } else return false
    }

    return false
}

private fun Iterator<String>.nextNonBlankLineTrimmed(): String? {
    while (hasNext()) {
        val line = next().trimEnd()
        if (line.isNotEmpty()) return line
    }
    return null
}
