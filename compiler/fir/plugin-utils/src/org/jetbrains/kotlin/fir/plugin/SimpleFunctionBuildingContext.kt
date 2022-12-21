/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildContextReceiver
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SimpleFunctionBuildingContext(
    session: FirSession,
    key: GeneratedDeclarationKey,
    owner: FirClassSymbol<*>?,
    callableId: CallableId,
    private val returnType: ConeKotlinType,
    private val extensionReceiverType: ConeKotlinType?,
    private val contextReceiverTypes: List<ConeKotlinType>,
) : FunctionBuildingContext<FirSimpleFunction>(callableId, session, key, owner) {
    override fun build(): FirSimpleFunction {
        return buildSimpleFunction {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin

            symbol = FirNamedFunctionSymbol(callableId)
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

            this@SimpleFunctionBuildingContext.valueParameters.mapTo(valueParameters) {
                generateValueParameter(it, symbol)
            }

            this@SimpleFunctionBuildingContext.typeParameters.mapTo(typeParameters) {
                generateTypeParameter(it, symbol)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

context(FirDeclarationGenerationExtension)
fun createMemberFunction(
    owner: FirClassSymbol<*>,
    key: GeneratedDeclarationKey,
    name: Name,
    returnType: ConeKotlinType,
    extensionReceiverType: ConeKotlinType? = null,
    contextReceiverTypes: List<ConeKotlinType> = emptyList(),
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    val callableId = CallableId(owner.classId, name)
    return SimpleFunctionBuildingContext(
        session, key, owner, callableId, returnType,
        extensionReceiverType, contextReceiverTypes
    ).apply(config).build()
}

context(FirDeclarationGenerationExtension)
fun createTopLevelFunction(
    key: GeneratedDeclarationKey,
    packageName: FqName,
    name: Name,
    returnType: ConeKotlinType,
    extensionReceiverType: ConeKotlinType? = null,
    contextReceiverTypes: List<ConeKotlinType> = emptyList(),
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    return createTopLevelFunction(key, CallableId(packageName, name), returnType, extensionReceiverType, contextReceiverTypes, config)
}

context(FirDeclarationGenerationExtension)
fun createTopLevelFunction(
    key: GeneratedDeclarationKey,
    callableId: CallableId,
    returnType: ConeKotlinType,
    extensionReceiverType: ConeKotlinType? = null,
    contextReceiverTypes: List<ConeKotlinType> = emptyList(),
    config: SimpleFunctionBuildingContext.() -> Unit = {}
): FirSimpleFunction {
    require(callableId.classId == null)
    return SimpleFunctionBuildingContext(
        session, key, owner = null, callableId, returnType,
        extensionReceiverType, contextReceiverTypes
    ).apply(config).build()
}
