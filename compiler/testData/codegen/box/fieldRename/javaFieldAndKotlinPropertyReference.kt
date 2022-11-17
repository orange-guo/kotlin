// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// Field VS property: case "reference"
// DUMP_IR

// FILE: BaseJava.java
public class BaseJava {
    public String a = "O";

    String b = "K";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "FAIL"

    private val b = "FAIL"
}

fun box(): String {
    val d = Derived()
    return d::a.get() + d::b.get()
}
