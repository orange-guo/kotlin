/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

object FirJsDebugInfoExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeDeclaration = expression.calleeReference.resolvedSymbol as? FirCallableSymbol<*>
        if (calleeDeclaration?.origin is FirDeclarationOrigin.DynamicScope) {
            reporter.reportOn(expression.calleeReference.source, FirJsErrors.DYNAMIC, context)
        }
    }
}