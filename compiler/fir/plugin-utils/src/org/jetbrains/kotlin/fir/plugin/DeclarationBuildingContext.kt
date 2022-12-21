/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

sealed class DeclarationBuildingContext<T : FirDeclaration>(
    protected val session: FirSession,
    protected val key: GeneratedDeclarationKey,
    protected val owner: FirClassSymbol<*>?
) {
    var visibility: Visibility = Visibilities.Public
    var modality: Modality = Modality.FINAL
    var classKind: ClassKind = ClassKind.CLASS

    fun status(statusConfig: FirResolvedDeclarationStatusImpl.() -> Unit) {
        statusConfigs += statusConfig
    }

    fun typeParameter(
        name: Name,
        variance: Variance = Variance.INVARIANT,
        isReified: Boolean = false,
        bounds: List<ConeKotlinType> = emptyList(),
        key: GeneratedDeclarationKey = this@DeclarationBuildingContext.key
    ) {
        typeParameters += TypeParameterData(name, variance, isReified, bounds, key)
    }

    protected data class TypeParameterData(
        val name: Name,
        val variance: Variance,
        val isReified: Boolean,
        val bounds: List<ConeKotlinType>,
        val key: GeneratedDeclarationKey
    )

    protected val typeParameters = mutableListOf<TypeParameterData>()

    private val statusConfigs: MutableList<FirResolvedDeclarationStatusImpl.() -> Unit> = mutableListOf()

    abstract fun build(): T

    protected fun generateStatus(): FirResolvedDeclarationStatusImpl {
        return FirResolvedDeclarationStatusImpl(
            visibility,
            modality,
            visibility.toEffectiveVisibility(owner, forClass = true)
        ).also {
            for (statusConfig in statusConfigs) {
                it.apply(statusConfig)
            }
        }
    }

    protected fun generateTypeParameter(
        typeParameter: TypeParameterData,
        containingDeclarationSymbol: FirBasedSymbol<*>
    ): FirTypeParameter {
        return buildTypeParameter {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = typeParameter.key.origin
            name = typeParameter.name
            symbol = FirTypeParameterSymbol()
            this.containingDeclarationSymbol = containingDeclarationSymbol
            variance = typeParameter.variance
            isReified = typeParameter.isReified
            if (typeParameter.bounds.isEmpty()) {
                bounds += session.builtinTypes.nullableAnyType
            } else {
                typeParameter.bounds.mapTo(bounds) { it.toFirResolvedTypeRef() }
            }
        }
    }
}
