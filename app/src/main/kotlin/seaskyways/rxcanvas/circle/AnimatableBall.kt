package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import io.reactivex.Observable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.AnkoLogger
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
        isRtl: Boolean = false,
        context: Context? = null
) : Ball(id, center, radius, strokeWidth), Animatable, AnkoLogger {
    object Defaults {
        const val MIN_TIME = 1_000_000_000L /*NANOSECONDS*/
        const val MAX_TIME = 3_000_000_000L
        const val NUMBER_OF_ANIMATION_EMISSIONS = 1001
        fun getRandTime(): Double = Defaults.MIN_TIME + (Math.random() * (Defaults.MAX_TIME - Defaults.MIN_TIME))
        fun getRandomTimeIntervalFromEmissions(factor: Int = 1) = (Defaults.getRandTime() * factor.coerceAtLeast(1) / Defaults.NUMBER_OF_ANIMATION_EMISSIONS).toLong().coerceAtLeast(1)
    }
    
    private val ctxRef = WeakReference(context)
    
    var canDispose = false
        private set
    
    private var isAnimating = false
    
    val startXExtremity = animationField.width() + (baseRadius * 2)
    val endXExtremity = -baseRadius * 2
    
    init {
        center.x = if (isRtl) endXExtremity else startXExtremity
    }
    
    private var _doOnNext: (() -> Unit)? = null
    fun doOnNext(b: () -> Unit) {
        _doOnNext = b
    }
    
    val animationObservable: ConnectableObservable<Double> =
            Observable.interval(
                    Defaults.getRandomTimeIntervalFromEmissions(
                            Math.log10(
                                    id.toDouble()
                            ).toInt()
                    ), TimeUnit.NANOSECONDS)
                    .subscribeOn(Schedulers.computation())
                    .filter { isAnimating }
                    .map { 1.0 / Defaults.NUMBER_OF_ANIMATION_EMISSIONS }
                    .scan(Double::plus)
                    .map { if (!isRtl) 1 - it else it }
                    .takeWhile { if (!isRtl) it > 0 else it < 1 }
                    .onTerminateDetach()
                    .map { (startXExtremity * it) + (endXExtremity) }
                    .publish()
    
    override val ballPaint: Lazy<Paint> = lazy {
        super.ballPaint.value.also {
            it.color = Color.WHITE
            it.xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
    }
    
    override fun render(canvas: Canvas) {
        if (isRunning || canDispose)
            super.render(canvas)
    }
    
    override fun isRunning() = isAnimating
    
    override fun start() {
        if (isRunning || canDispose) return
        val disposable = animationObservable
                .map(Double::toFloat)
                .subscribe(
                        {
                            center.x = it
                            _doOnNext?.invoke()
                            currentPositionSubject?.onNext(Circle(center, radius, id))
                        }
                        , Throwable::printStackTrace
                        , this::dispose
                )
        animationObservable.connect()
        disposables.add(disposable)
        isAnimating = true
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