package seaskyways.rxcanvas

import java.lang.ref.WeakReference

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
inline fun <reified T : Any> T.className(): String
        = T::class.java.simpleName

fun <T, R> WeakReference<T>.safe(body: T.() -> R?): R? {
    return this.get()?.body()
}