annotation class Ann(val kClass: KClass<*>)

@Ann(some.name.Unresolved::class)
class A

// COMPILATION_ERRORS
