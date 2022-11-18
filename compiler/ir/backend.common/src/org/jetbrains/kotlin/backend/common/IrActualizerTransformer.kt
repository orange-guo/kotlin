/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class IrActualizerTransformer(private val expectActualMap: Map<IrSymbol, IrSymbol>) : IrElementTransformerVoid() {
    override fun visitScript(declaration: IrScript) =
        (visitDeclaration(declaration) as IrScript).also {
            it.baseClass = it.baseClass?.actualizeType()
        }

    override fun visitClass(declaration: IrClass) =
        (visitDeclaration(declaration) as IrClass).also {
            it.superTypes = it.superTypes.map { superType -> superType.actualizeType() }
        }

    override fun visitFunction(declaration: IrFunction) =
        (visitDeclaration(declaration) as IrFunction).also {
            it.returnType = it.returnType.actualizeType()
        }

    override fun visitField(declaration: IrField) =
        (visitDeclaration(declaration) as IrField).also {
            it.type = it.type.actualizeType()
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) =
        (visitDeclaration(declaration) as IrLocalDelegatedProperty).also {
            it.type = it.type.actualizeType()
        }

    override fun visitTypeParameter(declaration: IrTypeParameter) =
        (visitDeclaration(declaration) as IrTypeParameter).also {
            it.superTypes = it.superTypes.map { superType -> superType.actualizeType() }
        }

    override fun visitValueParameter(declaration: IrValueParameter) =
        (visitDeclaration(declaration) as IrValueParameter).also {
            it.type = it.type.actualizeType()
            it.varargElementType = it.varargElementType?.actualizeType()
        }

    override fun visitVariable(declaration: IrVariable) =
        (visitDeclaration(declaration) as IrVariable).also {
            it.type = it.type.actualizeType()
        }

    override fun visitTypeAlias(declaration: IrTypeAlias) =
        (visitDeclaration(declaration) as IrTypeAlias).also {
            it.expandedType = it.expandedType.actualizeType()
        }

    override fun visitExpression(expression: IrExpression) = run {
        expression.transformChildren(this, null)
        expression.type = expression.type.actualizeType()
        expression
    }

    override fun visitConstantObject(expression: IrConstantObject) =
        (visitConstantValue(expression) as IrConstantObject).let {
            IrConstantObjectImpl(
                it.startOffset,
                it.endOffset,
                it.constructor.actualizeSymbol(),
                it.valueArguments,
                it.typeArguments.map { typeArgument -> typeArgument.actualizeType() },
                it.type.actualizeType()
            )
        }

    override fun visitVararg(expression: IrVararg) =
        (visitExpression(expression) as IrVararg).also {
            it.varargElementType = it.varargElementType.actualizeType()
        }

    override fun visitGetObjectValue(expression: IrGetObjectValue) =
        (visitSingletonReference(expression) as IrGetObjectValue).let {
            IrGetObjectValueImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                it.symbol.actualizeSymbol(),
            )
        }

    override fun visitGetEnumValue(expression: IrGetEnumValue) =
        (visitSingletonReference(expression) as IrGetEnumValue).let {
            IrGetEnumValueImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                it.symbol.actualizeSymbol(),
            )
        }

    override fun visitGetValue(expression: IrGetValue) =
        (visitValueAccess(expression) as IrGetValue).let {
            IrGetValueImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                it.symbol.actualizeSymbol(),
                it.origin,
            )
        }

    override fun visitSetValue(expression: IrSetValue) =
        (visitValueAccess(expression) as IrSetValue).let {
            IrSetValueImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                it.symbol.actualizeSymbol(),
                it.value,
                it.origin,
            )
        }

    override fun visitGetField(expression: IrGetField) =
        (visitFieldAccess(expression) as IrGetField).let {
            IrGetFieldImpl(
                it.startOffset,
                it.endOffset,
                it.symbol.actualizeSymbol(),
                it.type,
                it.receiver,
                it.origin,
            )
        }

    override fun visitSetField(expression: IrSetField) =
        (visitFieldAccess(expression) as IrSetField).let {
            IrSetFieldImpl(
                it.startOffset,
                it.endOffset,
                it.symbol.actualizeSymbol(),
                it.receiver,
                it.value,
                it.type,
                it.origin,
                it.superQualifierSymbol?.actualizeSymbol()
            )
        }

    override fun visitCall(expression: IrCall) =
        (visitFunctionAccess(expression) as IrCall).let { irCall ->
            IrCallImpl(
                irCall.startOffset,
                irCall.endOffset,
                irCall.type,
                irCall.symbol.actualizeSymbol(),
                irCall.typeArgumentsCount,
                irCall.valueArgumentsCount,
                irCall.origin,
                irCall.superQualifierSymbol?.actualizeSymbol()
            ).also {
                it.actualizeMemberAccessExpression(irCall)
            }
        }

    override fun visitConstructorCall(expression: IrConstructorCall) =
        (visitFunctionAccess(expression) as IrConstructorCall).let { irCall ->
            IrConstructorCallImpl(
                irCall.startOffset,
                irCall.endOffset,
                irCall.type,
                irCall.symbol.actualizeSymbol(),
                irCall.typeArgumentsCount,
                irCall.constructorTypeArgumentsCount,
                irCall.valueArgumentsCount,
                irCall.origin,
                irCall.source
            ).also {
                it.actualizeMemberAccessExpression(irCall)
                for (index in 0 until irCall.constructorTypeArgumentsCount) {
                    it.putConstructorTypeArgument(index, irCall.getConstructorTypeArgument(index)?.actualizeType())
                }
            }
        }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) =
        (visitFunctionAccess(expression) as IrDelegatingConstructorCall).let { irCall ->
            IrDelegatingConstructorCallImpl(
                irCall.startOffset,
                irCall.endOffset,
                irCall.type,
                irCall.symbol.actualizeSymbol(),
                irCall.typeArgumentsCount,
                irCall.valueArgumentsCount
            ).also {
                it.actualizeMemberAccessExpression(irCall)
            }
        }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) =
        (visitFunctionAccess(expression) as IrEnumConstructorCall).let { irCall ->
            IrEnumConstructorCallImpl(
                irCall.startOffset,
                irCall.endOffset,
                irCall.type,
                irCall.symbol.actualizeSymbol(),
                irCall.typeArgumentsCount,
                irCall.valueArgumentsCount
            ).also {
                it.actualizeMemberAccessExpression(irCall)
            }
        }

    override fun visitFunctionReference(expression: IrFunctionReference) =
        (visitCallableReference(expression) as IrFunctionReference).let { irReference ->
            IrFunctionReferenceImpl(
                irReference.startOffset,
                irReference.endOffset,
                irReference.type,
                irReference.symbol.actualizeSymbol(),
                irReference.typeArgumentsCount,
                irReference.valueArgumentsCount,
                irReference.reflectionTarget?.actualizeSymbol(),
                irReference.origin
            ).also {
                it.actualizeMemberAccessExpression(irReference)
            }
        }

    override fun visitPropertyReference(expression: IrPropertyReference) =
        (visitCallableReference(expression) as IrPropertyReference).let { irReference ->
            IrPropertyReferenceImpl(
                irReference.startOffset,
                irReference.endOffset,
                irReference.type,
                irReference.symbol.actualizeSymbol(),
                irReference.typeArgumentsCount,
                irReference.field?.actualizeSymbol(),
                irReference.getter?.actualizeSymbol(),
                irReference.setter?.actualizeSymbol(),
                irReference.origin
            ).also {
                it.actualizeMemberAccessExpression(irReference)
            }
        }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) =
        (visitCallableReference(expression) as IrLocalDelegatedPropertyReference).let { irReference ->
            IrLocalDelegatedPropertyReferenceImpl(
                irReference.startOffset,
                irReference.endOffset,
                irReference.type,
                irReference.symbol.actualizeSymbol(),
                irReference.delegate.actualizeSymbol(),
                irReference.getter.actualizeSymbol(),
                irReference.setter?.actualizeSymbol(),
                irReference.origin
            ).also {
                it.actualizeMemberAccessExpression(irReference)
            }
        }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference) =
        (visitDeclarationReference(expression) as IrRawFunctionReference).let {
            IrRawFunctionReferenceImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                it.symbol.actualizeSymbol()
            )
        }

    override fun visitClassReference(expression: IrClassReference) =
        (visitDeclarationReference(expression) as IrClassReference).let {
            IrClassReferenceImpl(
                it.startOffset,
                it.endOffset,
                it.type,
                it.symbol.actualizeSymbol(),
                it.classType.actualizeType()
            )
        }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) =
        (visitExpression(expression) as IrInstanceInitializerCall).let {
            IrInstanceInitializerCallImpl(
                it.startOffset,
                it.endOffset,
                it.classSymbol.actualizeSymbol(),
                it.type
            )
        }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) =
        (visitExpression(expression) as IrTypeOperatorCall).also {
            it.typeOperand = it.typeOperand.actualizeType()
        }

    private inline fun <reified S : IrSymbol> IrMemberAccessExpression<S>.actualizeMemberAccessExpression(irMemberAccess: IrMemberAccessExpression<S>) {
        this.dispatchReceiver = irMemberAccess.dispatchReceiver
        this.extensionReceiver = irMemberAccess.extensionReceiver
        for (index in 0 until irMemberAccess.valueArgumentsCount) {
            this.putValueArgument(index, irMemberAccess.getValueArgument(index))
        }
        for (index in 0 until irMemberAccess.typeArgumentsCount) {
            this.putTypeArgument(index, irMemberAccess.getTypeArgument(index)?.actualizeType())
        }
    }

    private fun IrType.actualizeType(): IrType {
        if (this !is IrSimpleType) return this
        return IrSimpleTypeImpl(
            classifier.actualizeSymbol(),
            nullability,
            if (arguments.isNotEmpty()) {
                arguments.map { (it.typeOrNull?.actualizeType() as? IrTypeArgument) ?: it }
            } else {
                arguments
            },
            if (annotations.isNotEmpty()) annotations.map { visitConstructorCall(it) } else emptyList(),
            abbreviation?.let { irTypeAbbreviation ->
                IrTypeAbbreviationImpl(
                    irTypeAbbreviation.typeAlias.actualizeSymbol(),
                    irTypeAbbreviation.hasQuestionMark,
                    irTypeAbbreviation.arguments.map { (it.typeOrNull?.actualizeType() as? IrTypeArgument) ?: it },
                    irTypeAbbreviation.annotations.map { visitConstructorCall(it) }
                )
            }
        )
    }

    private inline fun <reified S : IrSymbol> S.actualizeSymbol(): S {
        return (expectActualMap[this] as? S) ?: this
    }
}