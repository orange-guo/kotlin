/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.PackageName
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFile
import org.jetbrains.kotlin.konan.blackboxtest.support.TestKind
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule
import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunnerType
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_MODULE_NAME
import java.io.File

abstract class AbstractNativeStandaloneTest : AbstractNativeSimpleTest() {
    val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir
}

internal class NativeStandaloneTestHelper(val test: AbstractNativeStandaloneTest) {
    fun compileToLibrary(sourcesDir: File, vararg dependencies: TestCompilationArtifact.KLIB) =
        compileToLibrary(sourcesDir, test.buildDir, *dependencies)

    fun compileToLibrary(
        sourcesDir: File,
        outputDir: File,
        vararg dependencies: TestCompilationArtifact.KLIB
    ): TestCompilationArtifact.KLIB {
        val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir)
        val compilationResult = testCase.compileToLibrary(outputDir, dependencies.map { it.asLibraryDependency() })
        return compilationResult.resultingArtifact
    }

    fun compileToExecutable(
        sourcesDir: File,
        freeCompilerArgs: TestCompilerArgs,
        vararg dependencies: TestCompilationArtifact.KLIB
    ): TestCompilationArtifact.Executable {
        val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir, freeCompilerArgs)
        val compilationResult = testCase.compileToExecutable(dependencies.map { it.asLibraryDependency() })
        return compilationResult.resultingArtifact
    }

    fun generateTestCaseWithSingleModule(moduleDir: File?, freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY): TestCase {
        val moduleName: String = moduleDir?.name ?: LAUNCHER_MODULE_NAME
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet())

        moduleDir?.walkTopDown()
            ?.filter { it.isFile && it.extension == "kt" }
            ?.forEach { file -> module.files += TestFile.createCommitted(file, module) }

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = freeCompilerArgs,
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(test.testRunSettings.get<Timeouts>().executionTimeout),
            extras = DEFAULT_EXTRAS
        ).apply {
            initialize(null, null)
        }
    }

    fun TestCase.compileToLibrary(vararg dependencies: TestCompilationDependency<*>) =
        compileToLibrary(test.buildDir, dependencies.asList())

    private fun TestCase.compileToLibrary(
        dir: File,
        dependencies: List<TestCompilationDependency<*>>
    ): TestCompilationResult.Success<out TestCompilationArtifact.KLIB> {
        val compilation = LibraryCompilation(
            settings = test.testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            dependencies = dependencies,
            expectedArtifact = toLibraryArtifact(dir)
        )
        return compilation.result.assertSuccess()
    }

    fun TestCase.compileToExecutable(vararg dependencies: TestCompilationDependency<*>) =
        compileToExecutable(dependencies.asList())

    private fun TestCase.compileToExecutable(dependencies: List<TestCompilationDependency<*>>): TestCompilationResult.Success<out TestCompilationArtifact.Executable> {
        val compilation = ExecutableCompilation(
            settings = test.testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            extras = DEFAULT_EXTRAS,
            dependencies = dependencies,
            expectedArtifact = toExecutableArtifact()
        )
        return compilation.result.assertSuccess()
    }

    fun TestCompilationArtifact.KLIB.asLibraryDependency() = ExistingDependency(this, TestCompilationDependencyType.Library)
    fun TestCompilationArtifact.KLIB.asIncludedLibraryDependency() = ExistingDependency(this, TestCompilationDependencyType.IncludedLibrary)

    private fun TestCase.toLibraryArtifact(dir: File) = TestCompilationArtifact.KLIB(dir.resolve(modules.first().name + ".klib"))
    private fun toExecutableArtifact() =
        TestCompilationArtifact.Executable(test.buildDir.resolve("app." + test.testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))

    companion object {
        private val DEFAULT_EXTRAS = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
    }
}
