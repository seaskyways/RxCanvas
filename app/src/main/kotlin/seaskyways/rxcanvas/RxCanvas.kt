package seaskyways.rxcanvas

import android.app.Application
import android.content.Context
import io.realm.*
import java.lang.ref.WeakReference

/**
 * Created by Ahmad on 01/02 Feb/2017.
 */
class RxCanvas : Application() {
    companion object{
        private var ctxRef = WeakReference(null as Application?)
        val appCtx get() = ctxRef.get()
        inline fun <R> doWithAppCtx(block : (Application) -> R) : R? = appCtx?.let(block)
    }
    
    override fun onCreate() {
        super.onCreate()
        ctxRef = WeakReference(this)
        
        Realm.init(this)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder().build())
    }
}