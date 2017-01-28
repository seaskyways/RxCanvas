package seaskyways.rxcanvas.circle

import android.graphics.PointF

/**
 * Created by Ahmad on 28/01 Jan/2017.
 */
data class Circle(val center : PointF, val radius : Float, val id : Int? = null){
    fun contains(x : Number, y : Number) : Boolean{
        val dx = center.x - x.toFloat()
        val dy = center.y - y.toFloat()
        return dx <= radius && dy <= radius
    }
}