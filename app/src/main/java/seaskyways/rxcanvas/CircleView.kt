package seaskyways.rxcanvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.computation
import io.reactivex.schedulers.Schedulers.newThread
import io.reactivex.subjects.*
import org.jetbrains.anko.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
class CircleView : View, AnkoLogger {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    //    val refresherSubject = PublishSubject.create<Any>()!!
    val Paints = object {
        
    }
    
    val currPointSubject = BehaviorSubject.createDefault<PointF>(PointF(0f, 0f))!!
    val ballsObservable = ReplaySubject.create<Ball>()!!
    val ballDisposalSubject: BehaviorSubject<Int> = BehaviorSubject.create<Int>()
    
    val userBallOverlapSubject = BehaviorSubject.createDefault(false)
    
    var shouldContinue = true
    
    var lifecycleObservable: Observable<ActivityEvent>? = null
        set(value) {
            field = value
            value?.subscribe { currLifecycle = it }
        }
    
    var currLifecycle: ActivityEvent = ActivityEvent.PAUSE
    
    val currX: Float get() = currPointSubject.value.x
    val currY: Float get() = currPointSubject.value.y
    
    inner class Ball(val id: Int = 1, val y: Int, val radius: Long = 100L, x: Long? = null) : Renderable {
        val minTime = (1000000000L * (Math.log10(id.toDouble()))).toLong()
        val maxTime = (3000000000L * (Math.log10(id.toDouble()))).toLong()
        
        fun getRandTime() = minTime + (Math.random() * (maxTime - minTime))
        val timeWithDistance: Lazy<Long>
            get() = lazyOf((getRandTime() / 1001.0).toLong().coerceAtLeast(1))
        
        var canDispose = false
        var isAnimating = false
        val xExtremity = measuredWidth.toLong() + radius
        
        var x = AtomicLong(x ?: xExtremity)
        
        val idTextBox = Rect()
        
        init {
            textPaint.getTextBounds(id.toString(), 0, id.toString().length, idTextBox)
        }
        
        fun startAnim() {
            isAnimating = true
            Observable.interval(timeWithDistance.value, TimeUnit.NANOSECONDS)
                    .filter { shouldContinue }
                    .map { 0.001 }
                    .scan(Double::plus)
                    .map { 1 - it }
                    .takeWhile { it > 0 }
                    .map { (xExtremity) * it - (radius / 2) }
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
        
        override fun render(canvas: Canvas) {
            canvas.drawCircle(this.x.get().toFloat(), this.y.toFloat(), radius.toFloat(), circlePaint)
            canvas.drawText(currentBalls.indexOf(this@Ball).toString(), this.x.get().toFloat(), this.y.toFloat() + this.idTextBox.height() / 2, textPaint)
            if (!this.isAnimating && !this.canDispose)
                this.startAnim()
        }
        
        fun isIntersecting(another: Ball): Boolean {
            return (Math.abs(x.get() - another.x.get()) <= radius * 2) && (Math.abs(y - another.y) <= radius * 2)
        }
    }
    
    val stroke = 30f
    val randY: Int get() = (measuredHeight * Math.random()).toInt()
    
    val circlePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }
    
    val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = sp(20).toFloat()
    }
    
    val refresher: Observable<Long>
    
    val currentBalls = AtomicArray<Ball?>(45)
    
    init {
        refresher = Observable.interval(15, TimeUnit.MILLISECONDS)
                .filter { shouldContinue }
                .observeOn(mainThread())
        
        refresher.subscribe { invalidate() }
        
        val timer = Observable.interval(250, TimeUnit.MILLISECONDS)
                .filter { shouldContinue }
                .map(Long::toInt)
        
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
//                .doOnNext {
//                    clearDisposableBalls()
//                }
                .subscribe { ball ->
                    val nearestNullIndex = nearestNullIndex()
                    nearestNullIndex.let {
                        currentBalls.set(it, ball)
                    }
                }
        
        refresher
                .observeOn(Schedulers.single())
                .subscribe {
                    val overlap = currentBalls
                            .indexOfFirst { it?.isIntersecting(Ball(y = currY.toInt(),x = currX.toLong())) ?: false } != -1
                    userBallOverlapSubject.onNext(overlap)
                }
    }
    
    fun nearestNullIndex(): Int {
        val index = (0 until currentBalls.length())
                .filter { currentBalls[it] == null }
                .firstOrNull() ?:
                run {
                    return@run 0
                }
        return index
    }
    
    val nearestNullIndexText: Renderable = object : Renderable {
        override fun render(canvas: Canvas) {
            canvas.drawText(nearestNullIndex().toString(), 50f, measuredHeight.toFloat() - 50, textPaint)
        }
    }
    
    fun clearDisposableBalls() {
//        (0 until currentBalls.length())
//                .forEach {
//                    if (currentBalls[it] != null) {
//                        currentBalls[it]?.let { ball ->
//                            if (ball.canDispose) {
//                                warn("Disposing ball ${ball.id}")
//                                currentBalls.set(it, null)
//                            }
//                        }
//                    }
//                }
    }
    
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        currPointSubject.onNext(PointF(event.x, event.y))
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            nearestNullIndexText.render(canvas)
            (0 until currentBalls.length())
                    .map { currentBalls[it] }
                    .filter { it != null }
                    .forEach {
                        it?.render(canvas)
                    }
            drawCircle(currX, currY, 100f, circlePaint)
        }
    }
    
}