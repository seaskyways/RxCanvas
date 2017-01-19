package seaskyways.rxcanvas

import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }
    
    @Test
    @Throws(Exception::class)
    fun takeWhileTrial() {
        Observable.interval(1, TimeUnit.NANOSECONDS)
                .map { 0.0001 }
                .scan(Double::plus)
                .takeWhile { it < 1 }
                .subscribe({ }, {}, {
                    println("Reached the thing")
                })
        readLine()
    }
}