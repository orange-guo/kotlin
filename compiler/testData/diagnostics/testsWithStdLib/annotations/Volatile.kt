// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
import kotlin.jvm.Volatile as JvmVolatile
import kotlin.concurrent.Volatile
import kotlin.properties.Delegates

@OptIn(ExperimentalStdlibApi::class)
class ConcurrentVolatile {
    <!VOLATILE_ON_VALUE!>@Volatile<!> val x = 0
    // ok
    @Volatile var y = 1

    <!VOLATILE_ON_DELEGATE!>@delegate:Volatile<!> var z: String by Delegates.observable("?") { prop, old, new -> old.hashCode() }

    <!VOLATILE_ON_VALUE!>@field:Volatile<!> val w = 2
}

class JvmVolatile {
    <!VOLATILE_ON_VALUE!>@JvmVolatile<!> val x = 0
    // ok
    @JvmVolatile var y = 1

    <!VOLATILE_ON_DELEGATE!>@delegate:JvmVolatile<!> var z: String by Delegates.observable("?") { prop, old, new -> old.hashCode() }

    <!VOLATILE_ON_VALUE!>@field:JvmVolatile<!> val w = 2
}
