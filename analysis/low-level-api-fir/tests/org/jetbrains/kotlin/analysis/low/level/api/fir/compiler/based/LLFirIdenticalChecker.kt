/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.isLLFirTestData
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File

class LLFirIdenticalChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    private val helper = object : FirIdenticalCheckerHelper(testServices) {
        override fun getClassicFileToCompare(testDataFile: File): File = testDataFile.originalTestDataFile
        override fun getFirFileToCompare(testDataFile: File): File = testDataFile.firTestDataFile
    }

    override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isNotEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (!testDataFile.isLLFirTestData) return

        // `.ll.kt` test data should not be identical to its base `.fir.kt`/`.kt` test data. If a base `.fir.kt` file does not exist, the
        // base file is the `.kt` file.
        val originalFile = helper.getClassicFileToCompare(testDataFile)
        val baseFile = helper.getFirFileToCompare(originalFile).takeIf { it.exists() } ?: originalFile
        if (helper.contentsAreEquals(baseFile, testDataFile, trimLines = true)) {
            testServices.assertions.fail {
                "`${testDataFile.name}` and `${baseFile.name}` are identical. Remove `$testDataFile`."
            }
        }
    }
}