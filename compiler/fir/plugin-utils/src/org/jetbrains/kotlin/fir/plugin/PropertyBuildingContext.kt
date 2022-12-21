/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyBodyResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildContextReceiver
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class PropertyBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    private val callableId: CallableId,
    private val returnType: ConeKotlinType,
    private val extensionReceiverType: ConeKotlinType?,
    private val contextReceiverTypes: List<ConeKotlinType>,
    private val isVal: Boolean,
    private val hasBackingField: Boolean,
) : DeclarationBuildingContext<FirProperty>(session, key, owner) {
    private var setterVisibility: Visibility? = null

    fun setter(visibility: Visibility) {
        setterVisibility = visibility
    }

    override fun build(): FirProperty {
        return buildProperty {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            symbol = FirPropertySymbol(callableId)
            name = callableId.callableName

            status = generateStatus()
            returnTypeRef = returnType.toFirResolvedTypeRef()
            extensionReceiverType?.let {
                receiverParameter = buildReceiverParameter {
                    typeRef = it.toFirResolvedTypeRef()
                }
            }

            dispatchReceiverType = owner?.defaultType()
            contextReceiverTypes.mapTo(contextReceivers) {
                buildContextReceiver { typeRef = it.toFirResolvedTypeRef() }
            }

            this@PropertyBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }

            isVar = !isVal
            getter = FirDefaultPropertyGetter(source = null, session.moduleData, key.origin, returnTypeRef, status.visibility, symbol)
            if (isVar) {
                setter = FirDefaultPropertySetter(
                    source = null, session.moduleData, key.origin, returnTypeRef,
                    setterVisibility ?: status.visibility, symbol
                )
            }
            if (hasBackingField) {
                backingField = FirDefaultPropertyBackingField(session.moduleData, mutableListOf(), returnTypeRef, isVar, symbol, status)
            }
            isLocal = false
            bodyResolveState = FirPropertyBodyResolveState.EVERYTHING_RESOLVED
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

context(FirDeclarationGenerationExtension)
fun createMemberProperty(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    extensionReceiverType: ConeKotlinType? = null,
    contextReceiverTypes: List<ConeKotlinType> = emptyList(),
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    val callableId = CallableId(owner.classId, name)
    return PropertyBuildingContext(
        session, key, owner, callableId, returnType,
        extensionReceiverType, contextReceiverTypes,
        isVal, hasBackingField
    ).apply(config).build()
}

context(FirDeclarationGenerationExtension)
fun createTopLevelProperty(
    key: GeneratedDeclarationKey,
    packageName: FqName,
    name: Name,
    returnType: ConeKotlinType,
    extensionReceiverType: ConeKotlinType? = null,
    contextReceiverTypes: List<ConeKotlinType> = emptyList(),
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    return createTopLevelProperty(
        key, CallableId(packageName, name), returnType, extensionReceiverType,
        contextReceiverTypes, isVal, hasBackingField, config
    )
}

context(FirDeclarationGenerationExtension)
fun createTopLevelProperty(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnType: ConeKotlinType,
    extensionReceiverType: ConeKotlinType? = null,
    contextReceiverTypes: List<ConeKotlinType> = emptyList(),
    isVal: Boolean = true,
    hasBackingField: Boolean = true,
    config: PropertyBuildingContext.() -> Unit = {}
): FirProperty {
    require(callableId.classId == null)
    return PropertyBuildingContext(
        session, key, owner = null, callableId, returnType,
        extensionReceiverType, contextReceiverTypes,
        isVal, hasBackingField
    ).apply(config).build()
}
