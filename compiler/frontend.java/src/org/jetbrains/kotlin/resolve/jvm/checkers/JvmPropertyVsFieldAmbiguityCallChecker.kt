/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object JvmPropertyVsFieldAmbiguityCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.PreferJavaFieldOverload)) return
        val resultingDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return
        if (!resultingDescriptor.isJavaField) return
        val ownContainingClass = resultingDescriptor.containingDeclaration as? ClassDescriptor ?: return

        val field = DescriptorUtils.unwrapFakeOverride(resultingDescriptor)
        val fieldClassDescriptor = field.containingDeclaration as? ClassDescriptor
        if (fieldClassDescriptor === ownContainingClass) return

        // If we have visible alternative property, we leave everything as is (see KT-54393)
        ownContainingClass.unsubstitutedMemberScope.getContributedVariables(
            resultingDescriptor.name, NoLookupLocation.FOR_ALREADY_TRACKED
        ).forEach { alternativePropertyDescriptor ->
            if (alternativePropertyDescriptor !== resultingDescriptor) {
                val hasLateInit = alternativePropertyDescriptor.isLateInit
                if (!hasLateInit &&
                    alternativePropertyDescriptor.getter?.isDefault != false &&
                    alternativePropertyDescriptor.setter?.isDefault != false &&
                    alternativePropertyDescriptor.modality == Modality.FINAL
                ) return@forEach
                val propertyClassDescriptor =
                    DescriptorUtils.unwrapFakeOverride(alternativePropertyDescriptor).containingDeclaration as? ClassDescriptor
                if (fieldClassDescriptor != null && propertyClassDescriptor != null &&
                    DescriptorUtils.isSubclass(fieldClassDescriptor, propertyClassDescriptor)
                ) return@forEach
                if (DescriptorVisibilities.isVisible(
                        /* receiver = */ resolvedCall.dispatchReceiver,
                        /* what = */ alternativePropertyDescriptor,
                        /* from = */ context.scope.ownerDescriptor,
                        /* useSpecialRulesForPrivateSealedConstructors = */ false
                    )
                ) {
                    val factory = when {
                        alternativePropertyDescriptor.getter?.isDefault == false ->
                            ErrorsJvm.BASE_CLASS_FIELD_SHADOWS_DERIVED_CLASS_PROPERTY
                        hasLateInit || alternativePropertyDescriptor.setter?.isDefault == false ->
                            ErrorsJvm.BACKING_FIELD_ACCESSED_DUE_TO_PROPERTY_FIELD_CONFLICT
                        else ->
                            ErrorsJvm.BASE_CLASS_FIELD_MAY_SHADOW_DERIVED_CLASS_PROPERTY
                    }
                    context.trace.report(
                        factory.on(
                            reportOn,
                            fieldClassDescriptor?.fqNameSafe?.asString() ?: "unknown class",
                            propertyClassDescriptor?.fqNameSafe?.asString() ?: "unknown class"
                        )
                    )
                    return
                }
            }
        }
    }
}