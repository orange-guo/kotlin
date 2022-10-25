/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.DeclarationKind.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExpressionKind.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.Partially
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageCase.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.UNKNOWN_NAME
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.guessName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentAsClass

// TODO: Simplify and enhance PL error messages when new self-descriptive signatures are implemented.
internal fun PartialLinkageCase.renderErrorMessage(): String = buildString {
    when (this@renderErrorMessage) {
        is MissingDeclaration -> noDeclarationForSymbol(missingDeclarationSymbol)
        is MissingEnclosingClass -> noEnclosingClass(orphanedClassSymbol)
        is DeclarationUsesPartiallyLinkedClassifier -> declaration(declarationSymbol, capitalized = true).append(" uses ").cause(cause)
        is UnimplementedAbstractCallable -> unimplementedAbstractCallable(callable)
        is ExpressionUsesMissingDeclaration -> expression(expression).noDeclarationForSymbol(missingDeclarationSymbol)
        is ExpressionUsesPartiallyLinkedClassifier -> expression(expression).append("IR expression uses ").cause(cause)
        is ExpressionUsesDeclarationThatUsesPartiallyLinkedClassifier ->
            expression(expression).declaration(referencedDeclarationSymbol, capitalized = true).append(" uses ").cause(cause)
    }
}

private enum class DeclarationKind(val displayName: String) {
    CLASS("class"),
    INNER_CLASS("inner class"),
    INTERFACE("interface"),
    ENUM_CLASS("enum class"),
    ENUM_ENTRY("enum entry"),
    ENUM_ENTRY_CLASS("enum entry class"),
    ANNOTATION_CLASS("annotation class"),
    OBJECT("object"),
    ANONYMOUS_OBJECT("anonymous object"),
    COMPANION_OBJECT("companion object"),
    VARIABLE("variable"),
    VALUE_PARAMETER("value parameter"),
    FIELD("field"),
    FIELD_OF_PROPERTY("backing field of property"),
    PROPERTY("property"),
    PROPERTY_ACCESSOR("property accessor"),
    FUNCTION("function"),
    CONSTRUCTOR("constructor"),
    OTHER_DECLARATION("declaration");
}

private val IrSymbol.declarationKind: DeclarationKind
    get() = when (this) {
        is IrClassSymbol -> when (owner.kind) {
            ClassKind.CLASS -> when {
                owner.isAnonymousObject -> ANONYMOUS_OBJECT
                owner.isInner -> INNER_CLASS
                else -> CLASS
            }
            ClassKind.INTERFACE -> INTERFACE
            ClassKind.ENUM_CLASS -> ENUM_CLASS
            ClassKind.ENUM_ENTRY -> ENUM_ENTRY_CLASS
            ClassKind.ANNOTATION_CLASS -> ANNOTATION_CLASS
            ClassKind.OBJECT -> if (owner.isCompanion) COMPANION_OBJECT else OBJECT
        }
        is IrEnumEntrySymbol -> ENUM_ENTRY
        is IrVariableSymbol -> VARIABLE
        is IrValueParameterSymbol -> VALUE_PARAMETER
        is IrFieldSymbol -> if (owner.correspondingPropertySymbol != null) FIELD_OF_PROPERTY else FIELD
        is IrPropertySymbol -> PROPERTY
        is IrSimpleFunctionSymbol -> if (owner.correspondingPropertySymbol != null) PROPERTY_ACCESSOR else FUNCTION
        is IrConstructorSymbol -> CONSTRUCTOR
        else -> OTHER_DECLARATION
    }

private data class Expression(val kind: ExpressionKind, val referencedDeclarationKind: DeclarationKind?)

private enum class ExpressionKind(val prefix: String, val postfix: String?) {
    REFERENCE("Reference to", "can not be evaluated"),
    CALLING("Invocation of", "can not be performed"),
    CALLING_INSTANCE_INITIALIZER("Invocation of instance initializer for", "can not be performed"),
    READING("Can not read value from", null),
    WRITING("Can not write value to", null),
    GETTING_INSTANCE("Can not get instance of", null),
    OTHER_EXPRESSION("Expression", "can not be evaluated")
}

// More can be added for verbosity in the future.
private val IrExpression.expression: Expression
    get() = when (this) {
        is IrDeclarationReference -> when (this) {
            is IrFunctionReference -> Expression(REFERENCE, symbol.declarationKind)
            is IrPropertyReference,
            is IrLocalDelegatedPropertyReference -> Expression(REFERENCE, PROPERTY)
            is IrCall -> Expression(CALLING, symbol.declarationKind)
            is IrConstructorCall,
            is IrEnumConstructorCall,
            is IrDelegatingConstructorCall -> Expression(CALLING, CONSTRUCTOR)
            is IrClassReference -> Expression(REFERENCE, symbol.declarationKind)
            is IrGetField -> Expression(READING, symbol.declarationKind)
            is IrSetField -> Expression(WRITING, symbol.declarationKind)
            is IrGetValue -> Expression(READING, symbol.declarationKind)
            is IrSetValue -> Expression(WRITING, symbol.declarationKind)
            is IrGetSingletonValue -> Expression(GETTING_INSTANCE, symbol.declarationKind)
            else -> Expression(REFERENCE, OTHER_DECLARATION)
        }
        is IrInstanceInitializerCall -> Expression(CALLING_INSTANCE_INITIALIZER, classSymbol.declarationKind)
        else -> Expression(OTHER_EXPRESSION, null)
    }

private fun IrSymbol.guessName(): String? {
    return anySignature
        ?.let { effectiveSignature ->
            val nameSegmentsToPickUp = when {
                effectiveSignature is IdSignature.AccessorSignature -> 2 // property_name.accessor_name
                this is IrConstructorSymbol -> 2 // class_name.<init>
                else -> 1
            }
            effectiveSignature.guessName(nameSegmentsToPickUp)
        }
        ?: (owner as? IrDeclaration)?.nameForIrSerialization?.asString()
}

private val IrSymbol.anySignature: IdSignature?
    get() = signature ?: privateSignature

private const val UNKNOWN_SYMBOL = "<unknown symbol>"

private fun Appendable.signature(symbol: IrSymbol): Appendable =
    append(symbol.anySignature?.render() ?: UNKNOWN_SYMBOL)

private fun Appendable.declaration(symbol: IrSymbol, capitalized: Boolean): Appendable {
    val declarationKind = symbol.declarationKind
    appendCapitalized(declarationKind.displayName, capitalized)

    if (declarationKind != ANONYMOUS_OBJECT) {
        // This is a declaration NOT under a property.
        append(" ").append(symbol.guessName() ?: UNKNOWN_NAME.asString())
    }

    return this
}

private fun StringBuilder.expression(expression: IrExpression): Appendable {
    val (expressionKind, referencedDeclarationKind) = expression.expression
    append(expressionKind.prefix) // Prefix is already capitalized.

    if (referencedDeclarationKind != null) {
        append(" ")

        when (expression) {
            is IrDeclarationReference -> declaration(expression.symbol, capitalized = false)
            is IrInstanceInitializerCall -> declaration(expression.classSymbol, capitalized = false)
            else -> append(referencedDeclarationKind.displayName)
        }
    }

    expressionKind.postfix?.let { postfix -> append(" ").append(postfix) }

    return append(". ")
}

private fun Appendable.cause(cause: Partially): Appendable =
    when (cause) {
        is Partially.MissingClassifier -> unlinkedSymbol(cause)
        is Partially.MissingEnclosingClass -> noEnclosingClass(cause)
        is Partially.DueToOtherClassifier -> {
            when (val rootCause = cause.rootCause) {
                is Partially.MissingClassifier -> unlinkedSymbol(rootCause, cause)
                is Partially.MissingEnclosingClass -> noEnclosingClass(rootCause, cause)
            }
        }
    }

private fun Appendable.noDeclarationForSymbol(symbol: IrSymbol): Appendable =
    append("No ").append(symbol.declarationKind.displayName).append(" found for symbol ").signature(symbol)

private fun Appendable.noEnclosingClass(symbol: IrClassSymbol): Appendable =
    declaration(symbol, capitalized = true).append(" lacks enclosing class")

private fun Appendable.unlinkedSymbol(
    rootCause: Partially.MissingClassifier,
    cause: Partially.DueToOtherClassifier? = null
): Appendable {
    append("unlinked ").append(rootCause.symbol.declarationKind.displayName).append(" symbol ").signature(rootCause.symbol)
    if (cause != null) through(cause)
    return this
}

private fun Appendable.noEnclosingClass(
    rootCause: Partially.MissingEnclosingClass,
    cause: Partially.DueToOtherClassifier? = null
): Appendable {
    declaration(rootCause.symbol, capitalized = false)
    if (cause != null) through(cause)
    return append(". ").noEnclosingClass(rootCause.symbol)
}

private fun Appendable.through(cause: Partially.DueToOtherClassifier): Appendable =
    append(" through ").declaration(cause.symbol, capitalized = false)

private fun Appendable.unimplementedAbstractCallable(callable: IrOverridableDeclaration<*>): Appendable =
    append("Abstract ").declaration(callable.symbol, capitalized = false)
        .append(" is not implemented in non-abstract ").declaration(callable.parentAsClass.symbol, capitalized = false)

private fun Appendable.appendCapitalized(text: String, capitalized: Boolean): Appendable {
    if (capitalized && text.isNotEmpty()) {
        val firstChar = text[0]
        if (firstChar.isLowerCase())
            return append(firstChar.uppercaseChar()).append(text.substring(1))
    }

    return append(text)
}
