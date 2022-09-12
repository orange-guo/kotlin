// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
// To regenerate run 'generateGradleOptions' task
@file:Suppress("RemoveRedundantQualifierName", "Deprecation", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dsl

internal abstract class CompilerJvmOptionsDefault @javax.inject.Inject constructor(
    objectFactory: org.gradle.api.model.ObjectFactory
) : org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptionsDefault(objectFactory), org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions {

    override val javaParameters: org.gradle.api.provider.Property<kotlin.Boolean> =
        objectFactory.property(kotlin.Boolean::class.java).convention(false)

    override val jvmTarget: org.gradle.api.provider.Property<org.jetbrains.kotlin.gradle.dsl.JvmTarget> =
        objectFactory.property(org.jetbrains.kotlin.gradle.dsl.JvmTarget::class.java)

    override val moduleName: org.gradle.api.provider.Property<kotlin.String> =
        objectFactory.property(kotlin.String::class.java)

    override val noJdk: org.gradle.api.provider.Property<kotlin.Boolean> =
        objectFactory.property(kotlin.Boolean::class.java).convention(false)

    internal fun toCompilerArguments(args: org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments) {
        super.toCompilerArguments(args)
        args.javaParameters = javaParameters.get()
        args.jvmTarget = jvmTarget.orNull?.target
        args.moduleName = moduleName.orNull
        args.noJdk = noJdk.get()
    }

    internal fun fillDefaultValues(args: org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments) {
        super.fillDefaultValues(args)
        args.javaParameters = false
        args.jvmTarget = null
        args.moduleName = null
        args.noJdk = false
    }
}
