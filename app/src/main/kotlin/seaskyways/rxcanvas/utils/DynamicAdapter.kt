package seaskyways.rxcanvas.utils

/**
 * Created by Ahmad on 01/02 Feb/2017.
 */

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.AnkoComponent
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.layoutInflater
import java.lang.ref.WeakReference

/**
 * Created by Ahmad on 10/11 Nov/2016.
 */
open class DynamicAdapter<T, HOLDER : DynamicViewHolder?> protected constructor(val context: Context) : Adapter<DynamicViewHolder?>() {
    
    companion object {
        fun <T, HOLDER : DynamicViewHolder> with(ctx: Context) = DynamicAdapter<T, HOLDER>(ctx)
        
        fun <T> withDynamic(ctx: Context) = DynamicAdapter<T, DynamicViewHolder?>(ctx)
    }
    
    
    protected var dataList: Collection<T>? = null
    protected var layoutInt: Int? = null
    protected var layoutView: AnkoComponent<ViewGroup>? = null
    
    protected var recyclerViewRef = WeakReference<RecyclerView?>(null)
//    private var recyclerView: RecyclerView? = null
    
    protected var onBindLambda: DynamicAdapter<T, HOLDER>.(T?, View?, DynamicViewHolder?, Int) -> Unit = {
        data: T?, view: View?, holder: DynamicViewHolder?, position: Int ->
    }
    protected var onCreateViewHolderLambda: ((parent: View, viewType: Int) -> HOLDER)? = null
    
    fun overrideViewHolder(onViewHolder: (parent: View, viewType: Int) -> HOLDER): DynamicAdapter<T, HOLDER> {
        onCreateViewHolderLambda = onViewHolder
        return this
    }
    
    open fun data(list: Collection<T>): DynamicAdapter<T, HOLDER> {
        dataList = list
        return this
    }
    
    
    fun layout(layoutRes: Int? = null, view: AnkoComponent<ViewGroup>? = null): DynamicAdapter<T, HOLDER> {
        when {
            layoutRes != null -> layoutInt = layoutRes
            view != null -> layoutView = view
            else -> throw NullPointerException()
        }
        return this
    }
    
    @Suppress("UNCHECKED_CAST")
    open fun onBind(onBind: DynamicAdapter<T, HOLDER>.(data: T, view: View?, holder: HOLDER, position: Int) -> Unit): DynamicAdapter<T, HOLDER> {
        onBindLambda = onBind as DynamicAdapter<T, HOLDER>.(T?, View?, DynamicViewHolder?, Int) -> Unit
        return this
    }
    
    fun into(recycler: RecyclerView, layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)): Unit {
        recyclerViewRef = WeakReference(recycler)
        recycler.adapter = this
        recycler.layoutManager = layoutManager
    }
    
    override fun getItemCount(): Int {
        return dataList?.size ?: 0
    }
    
    override fun onBindViewHolder(holder: DynamicViewHolder?, position: Int) {
        onBindLambda(dataList?.elementAt(position), holder?.itemView, holder, position)
    }
    
    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DynamicViewHolder? {
        if (onCreateViewHolderLambda != null) {
            return onCreateViewHolderLambda?.invoke(layoutView!!.createView(AnkoContext.Companion.create(parent.context, parent)), viewType)!!
        } else {
            when {
                layoutInt != null -> return BasicViewHolder(context.layoutInflater.inflate(layoutInt!!, parent, false))
                layoutView != null -> return BasicViewHolder(layoutView!!.createView(AnkoContext.Companion.create(parent.context, parent)))
            }
        }
        return null
    }
    
    inner class BasicViewHolder(itemView: View?) : DynamicViewHolder(itemView)
}

abstract class DynamicViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView)

class AnkoDynamicAdapter<T, A : AnkoComponent<ViewGroup>> private constructor(val ctx: Context) : DynamicAdapter<T, AnkoDynamicAdapter<T, A>.AnkoViewHolder?>(ctx), AnkoLogger {
    
    companion object {
        fun <T, A : AnkoComponent<ViewGroup>> with(ctx: Context, ankoComponentGenerator: () -> A) = AnkoDynamicAdapter<T, A>(ctx).apply {
            ankoGenerator = ankoComponentGenerator
        }
    }
    
    inner class AnkoViewHolder(val itemView: View, val ankoComponent: A) : DynamicViewHolder(itemView)
    
    var ankoGenerator: (() -> A)? = null
    var _onBind: (AnkoDynamicAdapter<T, A>.(T, A, DynamicViewHolder, Int) -> Unit)? = null
    
    fun onBind(__onBind: AnkoDynamicAdapter<T, A>.(T, A, DynamicViewHolder, Int) -> Unit) = apply { _onBind = __onBind }
    
    override fun data(list: Collection<T>): AnkoDynamicAdapter<T, A> {
        super.data(list)
        return this
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnkoViewHolder? {
        ankoGenerator?.let { ankoGenerator ->
            val anko = ankoGenerator()
            val holder = AnkoViewHolder(anko.createView(AnkoContext.Companion.create(parent.context, parent)), anko)
            return holder
        }
        throw NullPointerException("Anko generator isn't set")
    }
    
    override fun onBindViewHolder(holder: DynamicViewHolder?, position: Int) {
        val data = try {
            dataList?.elementAt(position)
        } catch (e: IndexOutOfBoundsException) {
            null
        }
        @Suppress("UNCHECKED_CAST")
        val aholder = holder!! as AnkoDynamicAdapter<T, A>.AnkoViewHolder
        if (data != null)
            _onBind?.invoke(this, data, aholder.ankoComponent, aholder, position)
    }
}