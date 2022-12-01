/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidator
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver

@ThreadSafeMutableState
internal class LLFirLazyDeclarationResolver(private val sessionInvalidator: LLFirSessionInvalidator) : FirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: FirResolvePhase) {}
    override fun finishResolvingPhase(phase: FirResolvePhase) {}

    override fun disableLazyResolveContractChecks() {}
    override fun enableLazyResolveContractsChecks() {}

    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
        val fir = symbol.fir
        val session = fir.moduleData.session
        if (session !is LLFirResolvableModuleSession) return
        val moduleComponents = session.moduleComponents
        try {
            moduleComponents.firModuleLazyDeclarationResolver.lazyResolveDeclaration(
                firDeclarationToResolve = fir,
                scopeSession = moduleComponents.scopeSessionProvider.getScopeSession(),
                toPhase = toPhase,
                checkPCE = true,
            )
            if (fir is FirMemberDeclaration && fir.resolvePhase < toPhase &&
                (fir.visibility == Visibilities.Local || fir.getContainingClassSymbol(session)?.isLocal == true)
            ) {
                fir.replaceResolvePhase(toPhase)
            }
        } catch (e: Throwable) {
            sessionInvalidator.invalidate(session)
            throw e
        }
    }

    override fun assertResolvedToPhase(symbol: FirBasedSymbol<*>, phase: FirResolvePhase) {
        val fir = symbol.fir
        val session = fir.moduleData.session
        if (session !is LLFirResolvableModuleSession) return
        val fromPhase = fir.resolvePhase
        if (fromPhase < phase) {
            throw AssertionError(
                "LLFirLazyDeclarationResolver for\n${fir.render()}\n: expected phase $phase but actually phase is $fromPhase"
            )
        }
    }
}
