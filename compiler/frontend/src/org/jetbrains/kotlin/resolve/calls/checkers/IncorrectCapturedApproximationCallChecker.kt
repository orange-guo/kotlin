/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.util.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForSamType
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

object IncorrectCapturedApproximationCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!resolvedCall.isReallySuccess()) return
        val dispatchReceiverType = resolvedCall.smartCastDispatchReceiverType ?: resolvedCall.dispatchReceiver?.type ?: return
        if (dispatchReceiverType.arguments.all { it.projectionKind == Variance.INVARIANT && !it.type.isCaptured() }) return

        val substitutor =
            TypeSubstitutor.create(dispatchReceiverType)
                .substitution.wrapWithCapturingSubstitution(needApproximation = false).buildSubstitutor()

        val functionDescriptor = resolvedCall.resultingDescriptor.original as? FunctionDescriptor ?: return

        val capturedSubstituted = functionDescriptor.substitute(substitutor) ?: return

        val indexedArguments = resolvedCall.valueArgumentsByIndex ?: return
        for ((index, parameter) in capturedSubstituted.valueParameters.withIndex()) {
            for (argument in indexedArguments[index].arguments) {
                val expectedType =
                    getEffectiveExpectedType(parameter, argument, context.resolutionContext)

                val argumentExpression = argument.getArgumentExpression()
                val expressionType = argumentExpression?.getType(context.trace.bindingContext) ?: continue
                if (!TypeUtils.contains(expectedType) { it.isCaptured() }) continue

                val samExpectedType = getFunctionTypeForSamType(
                    expectedType, context.callComponents.samConversionResolver, context.callComponents.samConversionOracle,
                )

                if (!expressionType.isSubtypeOf(expectedType) && (samExpectedType == null || !expressionType.isSubtypeOf(samExpectedType))) {
                    context.trace.report(
                        Errors.TYPE_MISMATCH_WARNING_FOR_INCORRECT_CAPTURE_APPROXIMATION.on(
                            argumentExpression, expectedType, expressionType
                        )
                    )
                }
            }
        }

        capturedSubstituted.extensionReceiverParameter?.let { extensionReceiverParameter ->
            if (!TypeUtils.contains(extensionReceiverParameter.type) { it.isCaptured() }) return@let
            val receiverValueType = resolvedCall.extensionReceiver?.type ?: return@let

            if (!receiverValueType.isSubtypeOf(extensionReceiverParameter.type)) {
                val expression = (resolvedCall.extensionReceiver as? ExpressionReceiver)?.expression ?: reportOn
                context.trace.report(
                    Errors.RECEIVER_TYPE_MISMATCH_WARNING_FOR_INCORRECT_CAPTURE_APPROXIMATION.on(
                        expression, extensionReceiverParameter.type, receiverValueType
                    )
                )
            }
        }
    }
}
