/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.types.CompilerConeAttributes
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.types.Variance

sealed class TypeArgumentMapping {
    abstract operator fun get(typeParameterIndex: Int): FirTypeProjection

    object NoExplicitArguments : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection = buildPlaceholderProjection()
    }

    class Mapped(private val ordered: List<FirTypeProjection>) : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection {
            return ordered.getOrElse(typeParameterIndex) { buildPlaceholderProjection() }
        }
    }
}

internal object MapTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val typeArguments = callInfo.typeArguments
        val owner = candidate.symbol.fir as FirTypeParameterRefsOwner

        if (typeArguments.isEmpty()) {
            if (owner is FirCallableDeclaration &&
                owner.dispatchReceiverType?.attributes?.contains(CompilerConeAttributes.RawType) == true
            ) {
                val resultArguments = owner.typeParameters.map { typeParameterRef ->
                    buildTypeProjectionWithVariance {
                        typeRef =
                            ConeTypeIntersector.intersectTypes(
                                context.typeContext, typeParameterRef.symbol.resolvedBounds.map { it.type }
                            ).toFirResolvedTypeRef()
                        variance = Variance.INVARIANT
                    }
                }
                candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(resultArguments)
            } else {
                candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
            }
            return
        }

        if (
            typeArguments.size == owner.typeParameters.size ||
            callInfo.callKind == CallKind.DelegatingConstructorCall ||
            (owner as? FirDeclaration)?.origin is FirDeclarationOrigin.DynamicScope
        ) {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(typeArguments)
        } else {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(emptyList())
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }
}

internal object NoTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (callInfo.typeArguments.isNotEmpty()) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
        candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
    }
}
