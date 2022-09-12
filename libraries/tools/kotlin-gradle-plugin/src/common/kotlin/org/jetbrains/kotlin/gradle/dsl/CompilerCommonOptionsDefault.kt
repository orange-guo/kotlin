// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
// To regenerate run 'generateGradleOptions' task
@file:Suppress("RemoveRedundantQualifierName", "Deprecation", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dsl

internal abstract class CompilerCommonOptionsDefault @javax.inject.Inject constructor(
    objectFactory: org.gradle.api.model.ObjectFactory
) : org.jetbrains.kotlin.gradle.dsl.CompilerCommonToolOptionsDefault(objectFactory), org.jetbrains.kotlin.gradle.dsl.CompilerCommonOptions {

    override val apiVersion: org.gradle.api.provider.Property<org.jetbrains.kotlin.gradle.dsl.KotlinVersion> =
        objectFactory.property(org.jetbrains.kotlin.gradle.dsl.KotlinVersion::class.java)

    override val languageVersion: org.gradle.api.provider.Property<org.jetbrains.kotlin.gradle.dsl.KotlinVersion> =
        objectFactory.property(org.jetbrains.kotlin.gradle.dsl.KotlinVersion::class.java)

    override val useK2: org.gradle.api.provider.Property<kotlin.Boolean> =
        objectFactory.property(kotlin.Boolean::class.java).convention(false)

    internal fun toCompilerArguments(args: org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments) {
        super.toCompilerArguments(args)
        args.apiVersion = apiVersion.orNull?.version
        args.languageVersion = languageVersion.orNull?.version
        args.useK2 = useK2.get()
    }

    internal fun fillDefaultValues(args: org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments) {
        super.fillDefaultValues(args)
        args.apiVersion = null
        args.languageVersion = null
        args.useK2 = false
    }
}
