package seaskyways.rxcanvas.circle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.newThread
import io.reactivex.schedulers.Schedulers.single
import io.reactivex.subjects.*
import io.reactivex.subscribers.DisposableSubscriber
import org.jetbrains.anko.*
import seaskyways.rxcanvas.*
import java.util.*
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
        val circlesPositionSubject = PublishSubject.create<Circle>()
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
    
    init {
        backgroundColor = Color.WHITE
    }
    
    override fun isDisposed() = disposables.isDisposed
    
    override fun dispose() {
        Subjects.circlesPositionSubject.onComplete()
        Subjects.refresh.onComplete()
        Subjects.userPoint.onComplete()
        userBallOverlapSubject.onComplete()
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
    
    init {
        val userVelocityRefreshRate = 10L
        val sampledUserPoint = Subjects.userPoint
                .subscribeOn(Schedulers.computation())
                .toFlowable(BackpressureStrategy.DROP)
                .sample(userVelocityRefreshRate, TimeUnit.MILLISECONDS)
                .onTerminateDetach()
        
        sampledUserPoint
                .subscribeWith(object : DisposableSubscriber<PointF>() {
                    override fun onError(t: Throwable?) = Unit
                    override fun onComplete() = Unit
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
    
//    var lifecycleObservable: Observable<ActivityEvent>? = null
//        set(value) {
//            field = value
//            value?.subscribe { currLifecycle = it }
//        }
//
//    var currLifecycle: ActivityEvent = ActivityEvent.PAUSE

//    inner class OldBall(val id: Int = 1, val y: Int, val radius: Long = dip(25).toLong(), x: Long? = null, val strokeWidth: Int = defaultStrokeWidth) : Renderable {
//        val idTextBox = Rect()
//
//        init {
//            Paints.textPaint.getTextBounds(id.toString(), 0, id.toString().length, idTextBox)
//        }
//
//        override fun render(canvas: Canvas) {
//            canvas.drawCircle(this.x.get().toFloat(), this.y.toFloat(), radius.toFloat(), Paints.circlePaint)
//            canvas.drawText(currentBalls.indexOf(this@OldBall).toString(), this.x.get().toFloat(), this.y.toFloat() + this.idTextBox.height() / 2, Paints.textPaint)
//            if (!this.isAnimating && !this.canDispose)
//                this.startAnim()
//        }
//    }
    
    val randY: Int get() = (measuredHeight * Math.random()).toInt()
    
    val refresher: Flowable<Unit>
    
    val currentBalls = AtomicArray<AnimatableBall?>(45)
    
    private val refreshFlowableObserver = object : DisposableSubscriber<Unit>() {
        fun request(x: Number) = request(x.toLong())
        
        override fun onStart() {
            request(2)
        }
        
        override fun onNext(t: Unit) {
            if (!userBallOverlapSubject.values.contains(true))
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
                .takeUntil(
                        userBallOverlapSubject
                                .filter { it }
                )
                .toFlowable(BackpressureStrategy.LATEST)
//                .sample(15, TimeUnit.MILLISECONDS)
                .onTerminateDetach()
        
        refresher
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(refreshFlowableObserver)
                .addToDisposables()
        
        val timer = Observable
                .defer { Observable.timer(250, TimeUnit.MILLISECONDS) }
                .repeatUntil { userBallOverlapSubject.value ?: false }
                .subscribeOn(newThread())
                .map { 1 }
                .scan(Int::plus)
                .filter { shouldContinue }
        
        val bounds by lazy { Rect(0, 0, measuredWidth, measuredHeight) }
        timer
                .observeOn(newThread())
                .map {
                    AnimatableBall(
                            id = it,
                            center = PointF(measuredWidth.toFloat(), randY.toFloat()),
                            radius = dip(25).toFloat(),
                            strokeWidth = defaultStrokeWidth.toFloat(),
                            animationField = bounds,
                            context = context,
                            isRtl = false
                    )
                }
                .doOnNext { it.setCurrentPositionSubject(Subjects.circlesPositionSubject) }
                .subscribe(ballsObservable::onNext, Throwable::printStackTrace)
                .addToDisposables()
        
        ballDisposalSubject
                .observeOn(newThread())
                .map { ballId ->
                    currentBalls.indexOfFirst { it?.id == ballId }
                }
                .filter { it != -1 }
                .subscribe {
                    currentBalls.set(it, null)
                    score.incrementAndGet()
                }
                .addToDisposables()
        
        ballsObservable
                .subscribeOn(newThread())
                .observeOn(newThread())
                .subscribe { ball ->
                    val nearestNullIndex = findNearestNullIndex()
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

//        val renderScript = RenderScript.create(context)
        Subjects.circlesPositionSubject
                .subscribeOn(newThread())
                .observeOn(newThread())
                .flatMap { (_, _, circleId): Circle ->
                    Observable
                            .create<Ball> { emitter ->
                                val ball = currentBalls.firstOrNull { ball -> ball?.id == circleId ?: false }
                                ball?.let { emitter.onNext(it) }
                                emitter.onComplete()
                            }
                            .subscribeOn(Schedulers.computation())
                            .flatMapSingle {
                                Single.fromCallable { it.isIntersecting(userBall) }
                                        .subscribeOn(Schedulers.computation())
                            }
                            .filter { it }
                    
                }
                .filter { !(userBallOverlapSubject.value ?: false) }
                .onTerminateDetach()
                .observeOn(newThread())
                .subscribe { userBallOverlapSubject.onNext(it) }
                .addToDisposables()
        
        userBallOverlapSubject
                .observeOn(single())
                .filter { it }
                .subscribe { userBall.ballPaint.value.set(Paints.overlapPaint) }
                .addToDisposables()
        
    }
    
    fun findNearestNullIndex(): Int {
        var index = currentBalls.indexOfFirst { it == null }
        if (index == -1) index = 0
        return index
    }
    
    val bottomLeftText: Renderable = rederable { canvas ->
        canvas.drawText("Score : ${score.get()}", 50f, measuredHeight.toFloat() - 50, Paints.bottomLeftText)
    }
    
    private val pointsManager = object {
        private var index = 0
            @Synchronized get
            @Synchronized set
        
        private val arbitraryPoints = Array(5) { PointF() }
        
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
        
        @Synchronized
        fun getLastPoints(): List<PointF> = arbitraryPoints.toList()
        
        @Synchronized
        fun sendTouchEvent(event: MotionEvent) {
            Subjects.userPoint.onNext(setAndGet(event.x, event.y))
        }
    }
    
    var canMove = false
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                if (!canMove) return true
                canMove = false
                Observable.create<Unit> { emitter ->
                    for (i in 0..2) {
                        if (canMove) {
                            emitter.onComplete()
                            return@create
                        }
                        emitter.onNext(Unit)
                        Thread.sleep(5)
                    }
                    emitter.onComplete()
                }
                        .subscribeOn(Schedulers.single())
                        .map { event }
                        .subscribe(pointsManager::sendTouchEvent)
            }
            else -> {
                if (userBall.asCircle.contains(event.x, event.y))
                    canMove = true
            }
        }
        if (canMove) {
            pointsManager.sendTouchEvent(event)
        }
        return true
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        currentBalls.forEach { it?.render(canvas) }
        userBall.render(canvas)
        bottomLeftText.render(canvas)
        refreshFlowableObserver.request(2)
    }
//
//    override fun getLayerType(): Int {
//        return LAYER_TYPE_HARDWARE
//    }
    
}