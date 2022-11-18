/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.isProperExpect
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

object IrActualizer {
    fun actualize(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        val expectActualMap = calculateExpectActualMap(mainFragment, dependentFragments)
        removeExpectDeclaration(dependentFragments)
        linkExpectToActual(expectActualMap, dependentFragments)
        mergeIrFragments(mainFragment, dependentFragments)
    }

    private fun calculateExpectActualMap(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>): Map<IrSymbol, IrSymbol> {
        val result = mutableMapOf<IrSymbol, IrSymbol>()
        val (allActualDeclarations, typeAliasMap) = result.appendExpectActualTypesMap(mainFragment, dependentFragments)
        result.appendExpectActualCallablesMap(allActualDeclarations, typeAliasMap, dependentFragments)
        return result
    }

    private fun MutableMap<IrSymbol, IrSymbol>.appendExpectActualTypesMap(
        mainFragment: IrModuleFragment,
        dependentFragments: List<IrModuleFragment>
    ): Pair<Set<IrDeclaration>, Map<FqName, FqName>> {
        val actualTypes = mutableMapOf<FqName, IrSymbol>()
        val allActualDeclarations = mutableSetOf<IrDeclaration>() // It's needed to extract actual declarations both from library and builtins modules
        val typeAliasMap = mutableMapOf<FqName, FqName>()

        for (file in mainFragment.files) {
            for (declaration in file.declarations) {
                var name: FqName? = null
                var symbol: IrSymbol? = null
                when (declaration) {
                    is IrTypeAlias -> {
                        if (declaration.isActual) {
                            name = declaration.kotlinFqName
                            symbol = declaration.expandedType.classifierOrFail
                            if (symbol is IrClassSymbol) {
                                allActualDeclarations.add(symbol.owner)
                                typeAliasMap[name] = symbol.owner.kotlinFqName
                            }
                        }
                    }
                    is IrClass -> {
                        if (!declaration.isExpect) {
                            name = declaration.kotlinFqName
                            symbol = declaration.symbol
                            allActualDeclarations.add(declaration)
                        }
                    }
                    is IrEnumEntry -> {
                        if (!declaration.isProperExpect) {
                            name = FqName.fromSegments(listOf(declaration.parent.kotlinFqName.asString(), declaration.name.asString()))
                            symbol = declaration.symbol
                            allActualDeclarations.add(declaration)
                        }
                    }
                    else -> {
                        if (!declaration.isProperExpect) {
                            allActualDeclarations.add(declaration)
                        }
                    }
                }
                if (name != null && symbol != null) {
                    actualTypes[name] = symbol
                }
                if (declaration is IrTypeParametersContainer && !declaration.isProperExpect) {
                    for (typeParameter in declaration.typeParameters) {
                        actualTypes[FqName.fromSegments(listOf(typeParameter.parent.kotlinFqName.asString(), typeParameter.name.asString()))] = typeParameter.symbol
                    }
                }
            }
        }

        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                for (declaration in file.declarations) {
                    when (declaration) {
                        is IrClass -> {
                            if (declaration.isExpect) {
                                linkActualOrReportMissing(actualTypes, declaration, declaration.kotlinFqName)
                            }
                        }
                        is IrEnumEntry -> {
                            if (declaration.isProperExpect) {
                                linkActualOrReportMissing(
                                    actualTypes,
                                    declaration,
                                    FqName.fromSegments(listOf(declaration.parent.kotlinFqName.asString(), declaration.name.asString()))
                                )
                            }
                        }
                    }
                    if (declaration is IrTypeParametersContainer && declaration.isProperExpect) {
                        for (typeParameter in declaration.typeParameters) {
                            linkActualOrReportMissing(
                                actualTypes,
                                typeParameter,
                                FqName.fromSegments(listOf(typeParameter.parent.kotlinFqName.asString(), typeParameter.name.asString()))
                            )
                        }
                    }
                }
            }
        }

        return allActualDeclarations to typeAliasMap
    }

    private fun MutableMap<IrSymbol, IrSymbol>.linkActualOrReportMissing(
        actualTypes: MutableMap<FqName, IrSymbol>,
        expectElement: IrSymbolOwner,
        actualTypeId: FqName
    ) {
        val actualDeclaration = actualTypes[actualTypeId]
        if (actualDeclaration != null) {
            this[expectElement.symbol] = actualDeclaration
        } else {
            reportMissingActual(expectElement)
        }
    }

    private fun MutableMap<IrSymbol, IrSymbol>.appendExpectActualCallablesMap(
        allActualDeclarations: Set<IrDeclaration>,
        typeAliasMap: Map<FqName, FqName>,
        dependentFragments: List<IrModuleFragment>
    ) {
        val actualFunctions = mutableMapOf<CallableId, MutableList<IrFunction>>()
        val actualProperties = mutableMapOf<CallableId, IrProperty>()

        fun collectActuals(declaration: IrDeclaration) {
            if (declaration.isProperExpect) return
            when (declaration) {
                is IrFunction -> {
                    actualFunctions.getOrPut(CallableId(declaration.parent.kotlinFqName, declaration.name)) {
                        mutableListOf()
                    }.add(declaration)
                }
                is IrProperty -> {
                    actualProperties.getOrPut(CallableId(declaration.parent.kotlinFqName, declaration.name)) {
                        declaration
                    }
                }
                is IrClass -> {
                    for (member in declaration.declarations) {
                        collectActuals(member)
                    }
                }
            }
        }

        for (declaration in allActualDeclarations) {
            collectActuals(declaration)
        }

        fun actualizeCallable(declaration: IrDeclarationWithName): CallableId {
            val fullName = declaration.parent.kotlinFqName
            return CallableId(typeAliasMap[fullName] ?: fullName, declaration.name)
        }

        fun linkExpectToActual(declarationContainer: IrDeclarationContainer) {
            for (declaration in declarationContainer.declarations) {
                if (!declaration.isProperExpect) continue
                when (declaration) {
                    is IrFunction -> {
                        val functions = actualFunctions[actualizeCallable(declaration)]
                        var isFunctionFound = false
                        if (functions != null) {
                            for (actualFunction in functions) {
                                if (checkParameters(declaration, actualFunction, this)) {
                                    this[declaration.symbol] = actualFunction.symbol
                                    isFunctionFound = true
                                    break
                                }
                            }
                        }
                        if (!isFunctionFound) {
                            reportMissingActual(declaration)
                        }
                    }
                    is IrProperty -> {
                        val properties = actualProperties[actualizeCallable(declaration)]
                        if (properties != null) {
                            this[declaration.symbol] = properties.symbol
                            declaration.getter?.symbol?.let {
                                this[it] = properties.getter!!.symbol
                            }
                            declaration.setter?.symbol?.let {
                                this[it] = properties.setter!!.symbol
                            }
                        } else {
                            reportMissingActual(declaration)
                        }
                    }
                    is IrClass -> {
                        linkExpectToActual(declaration)
                    }
                }
            }
        }

        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                linkExpectToActual(file)
            }
        }
    }

    private fun checkParameters(
        expectFunction: IrFunction,
        actualFunction: IrFunction,
        expectActualTypesMap: Map<IrSymbol, IrSymbol>
    ): Boolean {
        if (expectFunction.valueParameters.size != actualFunction.valueParameters.size) return false
        for ((expectParameter, actualParameter) in expectFunction.valueParameters.zip(actualFunction.valueParameters)) {
            val expectParameterTypeSymbol = expectParameter.type.classifierOrFail
            val actualizedParameterTypeSymbol = expectActualTypesMap[expectParameterTypeSymbol] ?: expectParameterTypeSymbol
            if (actualizedParameterTypeSymbol != actualParameter.type.classifierOrNull) {
                return false
            }
        }
        return true
    }

    private fun reportMissingActual(irElement: IrElement) {
        throw AssertionError("Missing actual for ${irElement.render()}") // TODO: set up diagnostics reporting
    }

    private fun removeExpectDeclaration(dependentFragments: List<IrModuleFragment>) {
        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                file.declarations.removeAll { it.isProperExpect }
            }
        }
    }

    private fun linkExpectToActual(expectActualMap: Map<IrSymbol, IrSymbol>, dependentFragments: List<IrModuleFragment>) {
        val actualizer = IrActualizerTransformer(expectActualMap)
        for (dependentFragment in dependentFragments) {
            dependentFragment.transform(actualizer, null)
        }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        mainFragment.files.addAll(dependentFragments.flatMap { it.files })
    }
}