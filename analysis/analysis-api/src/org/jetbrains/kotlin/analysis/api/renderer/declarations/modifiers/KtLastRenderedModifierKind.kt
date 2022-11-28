/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers

/**
 * The kind of the last modifier that has been rendered by [KtDeclarationModifiersRenderer], if any, used to determine the separator
 * between the modifier list and the declaration.
 */
public enum class KtLastRenderedModifierKind { NONE, SIMPLE, CONTEXT_RECEIVERS }
