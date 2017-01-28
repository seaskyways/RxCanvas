package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import io.reactivex.Observable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Created by Ahmad on 26/01 Jan/2017.
 */
class AnimatableBall(
        id: Int,
        center: PointF,
        radius: Float,
        strokeWidth: Float,
        animationField: Rect,
        val isRtl: Boolean = false,
        context: Context? = null
) : Ball(id, center, radius, strokeWidth), Animatable {
    object Defaults {
        const val MIN_TIME = 1_000_000_000L /*NANOSECONDS*/
        const val MAX_TIME = 3_000_000_000L
        const val NUMBER_OF_ANIMATION_EMISSIONS = 600
        fun getRandTime(): Double = MIN_TIME + (Math.random() * (MAX_TIME - MIN_TIME))
        fun getRandomTimeIntervalFromEmissions(factor : Int = 1) = (getRandTime() * factor / NUMBER_OF_ANIMATION_EMISSIONS).toLong().coerceAtLeast(1)
    }
    
    val ctxRef = WeakReference(context)
    
    var canDispose = false
        get() = field && !isAnimating
    
    private var isAnimating = false
    
    val xExtremity = animationField.width() + radius
    
    private var _doOnNext: (() -> Unit)? = null
    fun doOnNext(b: () -> Unit) {
        _doOnNext = b
    }
    
    val animationObservable: ConnectableObservable<Double> =
            Observable.interval(Defaults.getRandomTimeIntervalFromEmissions(Math.log10(id.toDouble()).toInt().coerceAtLeast(1)), TimeUnit.NANOSECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .filter { isAnimating }
                    .map { 1.0 / Defaults.NUMBER_OF_ANIMATION_EMISSIONS }
                    .scan(Double::plus)
                    .map { 1 - it }
                    .takeWhile { it > 0 }
                    .onTerminateDetach()
                    .map { (xExtremity) * it - (radius / 2) }
                    .publish()
    
    override val ballPaint: Lazy<Paint> = lazy {
        super.ballPaint.value.also {
            it.color = Color.WHITE
            it.xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
    }
    
    override fun render(canvas: Canvas) {
        super.render(canvas)
    }
    
    override fun isRunning() = isAnimating
    
    override fun start() {
        if (isRunning || canDispose) return
        isAnimating = true
        
        val disposable = animationObservable
                .map(Double::toFloat)
                .subscribe(
                        {
                            center.set(it, center.y)
                            _doOnNext?.invoke()
                            currentPositionSubject?.onNext(Circle(center, radius, id))
                        }
                        , Throwable::printStackTrace
                        , this::dispose
                )
        animationObservable.connect()
        disposables.add(disposable)
    }
    
    override fun stop() {
        isAnimating = false
        canDispose = true
    }
    
    private var _onDispose: (() -> Unit)? = null
    fun doOnDispose(d: () -> Unit) {
        _onDispose = d
    }
    
    override fun dispose() {
        super.dispose()
        stop()
        _onDispose?.invoke()
        currentPositionSubject = null
    }
    
}