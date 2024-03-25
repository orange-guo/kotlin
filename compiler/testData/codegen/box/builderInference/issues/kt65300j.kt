// ISSUE: KT-65300

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: compile-time failure in K1 (java.lang.NullPointerException @ org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor.isExported)
// REASON: red code in K2 (see corresponding diagnostic test)

fun box(): String {
    build {
        class TypeInfoSourceClass: Buildee<TargetType> by this@build
    }
    return "OK"
}




class TargetType

interface Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return (object: Buildee<PTV> {}).apply(instructions)
}
