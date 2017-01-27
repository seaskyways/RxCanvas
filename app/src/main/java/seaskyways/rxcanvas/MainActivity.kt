package seaskyways.rxcanvas

import android.os.Bundle
import android.widget.FrameLayout
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.RxActivity
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.jetbrains.anko.*
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
        
        circleView.lifecycleObservable = lifecycle()
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
                    override fun onError(t: Throwable?) {
                        
                    }
                    
                    override fun onNext(t: Boolean?) {
                        alert("Restart Game ?") {
                            okButton {
                                circleView.dispose()
                                circleView.shouldContinue = true
                                circleView.userBallOverlapSubject.onComplete()
                                parentView.removeView(circleView)
                                startLogic(parentView)
                                request(1)
                            }
                            noButton()
                            show()
                        }
                    }
                    
                    
                    override fun onComplete() {
                        
                    }
                    
                })
    }
}
