package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import io.reactivex.disposables.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.*
import org.jetbrains.anko.dip
import seaskyways.rxcanvas.*
import java.lang.*
import java.lang.Math.pow

/**
 * Created by Ahmad on 26/01 Jan/2017.
 */
open class Ball(
        val id: Int,
        val center: PointF = PointF(),
        var radius: Float,
        var strokeWidth: Float,
        val minimumRadius: Float = 0f,
        val minimumStrokeWidth: Float = 5f,
        val isDynamic: Boolean = false
) : Renderable, Disposable {
    val disposables = CompositeDisposable()
    val asCircle get() = Circle(center, radius, id)
    
    open val ballPaint = lazy {
        Paint().also {
            it.style = Paint.Style.STROKE
            it.strokeWidth = strokeWidth
            it.color = Color.GREEN
        }
    }
    
    private var velocitySubject: BehaviorSubject<Double> = BehaviorSubject.createDefault<Double>(0.0)
    private var velocity: Double
        get() = velocitySubject.value
        set(value) {
            velocitySubject.onNext(value)
        }
    
    
    internal fun setCurrentPositionSubject(s: PublishSubject<Circle>) {
        if (currentPositionSubject == null) {
            currentPositionSubject = s
        } else {
            throw UnsupportedOperationException()
        }
    }
    
    protected var currentPositionSubject: PublishSubject<Circle>? = null
    val currentPositionObservable get() = currentPositionSubject?.hide()
    
    val baseRadius = radius
    val baseStrokeWidth = strokeWidth
    
    init {
        if (isDynamic) {
            velocitySubject
                    .subscribeOn(Schedulers.newThread())
                    .map { it * 4.0 }
                    .subscribe { newVelocity ->
                        radius = (baseRadius - newVelocity).toFloat().coerceAtLeast(minimumRadius)
                        if (radius == minimumRadius) {
                            strokeWidth = (baseStrokeWidth - radius).coerceAtLeast(minimumStrokeWidth)
                            ballPaint.value.strokeWidth = strokeWidth
                        } else {
                            strokeWidth = baseStrokeWidth
                            ballPaint.value.strokeWidth = strokeWidth
                        }
                    }
                    .addToDisposables(disposables)
        }
    }
    
    fun updateVelocity(p: PointF, userVelocityRefreshRate: Long, ctx: Context, shouldUpdatePosition: Boolean = true) = with(ctx) {
        val dx = dip(p.x - center.x)
        val dy = dip(p.y - center.y)
        val dx2 = dx * dx
        val dy2 = dy * dy
        velocity = (Math.sqrt((dx2 + dy2).toDouble()) / userVelocityRefreshRate)
        if (shouldUpdatePosition) updatePosition(p)
        return@with velocity
    }
    
    private fun updatePosition(p: PointF) = center.set(p)
    
    override fun render(canvas: Canvas) {
        canvas.drawCircle(center.x, center.y, radius, ballPaint.value)
    }
    
    override fun isDisposed() = disposables.isDisposed
    override fun dispose() = disposables.dispose()
    
    fun isIntersecting(another: Ball): Boolean {
        val distanceXS = pow((center.x - another.center.x).toDouble(), 2.0)
        val distanceYS = pow((center.y - another.center.y).toDouble(), 2.0)
        val radiiS = pow(((radius + strokeWidth / 4) + (another.radius + another.strokeWidth / 4)).toDouble(), 2.0)
        return distanceXS + distanceYS <= (radiiS)
    }
}
