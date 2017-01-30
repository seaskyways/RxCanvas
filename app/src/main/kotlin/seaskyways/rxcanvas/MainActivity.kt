package seaskyways.rxcanvas

import android.os.Bundle
import android.widget.FrameLayout
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.RxActivity
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.jetbrains.anko.alert
import org.jetbrains.anko.ctx
import org.jetbrains.anko.find
import org.jetbrains.anko.matchParent
import seaskyways.rxcanvas.R
import seaskyways.rxcanvas.circle.CircleView

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
                        alert(message = msg, title = "Restart Game ?") {
                            okButton {
                                circleView.dispose()
                                circleView.shouldContinue = true
                                parentView.removeView(circleView)
                                startLogic(parentView)
                            }
                            show()
                        }
                    }
                    
                    override fun onComplete() = Unit
                })
    }
}