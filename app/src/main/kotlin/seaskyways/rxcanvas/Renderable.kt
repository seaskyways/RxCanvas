package seaskyways.rxcanvas

import android.graphics.Canvas

/**
 * Created by Ahmad on 21/01 Jan/2017.
 */
interface Renderable {
    fun render(canvas: Canvas)
}

fun rederable(r: (Canvas) -> Unit): Renderable = object : Renderable {
    override fun render(canvas: Canvas) {
        r(canvas)
    }
}