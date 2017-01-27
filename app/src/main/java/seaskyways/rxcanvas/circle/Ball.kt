package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import io.reactivex.disposables.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.jetbrains.anko.dip
import seaskyways.rxcanvas.Renderable

/**
 * Created by Ahmad on 26/01 Jan/2017.
 */
open class Ball(
        val id: Int,
        val center: PointF,
        var radius: Float,
        var strokeWidth: Float,
        val minimumRadius: Float = 0f,
        val minimumStrokeWidth : Float = 5f
) : Renderable, Disposable {
    val disposables = CompositeDisposable()
    
    open val ballPaint = lazy {
        Paint().also {
            it.style = Paint.Style.STROKE
            it.strokeWidth = strokeWidth
            it.color = Color.BLACK
        }
    }
    
    private var velocitySubject: BehaviorSubject<Double> = BehaviorSubject.createDefault<Double>(0.0)
    private var velocity: Double
        get() = velocitySubject.value
        set(value) {
            velocitySubject.onNext(value)
        }
    
    
    val baseRadius = radius
    val baseStrokeWidth = strokeWidth
    
    init {
        disposables.add(
                velocitySubject
                        .subscribeOn(Schedulers.newThread())
                        .subscribe { newVelocity ->
                            radius = ((baseRadius - newVelocity).toFloat().coerceAtLeast(minimumRadius))
                            if (radius == minimumRadius) {
                                strokeWidth = (baseStrokeWidth - radius).coerceAtLeast(minimumStrokeWidth)
                                ballPaint.value.strokeWidth = strokeWidth
                            }
                        }
        )
    }
    
    fun updateVelocity(p: PointF, userVelocityRefreshRate: Long, ctx: Context, shouldUpdatePosition: Boolean = true) = with(ctx) {
        val dx = dip(p.x - center.x)
        val dy = dip(p.y - center.y)
        velocity = (Math.sqrt((dx * dx + dy * dy).toDouble()) / userVelocityRefreshRate)
        if (shouldUpdatePosition) updatePosition(p)
        return@with velocity
    }
    
    private fun updatePosition(p: PointF) = center.set(p)
    
    override fun render(canvas: Canvas) {
        canvas.drawCircle(center.x, center.y, radius, ballPaint.value)
    }
    
    override fun isDisposed() = disposables.isDisposed
    override fun dispose() = disposables.dispose()
}
