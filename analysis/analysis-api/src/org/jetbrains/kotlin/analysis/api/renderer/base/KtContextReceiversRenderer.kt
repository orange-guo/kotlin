/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

public interface KtContextReceiversRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderContextReceivers(owner: KtContextReceiversOwner, printer: PrettyPrinter)

    public object AS_SOURCE : KtContextReceiversRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderContextReceivers(owner: KtContextReceiversOwner, printer: PrettyPrinter) {
            val contextReceivers = owner.contextReceivers
            if (contextReceivers.isEmpty()) return

            printer {
                append("context(")
                printer.printCollection(contextReceivers) { contextReceiver ->
                    contextReceiver.label?.let { label ->
                        append(label.render())
                        append('@')
                    }
                    renderType(contextReceiver.type, printer)
                }
                append(")")
            }
        }
    }
}