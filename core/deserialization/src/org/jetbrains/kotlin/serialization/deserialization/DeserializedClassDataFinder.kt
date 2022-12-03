/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.packageFragments
import org.jetbrains.kotlin.name.ClassId

class DeserializedClassDataFinder(private val packageFragmentProvider: PackageFragmentProvider) : ClassDataFinder {
    override fun findClassData(classId: ClassId, languageVersion: LanguageVersion): ClassData? {
        val packageFragments = packageFragmentProvider.packageFragments(classId.packageFqName)
        for (fragment in packageFragments) {
            if (fragment !is DeserializedPackageFragment) continue

            fragment.classDataFinder.findClassData(classId, languageVersion)?.let { return it }
        }
        return null
    }
}
