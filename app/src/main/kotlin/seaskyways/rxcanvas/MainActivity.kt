package seaskyways.rxcanvas

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.FrameLayout
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.RxActivity
import com.vicpin.krealmextensions.*
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.jetbrains.anko.*
import seaskyways.rxcanvas.circle.CircleView
import seaskyways.rxcanvas.model.database.GameScore
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : RxActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val frame: FrameLayout = find(R.id.activity_main)
        
        startLogic(frame, true)
        
    }
    
    fun startLogic(parentView: FrameLayout, isOnCreate: Boolean = false) {
        val circleView = CircleView(ctx)
        parentView.addView(circleView, matchParent, matchParent)

//        circleView.lifecycleObservable = lifecycle()
        lifecycle()
                .subscribe {
                    circleView.shouldContinue = it == ActivityEvent.RESUME
                }
        
        circleView.userBallOverlapSubject
                .filter { it }
                .subscribe {
                    circleView.shouldContinue = false
                }
        
        circleView.userBallOverlapSubject
                .filter { it }
                .toFlowable(BackpressureStrategy.DROP)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposableSubscriber<Boolean>() {
                    override fun onStart() {
                        request(1)
                    }
                    
                    override fun onError(t: Throwable?) = Unit
                    override fun onNext(t: Boolean?) {
                        val msg = "You avOided ${circleView.score.get()} Balls !"
                        val future = saveGame(circleView)
                        alert(message = msg, title = "Restart Game ?") {
                            okButton {
                                future.get()
                                circleView.dispose()
                                circleView.shouldContinue = true
                                parentView.removeView(circleView)
                                startLogic(parentView)
                            }
                            negativeButton(negativeText = "Scoreboard"){
                                startActivity(intentFor<ScoreboardActivity>())
                            }
                            show()
                        }
                    }
                    
                    override fun onComplete() = Unit
                })
    }
    
    fun saveGame(circleView: CircleView) = run {
        circleView.isDrawingCacheEnabled = true
        circleView.buildDrawingCache()
        val imageOfCircleView: Bitmap = circleView.drawingCache
        doAsync {
            val byteArrayOutputStream = ByteArrayOutputStream()
            imageOfCircleView.compress(Bitmap.CompressFormat.WEBP, 100, byteArrayOutputStream)
            val gameImageAsByteArray = byteArrayOutputStream.toByteArray()
            
            val gameRecord = GameScore()
            val lastId = GameScore().lastItem?.id ?: 0
            gameRecord.id = lastId + 1
            gameRecord.gameDate = Date()
            gameRecord.score = circleView.score.get()
            gameRecord.gameImage = gameImageAsByteArray
            gameRecord.save()
        }
    }
}
