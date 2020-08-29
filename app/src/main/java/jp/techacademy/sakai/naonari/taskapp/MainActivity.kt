package jp.techacademy.sakai.naonari.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_input.*
import java.util.*

const val EXTRA_TASK = "jp.techacademy.sakai.naonari.taskapp"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private lateinit var mRealmCotegory: Realm
    lateinit var mCategoryobject: MutableList<Category>
    private var spinnerItems:MutableList<String> = mutableListOf<String>()
    private val mRealmListener = object  : RealmChangeListener<Realm>{
        override fun onChange(element: Realm) {
            reloadListView()
            reloadListViewCategory()
        }
    }

    private  lateinit var  mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity,InputActivity::class.java)
            startActivity(intent)
        }



        //Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        //Realmの設定（カテゴリー用）
        mRealmCotegory = Realm.getDefaultInstance()
        mRealmCotegory.addChangeListener(mRealmListener)

        reloadListViewCategory()


        //spinnerの設定
        val adapter = ArrayAdapter(applicationContext,
            R.layout.custom_spinner, spinnerItems)

        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)

        search_spinner.adapter = adapter

        search_spinner.setFocusable(false)

        search_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            //アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                if (search_spinner.isFocusable() == false) {
                    search_spinner.setFocusable(true);
                    return;
                }else {
                    val spinnerParent = parent as Spinner
                    val item: String = spinnerParent.selectedItem as String

                    val RealmQuery = mRealm.where(Task::class.java)
                    val RealmQueryResult =
                        RealmQuery.equalTo("category", "$item").findAll()
                            .sort("date", Sort.DESCENDING)

                    mTaskAdapter.taskList = mRealm.copyFromRealm(RealmQueryResult)


                    // TaskのListView用のアダプタに渡す
                    listView1.adapter = mTaskAdapter

                    // 表示を更新するために、アダプターにデータが変更されたことを知らせる
                    mTaskAdapter.notifyDataSetChanged()

                    Log.d("TaskApp", "test")
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.d("TaskApp","miss")

            }

        }

        cancelButton.setOnClickListener {
            reloadListView()
        }

        //ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //ListViewをタップした時の処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            //入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity,InputActivity::class.java)
            intent.putExtra(EXTRA_TASK,task.id)
            startActivity(intent)
        }

        //ListViewを長押しした時の処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            //タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            //ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title+"を削除しますか")

            builder.setPositiveButton("OK"){_,_ ->
                val results = mRealm.where(Task::class.java).equalTo("id",task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext,TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }


        reloadListView()
    }

    override fun onResume() {
        reloadListView()
        super.onResume()
            }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter


        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()

    }

    private fun reloadListViewCategory() {

        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得(カテゴリー)
        val taskRealmResultsCategory =
            mRealmCotegory.where(Category::class.java).findAll().sort("id", Sort.DESCENDING)

        // 上記の結果を、TaskList としてセットする
        mCategoryobject = mRealmCotegory.copyFromRealm(taskRealmResultsCategory)

        spinnerItems.clear()
        var categoryString: String
        for (i in mCategoryobject.indices) {
            categoryString = mCategoryobject[i].category
            spinnerItems.add(categoryString)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
        mRealmCotegory.close()
    }


}