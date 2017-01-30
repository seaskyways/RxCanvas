package seaskyways.rxcanvas.circle

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Created by Ahmad on 26/01 Jan/2017.
 */
class AnimatableBall(
        id: Int,
        center: android.graphics.PointF,
        radius: Float,
        strokeWidth: Float,
        animationField: android.graphics.Rect,
        isRtl: Boolean = false,
        context: android.content.Context? = null
) : Ball(id, center, radius, strokeWidth), android.graphics.drawable.Animatable, org.jetbrains.anko.AnkoLogger {
    object Defaults {
        const val MIN_TIME = 1_000_000_000L /*NANOSECONDS*/
        const val MAX_TIME = 3_000_000_000L
        const val NUMBER_OF_ANIMATION_EMISSIONS = 1001
        fun getRandTime(): Double = seaskyways.rxcanvas.circle.AnimatableBall.Defaults.MIN_TIME + (Math.random() * (seaskyways.rxcanvas.circle.AnimatableBall.Defaults.MAX_TIME - seaskyways.rxcanvas.circle.AnimatableBall.Defaults.MIN_TIME))
        fun getRandomTimeIntervalFromEmissions(factor: Int = 1) = (seaskyways.rxcanvas.circle.AnimatableBall.Defaults.getRandTime() * factor.coerceAtLeast(1) / seaskyways.rxcanvas.circle.AnimatableBall.Defaults.NUMBER_OF_ANIMATION_EMISSIONS).toLong().coerceAtLeast(1)
    }
    
    private val ctxRef = java.lang.ref.WeakReference(context)
    
    var canDispose = false
        private set
    
    private var isAnimating = false
    
    val startXExtremity = animationField.width() + (baseRadius * 2)
    val endXExtremity = baseRadius * 2
    
    init {
        center.x = if (isRtl) startXExtremity else endXExtremity
    }
    
    private var _doOnNext: (() -> Unit)? = null
    fun doOnNext(b: () -> Unit) {
        _doOnNext = b
    }
    
    val animationObservable: io.reactivex.observables.ConnectableObservable<Double> =
            io.reactivex.Observable.interval(
                    seaskyways.rxcanvas.circle.AnimatableBall.Defaults.getRandomTimeIntervalFromEmissions(
                            Math.log10(
                                    id.toDouble()
                            ).toInt()
                    ), java.util.concurrent.TimeUnit.NANOSECONDS)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.computation())
                    .filter { isAnimating }
                    .map { 1.0 / seaskyways.rxcanvas.circle.AnimatableBall.Defaults.NUMBER_OF_ANIMATION_EMISSIONS }
                    .scan(Double::plus)
                    .map {
                        if (!isRtl) 1 - it else it
                    }
                    .takeWhile {
                        if (!isRtl) it >= 0 else it <= 1
                    }
                    .onTerminateDetach()
                    .map { (startXExtremity * it) - (endXExtremity) }
                    .publish()
    
    override val ballPaint: Lazy<android.graphics.Paint> = lazy {
        super.ballPaint.value.also {
            it.color = Color.WHITE
            it.xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
    }
    
    override fun render(canvas: android.graphics.Canvas) {
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