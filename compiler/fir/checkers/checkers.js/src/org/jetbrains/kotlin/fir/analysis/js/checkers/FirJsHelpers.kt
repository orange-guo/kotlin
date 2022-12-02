/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.getAnnotationStringParameter
import org.jetbrains.kotlin.fir.declarations.isNativeObject
import org.jetbrains.kotlin.fir.declarations.isPredefinedObject
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.JsStandardClassIds

private val FirBasedSymbol<*>.isExternal
    get() = when (this) {
        is FirCallableSymbol<*> -> isExternal
        is FirClassSymbol<*> -> isExternal
        else -> false
    }

fun FirBasedSymbol<*>.isEffectivelyExternal(session: FirSession): Boolean {
    if (fir is FirMemberDeclaration && isExternal) return true

    if (this is FirPropertyAccessorSymbol) {
        val property = propertySymbol
        if (property.isEffectivelyExternal(session)) return true
    }

    if (this is FirPropertySymbol) {
        if (getterSymbol?.isExternal == true && (!isVar || setterSymbol?.isExternal == true)) {
            return true
        }
    }

    return getContainingClassSymbol(session)?.isEffectivelyExternal(session) == true
}

fun FirBasedSymbol<*>.isEffectivelyExternalMember(session: FirSession): Boolean {
    return fir is FirMemberDeclaration && isEffectivelyExternal(session)
}

fun FirBasedSymbol<*>.isEffectivelyExternal(context: CheckerContext) = isEffectivelyExternal(context.session)

fun FirFunctionSymbol<*>.isOverridingExternalWithOptionalParams(context: CheckerContext): Boolean {
    if (!isSubstitutionOrIntersectionOverride && modality == Modality.ABSTRACT) return false

    val overridden = (this as? FirNamedFunctionSymbol)?.directOverriddenFunctions(context) ?: return false

    for (overriddenFunction in overridden.filter { it.isEffectivelyExternal(context) }) {
        if (overriddenFunction.valueParameterSymbols.any { it.hasDefaultValue }) return true
    }

    return false
}

fun FirBasedSymbol<*>.getJsName(): String? {
    return getAnnotationStringParameter(JsStandardClassIds.Annotations.JsName)
}

fun FirBasedSymbol<*>.isNativeObject(context: CheckerContext) = isNativeObject(context.session)

fun FirBasedSymbol<*>.isPredefinedObject(context: CheckerContext) = isPredefinedObject(context.session)
