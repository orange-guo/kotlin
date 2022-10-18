/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.nullableModuleData
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider

@ThreadSafeMutableState
open class FirDependenciesSymbolProviderImpl(session: FirSession) : FirCachingSymbolProvider(
    session,
    calculateDependencyProviders(session)
) {
    companion object {
        protected fun calculateDependencyProviders(session: FirSession): List<FirSymbolProvider> {
            val moduleData =
                session.nullableModuleData ?: error("FirDependenciesSymbolProvider should not be created if there are no dependencies")
            val visited = mutableSetOf<FirSymbolProvider>()
            return (moduleData.dependencies + moduleData.friendDependencies + moduleData.dependsOnDependencies)
                .mapNotNull { session.sessionProvider?.getSession(it) }
                .map { it.symbolProvider }
                .flatMap { it.flatten(visited, collectSourceProviders = it.session.kind == FirSession.Kind.Source) }
                .sortedBy { it.session.kind }
        }

        /* It eliminates dependency and composite providers since the current dependency provider is composite in fact.
        *  To prevent duplications and resolving errors, library or source providers from other modules should be filtered out during flattening.
        *  It depends on the session's kind of the top-level provider */
        private fun FirSymbolProvider.flatten(
            visited: MutableSet<FirSymbolProvider>,
            collectSourceProviders: Boolean
        ): List<FirSymbolProvider> {
            val result = mutableListOf<FirSymbolProvider>()

            fun FirSymbolProvider.collectProviders() {
                if (!visited.add(this)) return
                when {
                    this is FirCachingSymbolProvider -> {
                        for (provider in providers) {
                            provider.collectProviders()
                        }
                    }
                    this is FirCompositeSymbolProvider -> {
                        for (provider in providers) {
                            provider.collectProviders()
                        }
                    }
                    collectSourceProviders && session.kind == FirSession.Kind.Source ||
                            !collectSourceProviders && session.kind == FirSession.Kind.Library -> {
                        result.add(this)
                    }
                }
            }

            collectProviders()

            return result
        }
    }
}
