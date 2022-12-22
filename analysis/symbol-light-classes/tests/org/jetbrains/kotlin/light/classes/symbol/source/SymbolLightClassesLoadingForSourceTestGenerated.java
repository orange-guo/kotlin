/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/asJava/lightClasses/lightClassByPsi")
@TestDataPath("$PROJECT_ROOT")
public class SymbolLightClassesLoadingForSourceTestGenerated extends AbstractSymbolLightClassesLoadingForSourceTest {
    @Test
    public void testAllFilesPresentInLightClassByPsi() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByPsi"), Pattern.compile("^(.+)\\.(kt|kts)$"), null, true);
    }

    @Test
    @TestMetadata("annotationWithSetParamPropertyModifier.kt")
    public void testAnnotationWithSetParamPropertyModifier() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/annotationWithSetParamPropertyModifier.kt");
    }

    @Test
    @TestMetadata("annotations.kt")
    public void testAnnotations() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/annotations.kt");
    }

    @Test
    @TestMetadata("classModifiers.kt")
    public void testClassModifiers() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/classModifiers.kt");
    }

    @Test
    @TestMetadata("constructors.kt")
    public void testConstructors() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/constructors.kt");
    }

    @Test
    @TestMetadata("coroutines.kt")
    public void testCoroutines() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/coroutines.kt");
    }

    @Test
    @TestMetadata("dataClasses.kt")
    public void testDataClasses() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/dataClasses.kt");
    }

    @Test
    @TestMetadata("defaultMethodInKotlinWithSettingAll.kt")
    public void testDefaultMethodInKotlinWithSettingAll() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/defaultMethodInKotlinWithSettingAll.kt");
    }

    @Test
    @TestMetadata("defaultMethodInKotlinWithSettingAllCompatibility.kt")
    public void testDefaultMethodInKotlinWithSettingAllCompatibility() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/defaultMethodInKotlinWithSettingAllCompatibility.kt");
    }

    @Test
    @TestMetadata("delegatesWithAnnotations.kt")
    public void testDelegatesWithAnnotations() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/delegatesWithAnnotations.kt");
    }

    @Test
    @TestMetadata("delegatingToInterfaces.kt")
    public void testDelegatingToInterfaces() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/delegatingToInterfaces.kt");
    }

    @Test
    @TestMetadata("dollarsInNameLocal.kt")
    public void testDollarsInNameLocal() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/dollarsInNameLocal.kt");
    }

    @Test
    @TestMetadata("enums.kt")
    public void testEnums() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/enums.kt");
    }

    @Test
    @TestMetadata("generics.kt")
    public void testGenerics() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/generics.kt");
    }

    @Test
    @TestMetadata("implementingKotlinCollections.kt")
    public void testImplementingKotlinCollections() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/implementingKotlinCollections.kt");
    }

    @Test
    @TestMetadata("importAliases.kt")
    public void testImportAliases() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/importAliases.kt");
    }

    @Test
    @TestMetadata("inferringAnonymousObjectTypes.kt")
    public void testInferringAnonymousObjectTypes() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/inferringAnonymousObjectTypes.kt");
    }

    @Test
    @TestMetadata("inheritance.kt")
    public void testInheritance() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/inheritance.kt");
    }

    @Test
    @TestMetadata("inlineClasses.kt")
    public void testInlineClasses() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/inlineClasses.kt");
    }

    @Test
    @TestMetadata("inlineOnly.kt")
    public void testInlineOnly() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/inlineOnly.kt");
    }

    @Test
    @TestMetadata("inlineReified.kt")
    public void testInlineReified() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/inlineReified.kt");
    }

    @Test
    @TestMetadata("jvmField.kt")
    public void testJvmField() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmField.kt");
    }

    @Test
    @TestMetadata("jvmName.kt")
    public void testJvmName() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmName.kt");
    }

    @Test
    @TestMetadata("jvmOverloads.kt")
    public void testJvmOverloads() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmOverloads.kt");
    }

    @Test
    @TestMetadata("jvmRecord.kt")
    public void testJvmRecord() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmRecord.kt");
    }

    @Test
    @TestMetadata("jvmStaticOnPropertySetter.kt")
    public void testJvmStaticOnPropertySetter() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmStaticOnPropertySetter.kt");
    }

    @Test
    @TestMetadata("jvmSynthetic.kt")
    public void testJvmSynthetic() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmSynthetic.kt");
    }

    @Test
    @TestMetadata("jvmSyntheticForAccessors.kt")
    public void testJvmSyntheticForAccessors() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmSyntheticForAccessors.kt");
    }

    @Test
    @TestMetadata("jvmWildcardAnnotations.kt")
    public void testJvmWildcardAnnotations() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/jvmWildcardAnnotations.kt");
    }

    @Test
    @TestMetadata("LateinitProperties.kt")
    public void testLateinitProperties() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/LateinitProperties.kt");
    }

    @Test
    @TestMetadata("lateinitProperty.kt")
    public void testLateinitProperty() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/lateinitProperty.kt");
    }

    @Test
    @TestMetadata("localClassDerived.kt")
    public void testLocalClassDerived() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/localClassDerived.kt");
    }

    @Test
    @TestMetadata("objects.kt")
    public void testObjects() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/objects.kt");
    }

    @Test
    @TestMetadata("properties.kt")
    public void testProperties() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/properties.kt");
    }

    @Test
    @TestMetadata("simpleFunctions.kt")
    public void testSimpleFunctions() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/simpleFunctions.kt");
    }

    @Test
    @TestMetadata("throwsAnnotation.kt")
    public void testThrowsAnnotation() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/throwsAnnotation.kt");
    }

    @Test
    @TestMetadata("typeAliases.kt")
    public void testTypeAliases() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/typeAliases.kt");
    }

    @Test
    @TestMetadata("typeAnnotations.kt")
    public void testTypeAnnotations() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/typeAnnotations.kt");
    }

    @Test
    @TestMetadata("wildcardOptimization.kt")
    public void testWildcardOptimization() throws Exception {
        runTest("compiler/testData/asJava/lightClasses/lightClassByPsi/wildcardOptimization.kt");
    }
}
