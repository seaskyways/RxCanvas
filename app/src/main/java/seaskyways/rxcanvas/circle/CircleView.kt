package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.*
import io.reactivex.subjects.*
import io.reactivex.subscribers.DisposableSubscriber
import org.jetbrains.anko.*
import seaskyways.rxcanvas.*
import seaskyways.rxcanvas.Renderable.Companion.rederable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
class CircleView : View, AnkoLogger, Disposable {
    
    
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    
    private val disposables = CompositeDisposable()
    private fun Disposable.addToDisposables() = addToDisposables(disposables)
    
    private val Subjects = object {
        val refresh = PublishSubject.create<Unit>()!!
        val userPoint = PublishSubject.create<PointF>()
    }
    private val Paints = object {
        val textPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = sp(20).toFloat()
        }
        
        val bottomLeftText = Paint(textPaint).apply {
            textAlign = Paint.Align.LEFT
            color = Color.WHITE
            xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
        
        
        val circlePaint by lazy {
            Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = defaultStrokeWidth.toFloat()
            }
        }
        
        val overlapPaint by lazy {
            Paint(circlePaint).apply {
                color = Color.RED
            }
        }
    }
    
    override fun isDisposed(): Boolean {
        return disposables.isDisposed
    }
    
    override fun dispose() {
        disposables.dispose()
    }
    
    val baseUserBallRadius by lazy { dip(30) }
    val defaultStrokeWidth by lazy { dip(10) }
    
    val userBall by lazy {
        Ball(
                id = 0,
                radius = baseUserBallRadius.toFloat(),
                strokeWidth = defaultStrokeWidth.toFloat(),
                isDynamic = true,
                center = PointF(measuredWidth / 8f, measuredHeight / 2f)
        )
    }
    val userBallRect by lazy { RectF() }
    
    init {
        val userVelocityRefreshRate = 15L
        val sampledUserPoint = Subjects.userPoint
                .subscribeOn(Schedulers.computation())
                .toFlowable(BackpressureStrategy.DROP)
                .sample(userVelocityRefreshRate, TimeUnit.MILLISECONDS)
                .onTerminateDetach()
        
        sampledUserPoint
                .subscribeWith(object : DisposableSubscriber<PointF>() {
                    override fun onError(t: Throwable?) = TODO()
                    override fun onComplete() = TODO()
                    override fun onNext(it: PointF) {
                        userBall.updateVelocity(it, userVelocityRefreshRate, ctx = context)
                        request(2)
                    }
                })
                .addToDisposables()
        
//        sampledUserPoint
//                .observeOn(newThread())
//                .subscribe {
//
//                }
    }
    
    val score = AtomicInteger(0)
    
    val ballsObservable = PublishSubject.create<AnimatableBall>()!!
    val ballDisposalSubject: BehaviorSubject<Int> = BehaviorSubject.create<Int>()
    
    val userBallOverlapSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
    
    var shouldContinue = true
    
    var lifecycleObservable: Observable<ActivityEvent>? = null
        set(value) {
            field = value
            value?.subscribe { currLifecycle = it }
        }
    
    var currLifecycle: ActivityEvent = ActivityEvent.PAUSE

//    inner class OldBall(val id: Int = 1, val y: Int, val radius: Long = dip(25).toLong(), x: Long? = null, val strokeWidth: Int = defaultStrokeWidth) : Renderable {
//        val MIN_TIME = (1000000000L * (Math.log10(id.toDouble()))).toLong()
//        val MAX_TIME = (3000000000L * (Math.log10(id.toDouble()))).toLong()
//
//        fun getRandTime() = MIN_TIME + (Math.random() * (MAX_TIME - MIN_TIME))
//        val timeWithDistance: Lazy<Long>
//            get() = lazyOf((getRandTime() / 1001.0).toLong().coerceAtLeast(1))
//
//        var canDispose = false
//        var isAnimating = false
//        val xExtremity = measuredWidth.toLong() + radius
//
//        var x = AtomicLong(x ?: xExtremity)
//
//        val idTextBox = Rect()
//
//        init {
//            Paints.textPaint.getTextBounds(id.toString(), 0, id.toString().length, idTextBox)
//        }
//
//        fun startAnim() {
//            isAnimating = true
//            Observable.interval(timeWithDistance.value, TimeUnit.NANOSECONDS)
//                    .subscribeOn(newThread())
//                    .filter { shouldContinue }
//                    .map { 0.001 }
//                    .scan(Double::plus)
//                    .map { 1 - it }
//                    .takeWhile { it > 0 }
//                    .onTerminateDetach()
//                    .map { (xExtremity) * it - (radius / 2) }
//                    .map(Double::toLong)
//                    .subscribe(
//                            {
//                                x.set(it)
//                                Subjects.refresh.onNext(Unit)
//                            },
//                            Throwable::printStackTrace
//                            ,
//                            {
//                                //                                warn("CAN DISPOSE !")
//                                canDispose = true
//                                isAnimating = false
//                                score.incrementAndGet()
//                                ballDisposalSubject.onNext(id)
//                            })
//                    .addToDisposables()
//        }
//
//        override fun render(canvas: Canvas) {
//            canvas.drawCircle(this.x.get().toFloat(), this.y.toFloat(), radius.toFloat(), Paints.circlePaint)
//            canvas.drawText(currentBalls.indexOf(this@OldBall).toString(), this.x.get().toFloat(), this.y.toFloat() + this.idTextBox.height() / 2, Paints.textPaint)
//            if (!this.isAnimating && !this.canDispose)
//                this.startAnim()
//        }
//
//        fun isIntersecting(another: OldBall): Boolean {
//            val distanceXS = Math.pow((x.get() - another.x.get()).toDouble(), 2.0)
//            val distanceYS = Math.pow((y - another.y).toDouble(), 2.0)
//            val radiiS = Math.pow(((radius + strokeWidth / 2) + (another.radius + another.strokeWidth / 2)).toDouble(), 2.0)
//            return distanceXS + distanceYS <= (radiiS)
//        }
//    }
    
    val randY: Int get() = (measuredHeight * Math.random()).toInt()
    
    val refresher: Flowable<Unit>
    
    val currentBalls = AtomicArray<AnimatableBall?>(45)
    
    private val refreshFlowableObserver = object : DisposableSubscriber<Unit>() {
        fun requestMore(x: Int) = request(x.toLong())
        
        override fun onStart() {
            super.onStart()
            request(1)
        }
        
        override fun onNext(t: Unit) {
            invalidate()
        }
        
        override fun onComplete() {
        }
        
        override fun onError(t: Throwable) {
        }
    }
    
    init {
        ballsObservable
                .subscribe { Subjects.refresh.onNext(Unit) }
                .addToDisposables()
        
        refresher = Subjects.refresh
                .subscribeOn(newThread())
                .sample(1, TimeUnit.MILLISECONDS)
                .toFlowable(BackpressureStrategy.DROP)
                .onTerminateDetach()
                .takeUntil(
                        userBallOverlapSubject
                                .toFlowable(BackpressureStrategy.MISSING)
                                .filter { it }
                )
        
        refresher
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(refreshFlowableObserver)
                .addToDisposables()
        
        val timer = Observable.interval(250, TimeUnit.MILLISECONDS)
                .filter { shouldContinue }
                .map(Long::toInt)
        
        val bounds by lazy { Rect(0, 0, measuredWidth, measuredHeight) }
        timer
                .observeOn(computation())
                .map { it + 2 }
                .subscribe({
                    ballsObservable.onNext(
                            AnimatableBall(
                                    it,
                                    PointF(measuredWidth.toFloat(), randY.toFloat()),
                                    radius = dip(25).toFloat(),
                                    strokeWidth = defaultStrokeWidth.toFloat(),
                                    animationField = bounds,
                                    context = context
                            )
                    )
                }, Throwable::printStackTrace)
                .addToDisposables()
        
        ballDisposalSubject
                .observeOn(newThread())
                .flatMapIterable { ballId ->
                    (0 until currentBalls.length())
                            .filter { i -> currentBalls[i]?.id ?: 0 == ballId }
                }
                .subscribe {
                    currentBalls.set(it, null)
                    score.incrementAndGet()
                    System.gc()
                }
                .addToDisposables()
        
        ballsObservable
                .subscribeOn(newThread())
                .observeOn(newThread())
                .subscribe { ball ->
                    val nearestNullIndex = nearestNullIndex()
                    nearestNullIndex.let {
                        currentBalls.set(it, ball)
                    }
                    val disposable = userBallOverlapSubject
                            .filter { it }
                            .subscribe { ball.stop() }
                    ball.doOnDispose {
                        ballDisposalSubject.onNext(ball.id)
                        disposable.dispose()
                    }
                    ball.doOnNext { Subjects.refresh.onNext(Unit) }
                    ball.start()
                }
                .addToDisposables()
        
        Observable.interval(1, TimeUnit.MILLISECONDS)
//                .delay(1, TimeUnit.MILLISECONDS)
                .filter { !(userBallOverlapSubject.value ?: false) }
                .subscribeOn(computation())
                .subscribe {
                    val overlap = currentBalls
                            .indexOfFirst {
                                it?.isIntersecting(userBall) ?: false
                            } != -1
                    userBallOverlapSubject.onNext(overlap)
                }
                .addToDisposables()
        
        userBallOverlapSubject
                .observeOn(single())
                .filter { it }
                .subscribe { userBall.ballPaint.value.set(Paints.overlapPaint) }
                .addToDisposables()
        
    }
    
    fun nearestNullIndex(): Int {
        val index = (0 until currentBalls.length())
                .filter { currentBalls[it] == null }
                .firstOrNull() ?: 0
        
        return index
    }
    
    val bottomLeftText: Renderable = rederable { canvas ->
        canvas.drawText("Score : ${score.get()}", 50f, measuredHeight.toFloat() - 50, Paints.bottomLeftText)
    }
    
    private val pointsManager = object {
        private var index = 0
            @Synchronized get
            @Synchronized set
        
        private val arbitraryPoints = Array(20) { PointF() }
        
        @Synchronized
        fun getAndMoveToNext(): PointF {
            val p = arbitraryPoints[index]
            index++
            if (index == arbitraryPoints.size) index = 0
            return p
        }
        
        @Synchronized
        fun setAndGet(x: Float, y: Float) =
                getAndMoveToNext().also {
                    it.x = x
                    it.y = y
                }
    }
    
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        Subjects.userPoint.onNext(pointsManager.setAndGet(event.x, event.y))
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            (0 until currentBalls.length())
                    .map { currentBalls[it] }
                    .filter { it != null }
                    .forEach {
                        it?.render(canvas)
                    }
            userBall.render(canvas)
//            drawCircle(userPoint.x, userPoint.y, userBallRadius.toFloat(), Paints.userCirclePaint)
            bottomLeftText.render(canvas)
        }
        refreshFlowableObserver.requestMore(1)
    }
    
    override fun getLayerType(): Int {
        return LAYER_TYPE_HARDWARE
    }
    
}