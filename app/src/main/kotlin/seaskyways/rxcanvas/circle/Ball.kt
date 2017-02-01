package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.dip
import org.jetbrains.anko.warn
import seaskyways.rxcanvas.Renderable
import seaskyways.rxcanvas.addToDisposables
import seaskyways.rxcanvas.power
import seaskyways.seaskyways.rxcanvas.circle.ScriptC_circle_ops
import java.lang.Math.pow
import java.lang.UnsupportedOperationException

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
) : Renderable, Disposable, AnkoLogger {
    val disposables = CompositeDisposable()
    val asCircle get() = Circle(center, radius, id)
    
    open val ballPaint = lazy {
        Paint().also {
            it.style = Paint.Style.STROKE
            it.strokeWidth = strokeWidth
            it.color = Color.GREEN
        }
    }
    
    private var velocitySubject: BehaviorSubject<Double> = BehaviorSubject.create<Double>()
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
        val distanceXS = (center.x - another.center.x) power 2
        val distanceYS = (center.y - another.center.y) power 2
        val radiiS = pow(((radius + strokeWidth / 4) + (another.radius + another.strokeWidth / 4)).toDouble(), 2.0)
        return distanceXS + distanceYS <= (radiiS)
    }
    
    fun isIntersection(renderScript: RenderScript, another: Ball): Single<Boolean> {
        val script = ScriptC_circle_ops(renderScript)
        script.apply {
            _x_in1 = center.x
            _y_in1 = center.y
            _radius_in1 = radius
            _stroke_width1 = strokeWidth
            _x_in2 = another.center.x
            _y_in2 = another.center.y
            _radius_in2 = another.radius
            _stroke_width2 = another.strokeWidth
            _result = -2
        }
        return Single.create { emitter ->
            val alloc = Allocation.createTyped(renderScript, Type.Builder(renderScript, Element.I32(renderScript)).create())
            val result = IntArray(1) { -2 }
            script.forEach_find_intersection_new(alloc)
            alloc.copy1DRangeFrom(0, 1, result)
            warn(result[0])
            emitter.onSuccess(result[0] == 1)
        }
        
//        return Single.fromCallable {
//            script.invoke_find_intersection()
//            while (script._result.toInt() == -2) {
//            }
//            script._result.toInt()
//        }
//                .subscribeOn(Schedulers.newThread())
//                .map { it == 1 }
    }
    
    init {
        if (isDynamic) {
            initVelocity()
        }
    }
    
    protected fun initVelocity(){
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
