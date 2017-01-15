package seaskyways.rxcanvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.newThread
import io.reactivex.subjects.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
class CircleView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    val refresherSubject = PublishSubject.create<Any>()!!
    val currPointSubject = BehaviorSubject.createDefault<PointF>(PointF(0f, 0f))!!
    
    val currX: Float get() = currPointSubject.value.x
    val currY: Float get() = currPointSubject.value.y
    
    inner class Ball(val id: Int = 1, val y: Int) {
        var canDispose = false
        var isAnimating = false
        val xExtrimity = measuredWidth.toLong() + 100
        var x = xExtrimity
        fun startAnim() {
            isAnimating = true
            
            Observable.intervalRange(100, xExtrimity, 0, (3000f / xExtrimity).toLong(), TimeUnit.MILLISECONDS)
                    .map { xExtrimity - it }
                    .subscribe({ x = it }, {}, {
                        canDispose = true
                        isAnimating = false
                    })
        }
    }
    
    val radius = 30f
    val incomingBallsList = ArrayList<Ball>()
    val randY: Int get() = (measuredHeight * Math.random()).toInt()
    
    val circlePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = radius
    }
    
    init {
        Observable.interval(15, TimeUnit.MILLISECONDS)
                .observeOn(mainThread())
                .subscribe { invalidate() }
        
        val timer = Observable.interval(1, TimeUnit.SECONDS)
                .map { 1 }
                .scan { i2: Int, i1: Int -> i2 + i1 }
        
        timer
                .subscribeOn(newThread())
                .observeOn(newThread())
                .subscribe { incomingBallsList.add(Ball(it, randY)) }
        
    }
    
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        currPointSubject.onNext(PointF(event.x, event.y))
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            drawCircle(currX, currY, 100f, circlePaint)
            incomingBallsList
                    .filter { !it.canDispose }
                    .forEach { ball ->
                        drawCircle(ball.x.toFloat(), ball.y.toFloat(), 100f, circlePaint)
                        if (!ball.isAnimating)
                            ball.startAnim()
                    }
        }
    }
    
}