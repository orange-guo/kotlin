/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.name.Name

private val membersFqNames = setOf(
    StandardNames.FqNames.charSequence.child(Name.identifier("get")),
    StandardNames.FqNames.charIterator.toUnsafe().child(Name.identifier("nextChar")),
)

private val memberNameByParent = membersFqNames.associate { it.parent() to it.shortName() }

object FirJsMultipleInheritanceChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        @Suppress("UNUSED_VARIABLE") val supertypes = lookupSuperTypes(declaration, true, true, context.session, false)

        for (it in supertypes) {
            val fqName = it.classId?.asSingleFqName()?.toUnsafe()
            val name = memberNameByParent[fqName] ?: continue
            val symbol = it.toSymbol(context.session) as? FirClassSymbol ?: error("Expected a class symbol")
            val scope = context.session.declaredMemberScope(symbol)
            val callable = scope.getFunctions(name).firstOrNull() ?: error("Expected some callable")
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_MULTIPLE_INHERITANCE, callable, context)
        }
    }
}