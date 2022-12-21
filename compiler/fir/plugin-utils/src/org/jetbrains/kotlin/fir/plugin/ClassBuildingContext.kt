/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class ClassBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    private val classId: ClassId,
    private val classKind: ClassKind,
) : DeclarationBuildingContext<FirRegularClass>(session, key, owner) {
    private val superTypeProviders = mutableListOf<(List<FirTypeParameterRef>) -> ConeKotlinType>()

    fun addSuperType(type: ConeKotlinType) {
        superTypeProviders += { type }
    }

    fun addSuperType(typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType) {
        superTypeProviders += typeProvider
    }

    override fun build(): FirRegularClass {
        return buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin
            classKind = this@ClassBuildingContext.classKind
            scopeProvider = session.kotlinScopeProvider
            status = generateStatus()
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)
            this@ClassBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }
            if (superTypeProviders.isEmpty()) {
                superTypeRefs += session.builtinTypes.anyType
            } else {
                superTypeProviders.mapTo(this.superTypeRefs) {
                    buildResolvedTypeRef { type = it(this@buildRegularClass.typeParameters) }
                }
            }
        }.apply {
            if (owner?.isLocal == true) {
                containingClassForLocalAttr = owner.toLookupTag()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

context(FirDeclarationGenerationExtension)
fun createTopLevelClass(
    classId: ClassId,
    key: GeneratedDeclarationKey,
    classKind: ClassKind = ClassKind.CLASS,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    return ClassBuildingContext(session, key, owner = null, classId, classKind).apply(config).build()
}

context(FirDeclarationGenerationExtension)
fun createNestedClass(
    owner: FirClassSymbol<*>,
    name: Name,
    key: GeneratedDeclarationKey,
    classKind: ClassKind = ClassKind.CLASS,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    return ClassBuildingContext(session, key, owner, owner.classId.createNestedClassId(name), classKind).apply(config).build()
}

context(FirDeclarationGenerationExtension)
fun createCompanionObject(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    val classId = owner.classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    return ClassBuildingContext(session, key, owner, classId, ClassKind.OBJECT).apply(config).apply {
        modality = Modality.FINAL
        status {
            isCompanion = true
        }
    }.build()
}
