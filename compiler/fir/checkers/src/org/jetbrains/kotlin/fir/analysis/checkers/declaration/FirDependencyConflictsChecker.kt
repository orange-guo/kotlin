/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider

object FirDependencyConflictsChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.visibility != Visibilities.Public) return
        if (declaration.classId.parentClassId != null) return
        if (declaration.classId.packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME) return
        val dependencySymbol = context.session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(declaration.classId) ?: return
        if (dependencySymbol.isExpect) return
        reporter.reportOn(
            declaration.source, FirErrors.PACKAGE_OR_CLASSIFIER_REDECLARATION, listOf(declaration.symbol, dependencySymbol), context
        )
    }
}