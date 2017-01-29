package seaskyways.rxcanvas

import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Created by Ahmad on 21/01 Jan/2017.
 */
class AtomicArray<T> : AtomicReferenceArray<T>, Iterable<T> {
    
    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var x = 1
            override fun hasNext(): Boolean {
                return length() != x
            }
            
            override fun next(): T {
                return get(x++ - 1)
            }
        }
    }
    
    constructor(length: Int) : super(length)
    constructor(array: Array<T>) : super(array)
    
    
}