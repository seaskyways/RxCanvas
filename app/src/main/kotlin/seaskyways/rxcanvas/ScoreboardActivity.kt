package seaskyways.rxcanvas

import android.os.Bundle
import android.support.v7.widget.*
import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.TextView
import com.vicpin.krealmextensions.allItems
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import seaskyways.rxcanvas.model.database.GameScore
import seaskyways.rxcanvas.model.view.GameScoreViewModel
import seaskyways.rxcanvas.utils.*

class ScoreboardActivity : BaseActivity() {
    
    val dataList = mutableListOf<GameScoreViewModel>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = ScoreboardActivityUI()
        ui.setContentView(this)
        
        AnkoDynamicAdapter
                .with<GameScoreViewModel, SingleGameScoreUI>(ctx) { SingleGameScoreUI() }
                .data(dataList)
                .onBind { gameScoreViewModel: GameScoreViewModel, singleGameScoreUI: SingleGameScoreUI, dynamicViewHolder: DynamicViewHolder, i: Int ->
                    singleGameScoreUI.bindGameScore(gameScoreViewModel)
                }
                .into(ui.recycler)
        
        GameScore().allItems
                .sortedByDescending { it.score }
                .mapTo(dataList) { GameScoreViewModel(it.id, it.gameDate, it.score, it.gameImage) }
        
        ui.recycler.adapter.notifyDataSetChanged()
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
    override fun createView(ui: AnkoContext<ViewGroup>) = with(ui) {
        verticalLayout {
            scoreTextView = textView()
            dateTextView = textView()
        }
    }
    
    fun bindGameScore(gameScore: GameScoreViewModel) {
        scoreTextView.text = "Score : ${gameScore.score}"
        val formattedDate = DateFormat.format("yyyy/mm/dd hh:mm", gameScore.date)
        dateTextView.text = "Date : $formattedDate"
    }
}