/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("warnings")
package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createExtensionReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn

/*
 * For classes annotated with @AllPropertiesConstructor and with no-arg constructor generates
 *   constructor which takes value parameters corresponding to all properties
 *
 * Parent class should be Any or class, annotated with @AllPropertiesConstructor
 */
class AllPropertiesConstructorIrGenerator(val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val ANNOTATION_FQN = "AllPropertiesConstructor".fqn()
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation()) {
            declaration.declarations += getOrGenerateMembersIfNeeded(declaration)
        }
        visitElement(declaration)
    }

    private val generatedConstructors = mutableMapOf<IrClass, List<IrFunction>>()

    private fun getOrGenerateMembersIfNeeded(klass: IrClass): List<IrFunction> = generatedConstructors.getOrPut(klass) {
        buildList {
            this += buildConstructor(klass)

            if (klass.declarations.none { it is IrFunction && it.name == Name.identifier("hasExtension") }) {
                this += context.irFactory.buildFun {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    name = Name.identifier("hasExtension")
                    returnType = context.irBuiltIns.unitType
                }.apply {
                    parent = klass
                    parameters += createDispatchReceiverParameterWithClassParent()
                    parameters += createExtensionReceiver(context.irBuiltIns.stringType)
                    body = context.irFactory.createBlockBody(startOffset, endOffset, emptyList())
                    context.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(this)
                }
            }
            if (klass.declarations.none { it is IrFunction && it.name == Name.identifier("hasContext") }) {
                this += context.irFactory.buildFun {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    name = Name.identifier("hasContext")
                    returnType = context.irBuiltIns.unitType
                }.apply {
                    parent = klass
                    parameters += createDispatchReceiverParameterWithClassParent()
                    parameters += buildValueParameter(this) {
                        kind = IrParameterKind.Context
                        name = Name.identifier("c")
                        type = context.irBuiltIns.stringType
                    }
                    body = context.irFactory.createBlockBody(startOffset, endOffset, emptyList())
                    context.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(this)
                }
            }
        }
    }

    private fun buildConstructor(klass: IrClass): IrConstructor {
        val superClass =
            klass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS } ?: context.irBuiltIns.anyClass.owner

        val properties =
            klass.properties.toList().sortedWith(Comparator.comparing { if (it.origin == IrDeclarationOrigin.FAKE_OVERRIDE) 0 else 1 })
        val overriddenProperties = properties.takeWhile { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
        val superConstructor = when {
            superClass.defaultType.isAny() -> superClass.constructors.singleOrNull { c -> c.parameters.none { it.kind == IrParameterKind.Regular } }
            else -> {
                require(superClass.hasAnnotation())
                superClass.constructors.singleOrNull { c -> c.parameters.any { it.kind == IrParameterKind.Regular } }
            }
        } ?: error("All properies constructor not found")

        return context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = klass.defaultType
        }.also { ctor ->
            ctor.parent = klass
            ctor.parameters = properties.map { property ->
                buildValueParameter(ctor) {
                    type = property.getter!!.returnType
                    name = property.name
                    kind = IrParameterKind.Regular
                }
            }
            ctor.body = context.irFactory.createBlockBody(
                ctor.startOffset, ctor.endOffset,
                listOf(
                    IrDelegatingConstructorCallImpl(
                        ctor.startOffset, ctor.endOffset, context.irBuiltIns.unitType,
                        superConstructor.symbol, 0,
                    ).apply {
                        ctor.parameters.filter { it.kind == IrParameterKind.Regular }
                            .take(overriddenProperties.size)
                            .forEachIndexed { index, parameter ->
                                arguments[index] = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, parameter.symbol)
                            }
                    }
                )
            )
            context.metadataDeclarationRegistrar.registerConstructorAsMetadataVisible(ctor)
        }
    }

    private fun IrClass.hasAnnotation(): Boolean {
        return annotations.hasAnnotation(ANNOTATION_FQN)
    }
}
