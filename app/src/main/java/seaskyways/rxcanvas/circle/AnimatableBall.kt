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
        strokeWidthObservable: Observable<Float>,
        animationField: Rect,
        val isRtl: Boolean = false,
        context: Context? = null
) : Ball(id, center, radius, strokeWidthObservable), Animatable {
    object Defaults {
        const val minTime = 1_000_000_000L /*NANOSECONDS*/
        const val maxTime = 3_000_000_000L
        const val numberOfAnimationEmissions = 1001 /*from 0 to 1000 inclusive*/
        fun getRandTime(): Double = minTime + (Math.random() * (maxTime - minTime))
        fun getRandomTimeIntervalFromEmissions() = (getRandTime() / numberOfAnimationEmissions).toLong().coerceAtLeast(1)
    }
    
    val ctxRef = WeakReference(context)
    
    var canDispose = false
        get() = field && !isAnimating
    
    private var isAnimating = false
    
    val xExtremity = animationField.width() + radius
    
    val animationObservable: ConnectableObservable<Double> =
            Observable.interval(Defaults.getRandomTimeIntervalFromEmissions(), TimeUnit.NANOSECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .filter { !canDispose }
                    .map { 0.001 }
                    .scan(Double::plus)
                    .map { 1 - it }
                    .takeWhile { it > 0 }
                    .onTerminateDetach()
                    .map { (xExtremity) * it - (radius / 2) }
                    .publish()
    
    
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
                        },
                        Throwable::printStackTrace
                        ,
                        {
                            stop()
                            dispose()
//                            score.incrementAndGet()
//                            ballDisposalSubject.onNext(id)
                        })
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
    }
    
}