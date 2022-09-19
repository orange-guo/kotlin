// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

@JvmInline
@JvmExposeBoxed
value class Example(val s: String) {
    constructor(i: Int) : this(i.toString())

    init {
        println("beb")
    }

    fun x(): Unit = println(s)
    fun y(another: Example): Unit = println(s + another.s)
}

fun box(): String {
    return "OK"
}

