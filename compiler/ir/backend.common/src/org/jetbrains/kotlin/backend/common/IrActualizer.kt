/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.platform.isCommon

object IrActualizer {
    fun actualize(mainFragment: IrModuleFragment, mainSymbolTable: SymbolTable, dependentFragments: List<IrModuleFragment>) {
        linkExpectToActual(mainSymbolTable, dependentFragments)
        mergeIrFragments(mainFragment, dependentFragments)
    }

    private fun linkExpectToActual(mainSymbolTable: SymbolTable, dependentFragments: List<IrModuleFragment>) {
        val actualizer = IrActualizerTransformer(mainSymbolTable)
        for (dependentFragment in dependentFragments) {
            if (dependentFragment.descriptor.platform.isCommon()) {
                dependentFragment.transform(actualizer, null)
            }
        }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        mainFragment.files.addAll(dependentFragments.flatMap { it.files })
    }
}