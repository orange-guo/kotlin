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
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.origin
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
    private val classId: ClassId,
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?
) : DeclarationBuildingContext<FirRegularClass>(session, key, owner) {
    private val superTypes = mutableListOf<ConeKotlinType>()

    fun addSuperType(type: ConeKotlinType) {
        superTypes += type
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
            if (superTypes.isEmpty()) {
                superTypeRefs += session.builtinTypes.anyType
            } else {
                superTypes.mapTo(this.superTypeRefs) { buildResolvedTypeRef { type = it } }
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
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    return ClassBuildingContext(classId, session, key, owner = null).apply(config).build()
}

context(FirDeclarationGenerationExtension)
fun createNestedClass(
    owner: FirClassSymbol<*>,
    name: Name,
    key: GeneratedDeclarationKey,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    return ClassBuildingContext(owner.classId.createNestedClassId(name), session, key, owner).apply(config).build()
}

context(FirDeclarationGenerationExtension)
fun createCompanionObject(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    config: ClassBuildingContext.() -> Unit = {}
): FirRegularClass {
    val classId = owner.classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    return ClassBuildingContext(classId, session, key, owner).apply(config).apply {
        classKind = ClassKind.OBJECT
        modality = Modality.FINAL
        status {
            isCompanion = true
        }
    }.build()
}
