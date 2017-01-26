package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import io.reactivex.Observable
import io.reactivex.disposables.*
import io.reactivex.schedulers.Schedulers
import seaskyways.rxcanvas.Renderable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Created by Ahmad on 26/01 Jan/2017.
 */
open class Ball(
        val id: Int,
        val center: PointF,
        var radius: Float,
        strokeWidthObservable: Observable<Float>
) : Renderable, Disposable {
    val disposables = CompositeDisposable()
    
    init {
        disposables.add(
                strokeWidthObservable.subscribe { ballPaint.value.strokeWidth = it }
        )
    }
    
    open val ballPaint = lazy {
        Paint().apply {
            style = Paint.Style.STROKE
//            strokeWidth = this@Ball.strokeWidthObservable.blockingLatest().first()
            color = Color.BLACK
        }
    }
    
    override fun render(canvas: Canvas) {
        canvas.drawCircle(center.x, center.y, radius, ballPaint.value)
    }
    
    override fun isDisposed() = disposables.isDisposed
    override fun dispose() = disposables.dispose()
}
