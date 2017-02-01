package seaskyways.rxcanvas.model.database

import io.realm.RealmObject
import io.realm.annotations.*
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Ahmad on 01/02 Feb/2017.
 */
@Suppress("LeakingThis")
open class GameScore() : RealmObject(){
    constructor(id : Int , score : Int , image : ByteArray) : this(){
        this.id = id
        this.score = score
        this.gameImage = image
    }
    
    @PrimaryKey
    open var id : Int = 0
    
    open var score : Int = 0
    @Required lateinit open var gameDate : Date
    @Required lateinit open var gameImage : ByteArray
}