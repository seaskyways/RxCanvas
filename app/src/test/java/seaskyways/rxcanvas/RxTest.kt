package seaskyways.rxcanvas

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers.*
import org.junit.Test
import java.lang.System.nanoTime
import java.lang.Thread.currentThread
import java.lang.Thread.sleep

/**
 * Created by User1 on 23/01 - Jan/17.
 */
class RxTest {
    
    @Test
    @Throws
    fun groupByTest() {
//
//        Observable.create<Int> { emitter ->
//            for (i in 0..100) {
//                emitter.onNext((Math.random() * i * 100).toInt())
////                Thread.sleep(50)
//            }
//            emitter.onComplete()
//        }
//                .subscribeOn(computation())
//                .groupBy { it.compareTo(1000) }
//                .flatMapSingle(GroupedObservable<Int, Int>::toList)
//                .doOnNext { it.sort() }
//                .observeOn(trampoline())
//                .subscribe(::println)
//
//        sleep(1000)
    }
    /*
    * [0, 20, 37, 46, 67, 108, 148, 152, 185, 203, 211, 257, 276, 308, 351, 380, 432, 439, 531, 550, 565, 576, 652, 680, 736, 767, 772, 802, 811, 865, 891, 897, 951, 961, 995]
    * [1000]
    * [1070, 1125, 1242, 1263, 1264, 1414, 1487, 1564, 1606, 1655, 1694, 1723, 1900, 1920, 1951, 1956, 1978, 2022, 2042, 2159, 2358, 2392, 2552, 2562, 2620, 2822, 2890, 2934, 2942, 3034, 3119, 3180, 3334, 3373, 3398, 3806, 3826, 3988, 4107, 4202, 4237, 4304, 4329, 4458, 4545, 4608, 4776, 4782, 4956, 4989, 5004, 5361, 5385, 5597, 5626, 6199, 6799, 6824, 6938, 7624, 8016, 8545, 8596, 8843, 9424]
    * Process finished with exit code 0
    * */
    
    @Test
    @Throws
    fun schedulersTest(){
        fun printThread() = println("Thread name : ${currentThread().name}")
        printThread()
        Observable.just(1)
                .doOnNext { printThread() }
                .subscribeOn(newThread())
                .doOnNext { printThread() }
                .observeOn(computation())
                .doOnNext { printThread() }
                .observeOn(io())
                .doOnNext { printThread() }
                .subscribe()
        
        sleep(1000)
    }
    
    @Test
    fun concurrencyTest() {
        val startTime = nanoTime()
        Observable.range(0 , 10)
                .map { object {
                    val id = it
                    val time = it * Math.random() * 100
                } }
                .flatMap {
                    Observable.just(it)
                            .subscribeOn(computation())
                            .doOnNext { Thread.sleep(it.time.toLong()) }
                }
                .observeOn(trampoline())
                .blockingSubscribe({
                    println("Just received ${it.id} of time ${it.time}")
                })
                
        println("Ended with at ${nanoTime() - startTime}")
    }
}