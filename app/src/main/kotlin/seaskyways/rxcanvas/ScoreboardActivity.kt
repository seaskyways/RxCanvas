package seaskyways.rxcanvas

import android.graphics.*
import android.os.Bundle
import android.support.v7.widget.*
import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.*
import com.vicpin.krealmextensions.*
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import seaskyways.rxcanvas.model.database.GameScore
import seaskyways.rxcanvas.model.view.GameScoreViewModel
import seaskyways.rxcanvas.utils.*

class ScoreboardActivity : BaseActivity() {
    
    companion object{
        
    }
    
    val dataList = mutableListOf<GameScoreViewModel>()
    val dataEventSubject: PublishSubject<DataEvent<GameScore>> = PublishSubject.create<DataEvent<GameScore>>()
    lateinit var disposable: Disposable
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = ScoreboardActivityUI()
        ui.setContentView(this)
        
        val realmList = realm
                .where(GameScore::class.java)
                .findAll()
    
        AnkoDynamicAdapter
                .with<GameScoreViewModel, SingleGameScoreUI>(::SingleGameScoreUI)
                .data(dataList)
                .onAnkoBind { gameScore, singleGameScoreUI, holder, _ ->
                    singleGameScoreUI.apply {
                        scoreTextView.text = "Score : ${gameScore.score}"
                        val formattedDate = DateFormat.format("yyyy/mm/dd hh:mm a", gameScore.date)
                        dateTextView.text = "Date : $formattedDate"
                        doAsync {
                            val bitmap = BitmapFactory.decodeByteArray(gameScore.gameImage, 0, gameScore.gameImage.size)
                            uiThread {
                                it.gameImageView.setImageBitmap(bitmap)
                            }
                        }
                        parent.onLongClick {
                            parent.context.alert("Do you want to delete ?") {
                                okButton {
                                    realm.transaction { realm ->
                                        realmList
                                                .filter { it.id == gameScore.id }
                                                .forEach {
                                                    val copy = realm.copyFromRealm(it)
                                                    it.deleteFromRealm()
                                                    dataEventSubject.onNext(DataEvent(copy, EventTag.DELETE))
                                                }
                                    }
                                }
                                noButton()
                            }.show()
                            true
                        }
                    }
                }
                .into(ui.recycler, LinearLayoutManager(ctx))
        
        GameScore().allItems
                .sortedByDescending { it.score }
                .mapTo(dataList) { GameScoreViewModel(it.id, it.gameDate, it.score, it.gameImage) }
        
        ui.recycler.adapter.notifyDataSetChanged()
        
        disposable = dataEventSubject
                .filter { it.eventTag === EventTag.DELETE }
                .map { it.data }
                .map { gameScore: GameScore ->
                    dataList.indexOfFirst { it.id == gameScore.id }
                }
                .subscribe {
                    dataList.removeAt(it)
                    ui.recycler.adapter.notifyItemRemoved(it)
                }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        dataEventSubject.onComplete()
    }
}

private class ScoreboardActivityUI : AnkoComponent<ScoreboardActivity> {
    lateinit var recycler: RecyclerView
    override fun createView(ui: AnkoContext<ScoreboardActivity>) = with(ui) {
        coordinatorLayout {
            recycler = recyclerView {
                layoutManager = LinearLayoutManager(ctx)
            }.lparams(matchParent, matchParent)
        }
    }
}

private class SingleGameScoreUI : AnkoComponent<ViewGroup> {
    lateinit var dateTextView: TextView
    lateinit var scoreTextView: TextView
    lateinit var gameImageView: ImageView
    lateinit var parent: ViewGroup
    override fun createView(ui: AnkoContext<ViewGroup>) = with(ui) {
        parent = verticalLayout {
            scoreTextView = textView()
            dateTextView = textView()
            gameImageView = imageView {
                backgroundColor = Color.LTGRAY
            }.lparams(width = matchParent, height = dip(250))
        }
        parent
    }
}