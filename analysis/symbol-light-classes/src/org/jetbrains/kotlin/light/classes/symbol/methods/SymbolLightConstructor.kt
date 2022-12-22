/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.isOriginEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.LazyModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import java.util.*

internal class SymbolLightConstructor(
    ktAnalysisSession: KtAnalysisSession,
    constructorSymbol: KtConstructorSymbol,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    argumentsSkipMask: BitSet? = null,
) : SymbolLightMethod<KtConstructorSymbol>(
    ktAnalysisSession = ktAnalysisSession,
    functionSymbol = constructorSymbol,
    lightMemberOrigin = null,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask,
) {
    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || isOriginEquivalentTo(another)
    }

    private val _modifierList: PsiModifierList by lazyPub {
        val initialValue = if (containingClass is SymbolLightClassForEnumEntry) {
            LazyModifiersBox.VISIBILITY_MODIFIERS_MAP.with(PsiModifier.PACKAGE_LOCAL)
        } else {
            emptyMap()
        }

        SymbolLightMemberModifierList(
            containingDeclaration = this,
            initialValue = initialValue,
            lazyModifiersComputer = ::computeModifiers,
        ) { modifierList ->
            withFunctionSymbol { constructorSymbol ->
                constructorSymbol.computeAnnotations(
                    modifierList = modifierList,
                    nullability = NullabilityType.Unknown,
                    annotationUseSiteTarget = null,
                )
            }
        }
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? {
        if (modifier !in LazyModifiersBox.VISIBILITY_MODIFIERS) return null
        return LazyModifiersBox.computeVisibilityForMember(ktModule, functionSymbolPointer)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getReturnType(): PsiType? = null
}
