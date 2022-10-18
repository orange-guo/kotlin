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
            val result = (moduleData.dependencies + moduleData.friendDependencies + moduleData.dependsOnDependencies)
                .mapNotNull { session.sessionProvider?.getSession(it) }
                .sortedBy { it.kind }
                .map { it.symbolProvider }
            return result.flatMap {
                when (it) {
                    is FirCompositeSymbolProvider -> it.providers
                    is FirCachingSymbolProvider -> it.providers
                    else -> listOf(it)
                }
            }
        }
    }
}
