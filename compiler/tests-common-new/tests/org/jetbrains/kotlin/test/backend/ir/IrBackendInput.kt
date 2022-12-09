/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule

// IR backend (JVM, JS, Native)
sealed class IrBackendInput : ResultingArtifact.BackendInput<IrBackendInput>() {
    override val kind: BackendKinds.IrBackend
        get() = BackendKinds.IrBackend

    abstract val irModuleFragment: IrModuleFragment

    data class JsIrBackendInput(
        val module: TestModule,
        val irModuleFragments: List<IrModuleFragment>,
        val sourceFiles: List<KtSourceFile>,
        val icData: List<KotlinFileSerializedData>,
        val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>, // TODO: abstract from descriptors
        val hasErrors: Boolean,
        val symbolTable: SymbolTable,
        val serializeSingleFile: (KtSourceFile) -> ProtoBuf.PackageFragment
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = irModuleFragments.last()
    }

    data class JvmIrBackendInput(
        val state: GenerationState,
        val codegenFactory: JvmIrCodegenFactory,
        val backendInput: List<JvmIrCodegenFactory.JvmIrBackendInput>,
        val sourceFiles: List<KtSourceFile>
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = backendInput.last().irModuleFragment
    }
}
