/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

object FirJavaShadowedFieldReferenceChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirCallableReferenceAccess) return

        val reference = expression.calleeReference as? FirResolvedNamedReference ?: return
        val referredSymbol = reference.resolvedSymbol as? FirFieldSymbol ?: return
        if (referredSymbol.visibility != JavaVisibilities.ProtectedAndPackage) return
        val session = context.session
        val fieldContainingClassSymbol = referredSymbol.containingClassLookupTag()?.toFirRegularClassSymbol(session) ?: return
        if (context.containingFile?.packageFqName == fieldContainingClassSymbol.classId.packageFqName) {
            return
        }

        val dispatchReceiver = expression.dispatchReceiver.takeIf { it !is FirNoReceiverExpression } ?: return
        val scope = dispatchReceiver.typeRef.coneType.scope(
            session, context.sessionHolder.scopeSession, FakeOverrideTypeCalculator.DoNothing
        ) ?: return

        var shadowingPropertyClassId: ClassId? = null
        scope.processPropertiesByName(referredSymbol.name) {
            if (it !is FirPropertySymbol) return@processPropertiesByName
            if (!it.hasBackingField) return@processPropertiesByName
            val propertyContainingClassSymbol = it.containingClassLookupTag()?.toFirRegularClassSymbol(session)
                ?: return@processPropertiesByName
            if (lookupSuperTypes(
                    listOf(propertyContainingClassSymbol), lookupInterfaces = false, deep = true, session, substituteTypes = false
                ).any { superType ->
                    (superType as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag?.classId == fieldContainingClassSymbol.classId
                }
            ) {
                shadowingPropertyClassId = propertyContainingClassSymbol.classId
            }
        }

        shadowingPropertyClassId?.let {
            reporter.reportOn(
                reference.source,
                FirJvmErrors.JAVA_SHADOWED_PROTECTED_FIELD_REFERENCE,
                fieldContainingClassSymbol.classId,
                it,
                context
            )
        }
    }
}