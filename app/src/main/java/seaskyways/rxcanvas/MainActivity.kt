package seaskyways.rxcanvas

import android.os.Bundle
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.RxActivity
import org.jetbrains.anko.find

class MainActivity : RxActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}
