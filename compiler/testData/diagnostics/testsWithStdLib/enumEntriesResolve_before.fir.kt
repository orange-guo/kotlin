// !LANGUAGE: -EnumEntries
// FIR_DUMP
// FILE: JavaEnum.java

public enum JavaEnum {
    SINGLE;

    public static final String entries = "E";
}

// FILE: JavaOtherEnum.java

public enum JavaOtherEnum {
    SINGLE;

    public static final String getEntries() {
        return "E";
    };
}

// FILE: JavaAnotherEnum.java

public enum JavaAnotherEnum {
    entries;
}

// FILE: test.kt
@file:OptIn(ExperimentalStdlibApi::class)

enum class E {
    SINGLE;

    companion object {
        val entries = "E"
    }
}

enum class EE {
    entries;
}

val e1 = E.entries
val e2 = EE.entries
val e3 = JavaEnum.entries
val e4 = JavaOtherEnum.entries
val e5 = JavaAnotherEnum.entries
