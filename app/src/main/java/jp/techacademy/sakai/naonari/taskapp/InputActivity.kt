package jp.techacademy.sakai.naonari.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.content_input.*
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class InputActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListener = object  : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0

    private var mCategory = "未設定"
    lateinit var mCategoryobject: MutableList<Category>
    private var mTask: Task? = null

    private var spinnerItems = mutableListOf<String>()



    private val mOnDateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(this,
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            mYear = year
            mMonth = month
            Log.d("month","${month}")
            mDay = dayOfMonth
            val dateString = mYear.toString() + "/" + String.format("%02d",mMonth + 1) + "/" + String.format("%02d", mDay)
            date_button.text = dateString
        },mYear,mMonth,mDay)
        datePickerDialog.show()
    }

    private val mOnTimeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(this,
        TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            mHour = hour
            mMinute = minute
            val timeString = String.format("%02d",mHour)+":"+String.format("%02d",mMinute)
            times_button.text=timeString
            },mHour,mMinute,false)
        timePickerDialog.show()
    }

    private val mOnDoneClickListener = View.OnClickListener {
        addTask()
        finish()
    }

    private val mOnCategorySetClickListener = View.OnClickListener {
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)


        //ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (supportActionBar != null){
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)


        //UI部品の設定
        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimeClickListener)
        done_button.setOnClickListener(mOnDoneClickListener)
        category_add_button.setOnClickListener(mOnCategorySetClickListener)

        //spinnerの設定
        val adapter = ArrayAdapter(applicationContext,
            android.R.layout.simple_spinner_item, spinnerItems)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner2.adapter = adapter

        spinner2.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{

            //アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                val item:String = spinnerParent.selectedItem as String
                mCategory = item
                Log.d("TaskApp","test")
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.d("TaskApp","miss")

            }

        }

        //EXTRA_TASKからTaskのidを取得して、idからTaskのインスタンスを取得する
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)
        val realm = Realm.getDefaultInstance()
        mTask = realm.where(Task::class.java).equalTo("id",taskId).findFirst()
        realm.close()

        if (mTask == null){
            //新規作成の場合
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

        }else{
            //更新の場合
            title_edit_text.setText(mTask!!.title)
            content_edit_text.setText(mTask!!.contents)

            val calendar = Calendar.getInstance()
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val dateString = mYear.toString() + "/" + String.format("%02d",mMonth + 1) + "/" + String.format("%02d",mDay)
            val timeString = String.format("%02d",mHour) + ":" + String.format("%02d",mMinute)

            date_button.text = dateString
            times_button.text = timeString
        }

        reloadListView()
    }

    override fun onResume() {
        super.onResume()


    }

    private fun  addTask() {
        val realm = Realm.getDefaultInstance()

        realm.beginTransaction()

        if (mTask == null){
            //新規作成の場合
            mTask = Task()

            val taskRealmResult = realm.where(Task::class.java).findAll()

            val identifier: Int =
                if (taskRealmResult.max("id") != null){
                    taskRealmResult.max("id")!!.toInt() + 1
                }else{
                    0
                }
            mTask!!.id = identifier
        }

        val title = title_edit_text.text.toString()
        val content = content_edit_text.text.toString()

        mTask!!.title = title
        mTask!!.contents = content
        mTask!!.category = mCategory
        val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        val date = calendar.time
        mTask!!.date = date

        realm.copyToRealmOrUpdate(mTask!!)
        realm.commitTransaction()

        realm.close()

        val resultIntent = Intent(applicationContext,TaskAlarmReceiver::class.java)
        resultIntent.putExtra(EXTRA_TASK,mTask!!.id)//通知の内容を作るための準備
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,//識別のため
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT//既存のPendingIntentは内容のみ置き換え
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP/*UTC時間を指定する、スリープ中でもアラームを発行する*/,
            calendar.timeInMillis/*UTC時間で指定*/,resultPendingIntent)
    }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Category::class.java).findAll().sort("id", Sort.DESCENDING)

        // 上記の結果を、TaskList としてセットする
        mCategoryobject = mRealm.copyFromRealm(taskRealmResults)

        var categoryString:String
        for (i in mCategoryobject.indices){
            categoryString = mCategoryobject[i].category
            spinnerItems.add(categoryString)
        }



    }

}