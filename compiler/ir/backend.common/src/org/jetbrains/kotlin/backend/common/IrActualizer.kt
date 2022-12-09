/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable

object IrActualizer {
    fun actualize(mainFragment: IrModuleFragment, mainSymbolTable: SymbolTable, dependentFragments: List<IrModuleFragment>) {
        linkExpectToActual(mainSymbolTable, dependentFragments)
        mergeIrFragments(mainFragment, dependentFragments)
    }

    private fun linkExpectToActual(mainSymbolTable: SymbolTable, dependentFragments: List<IrModuleFragment>) {
        val actualizer = IrActualizerTransformer(mainSymbolTable)
        for (dependentFragment in dependentFragments) {
            dependentFragment.transform(actualizer, null)
        }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        mainFragment.files.addAll(0, dependentFragments.flatMap { it.files })
    }
}