package seaskyways.rxcanvas

import android.os.Bundle
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.RxActivity
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.jetbrains.anko.alert
import org.jetbrains.anko.find

class MainActivity : RxActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startLogic()
        
    }
    
    fun startLogic() {
        setContentView(R.layout.activity_main)
        
        val circleView = find<CircleView>(R.id.circle_view)
        
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
                                circleView.shouldContinue = true
                                circleView.userBallOverlapSubject.onComplete()
                                startLogic()
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
