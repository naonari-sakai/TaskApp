package jp.techacademy.sakai.naonari.taskapp

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.realm.Realm
import kotlinx.android.synthetic.main.content_input.*
import kotlinx.android.synthetic.main.cotegorysetting.*

class CategoryActivity : AppCompatActivity(){
    private var mCategoryString = ""
    private var mCategory:Category? = null

    private val mOnAddClickListener = View.OnClickListener {

        if (category_edit_text.text.isNotBlank()) {

            val realm = Realm.getDefaultInstance()
            mCategory = Category()
            realm.beginTransaction()

            val taskRealmResult = realm.where(Category::class.java).findAll()

            val identifier: Int =
                if (taskRealmResult.max("id") != null) {
                    taskRealmResult.max("id")!!.toInt() + 1
                } else {
                    0
                }
            mCategory!!.id = identifier

            mCategoryString = category_edit_text.text.toString()
            mCategory!!.category = mCategoryString

            realm.copyToRealmOrUpdate(mCategory!!)
            realm.commitTransaction()

            realm.close()
            val view: View = findViewById(android.R.id.content);
            Snackbar.make(view, "カテゴリーに追加しました。", Snackbar.LENGTH_INDEFINITE)
                .show()

            category_edit_text.setText("")
        }
    }

    private val mOnDeleteClickListener = View.OnClickListener {

        if (category_edit_text.text.isNotBlank()) {
            mCategoryString = category_edit_text.text.toString()
            val realm = Realm.getDefaultInstance()
            val results =
                realm.where(Category::class.java).equalTo("category", mCategoryString).findAll()

            if (results != null) {
                realm.beginTransaction()
                results.deleteAllFromRealm()
                realm.commitTransaction()

                val view: View = findViewById(android.R.id.content);
                Snackbar.make(view, "カテゴリーから削除しました。", Snackbar.LENGTH_INDEFINITE)
                    .show()
            }

            realm.close()

            category_edit_text.setText("")
        }
    }

    private val mOnFinishButton = View.OnClickListener {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cotegorysetting)

        button2.setOnClickListener(mOnAddClickListener)
        deletebutton.setOnClickListener(mOnDeleteClickListener)
        finishadd.setOnClickListener(mOnFinishButton)

    }


}