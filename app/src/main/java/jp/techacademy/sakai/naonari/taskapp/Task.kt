package jp.techacademy.sakai.naonari.taskapp

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.*

open class Task : RealmObject(), Serializable {
    //Realmが内部的にTaskを継承したクラスを作成するためopen
    var title: String = ""
    var contents: String = ""
    var date: Date = Date()
    var category: String = ""

    //idをプライマリーキーとして設定
    @PrimaryKey
    var id: Int = 0
}