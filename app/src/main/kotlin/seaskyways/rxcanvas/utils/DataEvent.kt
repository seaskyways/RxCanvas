package seaskyways.rxcanvas.utils

/**
 * Created by Ahmad on 03/02 Feb/2017.
 */
data class DataEvent<out T>(val data: T, val eventTag: String)

object EventTag{
    const val DELETE = "del"
    const val ADD = "add"
    const val CHANGE = "chn"
}