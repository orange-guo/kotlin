/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.common

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.JvmAtomicSymbols

private const val AFU_PKG = "kotlinx.atomicfu"
private const val TRACE_BASE_TYPE = "TraceBase"
private const val ATOMIC_VALUE_FACTORY = "atomic"
private const val INVOKE = "invoke"
private const val APPEND = "append"
private const val GET = "get"

abstract class AbstractAtomicfuTransformer(
    protected val context: IrPluginContext,
    protected val atomicSymbols: JvmAtomicSymbols
) {
    protected val irBuiltIns = context.irBuiltIns

    protected val AFU_VALUE_TYPES: Map<String, IrType> = mapOf(
        "AtomicInt" to irBuiltIns.intType,
        "AtomicLong" to irBuiltIns.longType,
        "AtomicBoolean" to irBuiltIns.booleanType,
        "AtomicRef" to irBuiltIns.anyNType
    )

    protected val ATOMICFU_INLINE_FUNCTIONS = setOf("loop", "update", "getAndUpdate", "updateAndGet")
    protected val ATOMIC_VALUE_TYPES = setOf("AtomicInt", "AtomicLong", "AtomicBoolean", "AtomicRef")
    protected val ATOMIC_ARRAY_TYPES = setOf("AtomicIntArray", "AtomicLongArray", "AtomicBooleanArray", "AtomicArray")

    fun transform(moduleFragment: IrModuleFragment) {
        transformAtomicFields(moduleFragment)
        transformAtomicExtensions(moduleFragment)
        transformAtomicFunctions(moduleFragment)
        for (irFile in moduleFragment.files) {
            irFile.patchDeclarationParents()
        }
    }

    protected abstract fun transformAtomicFields(moduleFragment: IrModuleFragment)

    protected abstract fun transformAtomicExtensions(moduleFragment: IrModuleFragment)

    protected abstract fun transformAtomicFunctions(moduleFragment: IrModuleFragment)

    // util transformer funcitons

    protected fun IrType.isKotlinxAtomicfuPackage() =
        classFqName?.let { it.parent().asString() == AFU_PKG } ?: false

    // todo replace with fromKotlinxAtomicfu
    protected fun IrSimpleFunctionSymbol.isKotlinxAtomicfuPackage(): Boolean =
        owner.parentClassOrNull?.classId?.let {
            it.packageFqName.asString() == AFU_PKG
        } ?: false

    protected fun fromKotlinxAtomicfu(declaration: IrDeclaration): Boolean =
        declaration is IrProperty &&
                declaration.backingField?.type?.isKotlinxAtomicfuPackage() ?: false

    protected fun IrProperty.isAtomic(): Boolean =
        !isDelegated && backingField?.type?.isAtomicValueType() ?: false

    protected fun IrProperty.isDelegatedToAtomic(): Boolean =
        isDelegated && backingField?.type?.isAtomicValueType() ?: false

    protected fun IrProperty.isAtomicArray(): Boolean =
        backingField?.type?.isAtomicArrayType() ?: false

    protected fun IrProperty.isTrace(): Boolean =
        backingField?.type?.isTraceBaseType() ?: false

    protected fun IrType.isAtomicValueType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_VALUE_TYPES
        } ?: false

    protected fun IrType.isAtomicArrayType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() in ATOMIC_ARRAY_TYPES
        } ?: false

    protected fun IrType.isTraceBaseType() =
        classFqName?.let {
            it.parent().asString() == AFU_PKG && it.shortName().asString() == TRACE_BASE_TYPE
        } ?: false

    protected fun IrCall.isTraceInvoke(): Boolean =
        symbol.isKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == INVOKE &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    protected fun IrCall.isTraceAppend(): Boolean =
        symbol.isKotlinxAtomicfuPackage() &&
                symbol.owner.name.asString() == APPEND &&
                symbol.owner.dispatchReceiverParameter?.type?.isTraceBaseType() == true

    protected fun IrCall.isArrayElementGetter(): Boolean =
        dispatchReceiver?.let {
            it.type.isAtomicArrayType() && symbol.owner.name.asString() == GET
        } ?: false

    protected fun IrType.atomicToValueType(): IrType =
        classFqName?.let {
            AFU_VALUE_TYPES[it.shortName().asString()]
        } ?: error("No corresponding value type was found for this atomic type: ${this.render()}")

    protected fun IrCall.isAtomicFactory(): Boolean =
        symbol.isKotlinxAtomicfuPackage() && symbol.owner.name.asString() == ATOMIC_VALUE_FACTORY &&
                type.isAtomicValueType()

    protected fun IrFunction.isAtomicExtension(): Boolean =
        extensionReceiverParameter?.let { it.type.isAtomicValueType() && this.isInline } ?: false


}
