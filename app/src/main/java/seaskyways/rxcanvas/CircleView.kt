package seaskyways.rxcanvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.computation
import io.reactivex.schedulers.Schedulers.newThread
import io.reactivex.subjects.*
import org.jetbrains.anko.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.*

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
class CircleView : View, AnkoLogger {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    //    val refresherSubject = PublishSubject.create<Any>()!!
    val currPointSubject = BehaviorSubject.createDefault<PointF>(PointF(0f, 0f))!!
    val ballsObservable = ReplaySubject.create<Ball>()!!
    val ballDisposalSubject = BehaviorSubject.create<Int>()
    
    var lifecycleObservable: Observable<ActivityEvent>? = null
        set(value) {
            field = value
            value?.subscribe { currLifecycle = it }
        }
    
    var currLifecycle: ActivityEvent = ActivityEvent.PAUSE
    
    val currX: Float get() = currPointSubject.value.x
    val currY: Float get() = currPointSubject.value.y
    
    inner class Ball(val id: Int = 1, val y: Int) {
        val minTime = (1000000000L * (Math.sqrt(id.toDouble()))).toLong()
        val maxTime = (3000000000L * (Math.sqrt(id.toDouble()))).toLong()
        
        fun getRandTime() = minTime + (Math.random() * (maxTime - minTime))
        val timeWithDistance: Lazy<Long>
            get() = lazyOf((getRandTime() / 1001.0).toLong().coerceAtLeast(1))
        
        var canDispose = false
        var isAnimating = false
        val xExtremity = measuredWidth.toLong() + 100L
        
        var x = AtomicLong(xExtremity)
        
        val idTextBox = Rect()
        
        init {
            textPaint.getTextBounds(id.toString(), 0, id.toString().length, idTextBox)
        }
        
        fun startAnim() {
            isAnimating = true
            Observable.interval(timeWithDistance.value, TimeUnit.NANOSECONDS)
                    .filter { currLifecycle == ActivityEvent.RESUME }
                    .map { 0.001 }
                    .scan(Double::plus)
                    .map { 1 - it }
                    .takeWhile { it > 0 }
                    .map { (xExtremity) * it - 50 }
                    .map(Double::toLong)
                    .subscribe(
                            {
                                x.set(it)
                            },
                            Throwable::printStackTrace
                            ,
                            {
                                //                                warn("CAN DISPOSE !")
                                ballDisposalSubject.onNext(id)
                                canDispose = true
                                isAnimating = false
                            })
        }
    }
    
    val radius = 30f
    val randY: Int get() = (measuredHeight * Math.random()).toInt()
    
    val circlePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = radius
    }
    
    val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = sp(20).toFloat()
    }
    
    val refresher: Observable<Long>
    
    val currentBalls = AtomicReferenceArray<Ball?>(45)
    
    init {
        refresher = Observable.interval(15, TimeUnit.MILLISECONDS)
                .filter { currLifecycle == ActivityEvent.RESUME }
                .observeOn(mainThread())
        
        refresher.subscribe { invalidate() }
        
        val timer = Observable.interval(1, TimeUnit.SECONDS)
                .filter { currLifecycle == ActivityEvent.RESUME }
                .map { 1 }
                .scan { i2: Int, i1: Int -> i2 + i1 }
        
        timer
                .subscribeOn(computation())
                .subscribe({ ballsObservable.onNext(Ball(it, randY)) }, Throwable::printStackTrace)
        
        ballDisposalSubject
                .subscribeOn(newThread())
                .flatMap {
                    (0 until currentBalls.length())
                            .filter { i -> currentBalls[i]?.id ?: 0 == it }
                            .forEach { return@flatMap Observable.just(it) }
                    return@flatMap Observable.empty<Int>()
                }
                .subscribe {
                    currentBalls.set(it, null)
                    System.gc()
                }
        
        ballsObservable
                .subscribeOn(newThread())
                .observeOn(newThread())
                .doOnNext {
                    clearDisposableBalls()
                }
                .subscribe { ball ->
                    val nearestNullIndex = nearestNullIndex() ?: run {
                        currentBalls.set(0, null)
                        0
                    }
                    nearestNullIndex.let {
                        currentBalls.set(it, ball)
                    }
                }
    }
    
    fun nearestNullIndex(): Int? {
        (0 until currentBalls.length())
                .filter { currentBalls[it] == null }
                .forEach {
                    return it
                }
        return null
    }
    
    fun clearDisposableBalls() {
        (0 until currentBalls.length())
                .forEach {
                    if (currentBalls[it] != null) {
                        warn("can dispose $it : ${currentBalls[it]?.canDispose}")
                        currentBalls[it]?.let { ball ->
                            if (ball.canDispose) {
                                warn("Disposing ball ${ball.id}")
                                currentBalls.set(it, null)
                            }
                        }
                    }
                }
    }
    
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        currPointSubject.onNext(PointF(event.x, event.y))
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            drawCircle(currX, currY, 100f, circlePaint)
            
            (0 until currentBalls.length())
                    .map { currentBalls[it] }
                    .filter { it != null }
                    .forEach {
                        it?.let { ball ->
                            drawCircle(ball.x.get().toFloat(), ball.y.toFloat(), 100f, circlePaint)
                            drawText(ball.id.toString(), ball.x.get().toFloat(), ball.y.toFloat() + ball.idTextBox.height() / 2, textPaint)
                            if (!ball.isAnimating && !ball.canDispose)
                                ball.startAnim()
                        }
                    }
        }
    }
    
}