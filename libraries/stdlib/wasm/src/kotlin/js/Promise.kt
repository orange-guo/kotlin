/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

public external interface JsValue

public external class Promise<T>(executor: (resolve: (JsValue?) -> Unit, reject: (JsValue) -> Unit) -> Unit): JsValue {
    public open fun <S> then(onFulfilled: (JsValue?) -> JsValue?): Promise<S>
    public open fun <S> then(onFulfilled: (JsValue?) -> JsValue?, onRejected: (JsValue) -> JsValue?): Promise<S>
    public open fun <S> catch(onRejected: (JsValue) -> JsValue?): Promise<S>
    public open fun <S> finally(onFinally: () -> Unit): Promise<S>

    public companion object {
        public fun <S> reject(e: JsValue): Promise<S>
        public fun <S> resolve(e: JsValue): Promise<S>
        public fun <T, S> resolve(e: Promise<T>): Promise<S>
    }
}

@JsFun("e => { throw e; }")
internal external fun jsThrow(e: JsValue)

@JsFun("""(f) => {
    let result = null;
    try { 
        f();
    } catch (e) {
       result = e;
    }
    return result;
}""")
internal external fun jsCatch(f: () -> Unit): JsValue?

public fun JsValue.toThrowableOrNull(): Throwable? {
    val thisAny: Any = this
    if (thisAny is Throwable) return thisAny
    var result: Throwable? = null
    jsCatch {
        try {
            jsThrow(this)
        } catch (e: Throwable) {
            result = e
        }
    }
    return result
}
