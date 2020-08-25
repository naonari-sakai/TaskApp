package jp.techacademy.sakai.naonari.taskapp

import androidx.appcompat.app.AppCompatActivity
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable

open class Category:RealmObject(),Serializable {
    var category : String = ""

    @PrimaryKey
    var id:Int = 0
}