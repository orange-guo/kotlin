/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.native

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm.JvmAtomicSymbols
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlinx.atomicfu.compiler.backend.addDefaultGetter
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuTransformer

class AtomicfuNativeIrTransformer(
    context: IrPluginContext,
    atomicSymbols: JvmAtomicSymbols
) : AbstractAtomicfuTransformer(context, atomicSymbols) {

    override fun transformAtomicFields(moduleFragment: IrModuleFragment) {
        for (irFile in moduleFragment.files) {
            irFile.transform(AtomicTransformer(), null)
        }
    }

    override fun transformAtomicExtensions(moduleFragment: IrModuleFragment) {

    }

    override fun transformAtomicFunctions(moduleFragment: IrModuleFragment) {

    }

    private inner class AtomicTransformer : IrElementTransformer<IrFunction?> {

        override fun visitClass(declaration: IrClass, data: IrFunction?): IrStatement {
            declaration.declarations.filter(::fromKotlinxAtomicfu).forEach {
                (it as IrProperty).transformAtomicProperty(declaration)
            }
            return super.visitClass(declaration, data)
        }

        override fun visitFile(declaration: IrFile, data: IrFunction?): IrFile {
            declaration.declarations.filter(::fromKotlinxAtomicfu).forEach {
                (it as IrProperty).transformAtomicProperty(declaration)
            }
            return super.visitFile(declaration, data)
        }

        private fun IrProperty.transformAtomicProperty(parentClass: IrDeclarationContainer) {
            // todo can I get parent from the parent field of the property?
            // Atomic property is replaced with the
            // For now supported for primitive types (int, long, boolean) and for linuxX64 and macosX64
            //  val a = atomic(0)
            //  @Volatile var a: Int = 0
            backingField = buildVolatileRawField(this, parentClass)
            // update property accessors
            context.addDefaultGetter(this, parentClass)
        }

        private fun buildVolatileRawField(property: IrProperty, parent: IrDeclarationContainer): IrField =
            // Generate a new backing field for the given property:
            // a volatile variable of the atomic value type
            // val a = atomic(0)
            // volatile var a: Int = 0
            property.backingField?.let { backingField ->
                val valueType = backingField.type.atomicToValueType()
                context.irFactory.buildField {
                    name = property.name
                    type = if (valueType.isBoolean()) irBuiltIns.intType else valueType
                    visibility = backingField.visibility // private
                    isFinal = false
                    isStatic = parent is IrFile
                }.apply {
                    annotations = backingField.annotations + atomicSymbols.volatileAnnotationConstructorCall
                    this.parent = parent
                }
            } ?: error("Backing field of the atomic property ${property.render()} is null")
    }
}
