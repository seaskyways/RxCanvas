@file:Suppress("NOTHING_TO_INLINE")

package seaskyways.rxcanvas

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.realm.Realm
import java.lang.ref.WeakReference

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
inline fun <reified T : Any> T.className(): String
        = T::class.java.simpleName

inline fun <T, R> WeakReference<T>.safe(body: T.() -> R?): R? {
    return this.get()?.body()
}

inline fun <T : Disposable> T.addToDisposables(compositeDisposable: CompositeDisposable) = compositeDisposable.add(this)

infix fun Number.power(pow: Number) = Math.pow(this.toDouble(), pow.toDouble())

val realm: Realm get() = Realm.getDefaultInstance()