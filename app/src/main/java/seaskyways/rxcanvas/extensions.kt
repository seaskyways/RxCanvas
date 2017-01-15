package seaskyways.rxcanvas

/**
 * Created by Ahmad on 15/01 Jan/2017.
 */
inline fun <reified T : Any> T.className(): String
        = T::class.java.simpleName