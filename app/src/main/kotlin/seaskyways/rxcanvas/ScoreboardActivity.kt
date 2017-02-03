package seaskyways.rxcanvas

import android.graphics.*
import android.os.Bundle
import android.support.v7.widget.*
import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.*
import com.vicpin.krealmextensions.*
import io.realm.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import seaskyways.rxcanvas.model.database.GameScore
import seaskyways.rxcanvas.model.view.GameScoreViewModel
import seaskyways.rxcanvas.utils.AnkoDynamicAdapter

class ScoreboardActivity : BaseActivity() {
    
    val dataList = mutableListOf<GameScoreViewModel>()
    lateinit var realmListener: RealmChangeListener<RealmResults<GameScore>>
    lateinit var realmList: RealmResults<GameScore>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = ScoreboardActivityUI()
        ui.setContentView(this)
        
        AnkoDynamicAdapter
                .with<GameScoreViewModel, SingleGameScoreUI>(::SingleGameScoreUI)
                .data(dataList)
                .onAnkoBind { gameScoreViewModel, singleGameScoreUI, holder, _ ->
                    singleGameScoreUI.bindGameScore(gameScoreViewModel, holder, realmList)
                }
                .into(ui.recycler, LinearLayoutManager(ctx))
        
        GameScore().allItems
                .sortedByDescending { it.score }
                .mapTo(dataList) { GameScoreViewModel(it.id, it.gameDate, it.score, it.gameImage) }
        
        ui.recycler.adapter.notifyDataSetChanged()
        
        realmListener = RealmChangeListener<RealmResults<GameScore>> { realm ->
            realm.sortedByDescending { it.score }
                    .mapTo(dataList) { GameScoreViewModel(it.id, it.gameDate, it.score, it.gameImage) }
            ui.recycler.adapter.notifyDataSetChanged()
        }
        
        realmList = realm
                .where(GameScore::class.java)
                .findAll()
        
        realmList.addChangeListener(realmListener)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        realm.removeAllChangeListeners()
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
    
    fun bindGameScore(gameScore: GameScoreViewModel, viewHolder: RecyclerView.ViewHolder, realmList: RealmResults<GameScore>) {
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
                    realm.transaction {
                        realmList
                                .filter { it.id == gameScore.id }
                                .forEach { it.deleteFromRealm() }
                    }
                }
                noButton()
            }.show()
            true
        }
    }
}