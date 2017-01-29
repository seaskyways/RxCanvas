package seaskyways.rxcanvas

import io.reactivex.Scheduler
import java.util.*

/**
 * Created by Ahmad on 29/01 Jan/2017.
 */
class SchedulerPool {
    val schedulersMap = WeakHashMap<Int, Scheduler>()
    val finderMap = mutableMapOf<Int, String>()
    private var schedulerIndex = 0
    
    fun shutdownByTag(tag : String){
        val keysFromFinder = mutableListOf<Int>()
        for ((i , mapTag) in finderMap){
            if (mapTag == tag){
                schedulersMap[i]?.shutdown()
                schedulersMap.remove(i)
                keysFromFinder.add(i)
            }
        }
        keysFromFinder.forEach { finderMap.remove(it) }
    }
    
    fun shutdownAll(){
        schedulersMap.forEach { it.value.shutdown() }
        schedulersMap.clear()
        finderMap.clear()
    }
    
    fun addAndGet(tag : String , sch : Scheduler): Scheduler {
        schedulersMap[schedulerIndex] = sch
        finderMap[schedulerIndex] = tag
        schedulerIndex++
        return sch
    }
}