package seaskyways.rxcanvas

import android.app.Activity
import android.os.Bundle
import com.trello.rxlifecycle2.components.RxActivity
import io.reactivex.Observable

class MainActivity : RxActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    
        (findViewById(R.id.circle_view) as CircleView).lifecycleObservable = lifecycle()
        
        
    }
}
