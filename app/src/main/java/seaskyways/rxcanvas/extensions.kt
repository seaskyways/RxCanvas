@file:Suppress("NOTHING_TO_INLINE")

package seaskyways.rxcanvas

import io.reactivex.disposables.*
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
inline fun <reified T : Any> T.className(): String
        = T::class.java.simpleName

inline fun <T, R> WeakReference<T>.safe(body: T.() -> R?): R? {
    return this.get()?.body()
}

inline fun <T : Disposable> T.addToDisposables(compositeDisposable: CompositeDisposable) = compositeDisposable.add(this)