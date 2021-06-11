package com.example.ioenglish.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bumptech.glide.Glide
import com.example.ioenglish.R
import com.example.ioenglish.adapters.NoteItemsAdapter
import com.example.ioenglish.databinding.ActivityMainBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Note
import com.example.ioenglish.models.User
import com.example.ioenglish.utils.Constants
import com.example.ioenglish.utils.SwipeController
import com.example.ioenglish.utils.SwipeControllerActions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.*


@Suppress("NAME_SHADOWING")
class MainActivity: BaseActivity(), NavigationView.OnNavigationItemSelectedListener, TextToSpeech.OnInitListener {

    private val sttTag = "RecognitionListener"

    private var tts: TextToSpeech? = null

    var swipeController: SwipeController? = null

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_NOTE_REQUEST_CODE: Int = 12 // ノートを新しく作った時、メイン画面のリロード用
        const val EDIT_NOTE_REQUEST_CODE: Int = 13 // ノートを編集した時、メイン画面のリロード用
    }

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 検索バー左のボタンをタップでドロワーが開く
        binding.iAppBarMain.iMainContent.ibToggleDrawerOpen.setOnClickListener {
            toggleDrawer()

            // tts の停止
            onStop()
        }

        // 右下のフローティングボタンを押すとノートを書くページへ遷移
        binding.iAppBarMain.fabCreateNote.setOnClickListener {
            startActivityForResult(
                Intent(this, CreateNoteActivity::class.java),
                CREATE_NOTE_REQUEST_CODE)

            // tts の停止
            onStop()
        }


//        setupActionBar()

        // text to speech
        tts = TextToSpeech(this, this)

        binding.navView.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(
            Constants.IOENGLISH_PREFERENCES, Context.MODE_PRIVATE
        )

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this)
        FirestoreClass().getNotesList(this)
        hideProgressDialog()

        // speech to text 用
        checkPermission()
        startSpeechToText()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // ナビゲーションドローワーの画像をロードして表示
        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this)
        }
        // ノートを新しく作った時、メイン画面のリロード用
        else if (resultCode == Activity.RESULT_OK && requestCode == CREATE_NOTE_REQUEST_CODE) {
            FirestoreClass().getNotesList(this)
        }
        // ノートを編集した時、メイン画面のリロード用
        else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_NOTE_REQUEST_CODE) {
            FirestoreClass().getNotesList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }

    }

    // 画面左からドローワーを開く or 閉じる
    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)

        } else {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    // デバイスの戻るボタンをシングルタップ → ドローワーを閉じる。 / ダブルタップ → アプリを閉じる
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)

        } else {
            doubleBackToExit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_account -> {
                startActivityForResult(
                    Intent(this, MyAccountActivity::class.java),
                    MY_PROFILE_REQUEST_CODE
                )
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()

                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            //todo tag用のコード
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    // メイン画面にノート or 何もない画面表示
    fun populateNotesListToUI(notesList: ArrayList<Note>) {

        hideProgressDialog()

        // 作成されたノートがある場合ノートを並べて表示、なければない No notes available とかいた画面を表示
        if (notesList.size > 0) {
            binding.iAppBarMain.iMainContent.rvNotesList.visibility = View.VISIBLE
            binding.iAppBarMain.iMainContent.tvNoNotesAvailable.visibility = View.GONE
            binding.iAppBarMain.iMainContent.rvNotesList.layoutManager = LinearLayoutManager(this)
            binding.iAppBarMain.iMainContent.rvNotesList.setHasFixedSize(true)

            val adapter = NoteItemsAdapter(this, notesList)
            binding.iAppBarMain.iMainContent.rvNotesList.adapter = adapter

            swipeController = SwipeController(object : SwipeControllerActions() {

                // viewHolder を左にスワイプして下層レイヤーから出てくる EDIT button を押した時の処理
                override fun onRightClicked(position: Int) {
                    val adapter = binding.iAppBarMain.iMainContent.rvNotesList.adapter as NoteItemsAdapter
                    adapter.notifyEditItem(
                        this@MainActivity,
                        position,
                        EDIT_NOTE_REQUEST_CODE
                    )
                    // tts の停止
                    onStop()
                }

            })

            val itemTouchHelper = ItemTouchHelper(swipeController!!)
            itemTouchHelper.attachToRecyclerView(binding.iAppBarMain.iMainContent.rvNotesList)

            binding.iAppBarMain.iMainContent.rvNotesList.addItemDecoration(object : ItemDecoration() {
                override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    swipeController!!.onDraw(c)
                }
            })

            // 作成されたノートをクリックした時の処理
            adapter.setOnClickListener(object : NoteItemsAdapter.OnClickListener {
                override fun onClick(position: Int, model: Note) {

                    //text to speech 画面のノートをタップすると英文を読み上げてくれる
                    speakOut(model.english)
                }
            })

        } else {
            binding.iAppBarMain.iMainContent.rvNotesList.visibility = View.GONE
            binding.iAppBarMain.iMainContent.tvNoNotesAvailable.visibility = View.VISIBLE
        }
    }

    fun updateNavigationUserDetails(user: User?, readNotesList: Boolean) {

        Glide
            .with(this)
            .load(user!!.image)
            .centerCrop()
//            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.iNavHeaderMain.navUserImage)

        if (readNotesList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getNotesList(this)
        }
    }

    // text to speech 用
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    //todo try
    override fun onStop() {
        if (tts != null) {
            tts!!.stop()
        }
        super.onStop()
    }

    // text to speech 用
    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    // テキストを読み上げる機能
    private fun speakOut(text: String) {
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    // speech to text (emulator では使えない。)
    @SuppressLint("ClickableViewAccessibility")
    private fun startSpeechToText() {
        val editText = binding.iAppBarMain.iMainContent.etSearchNote

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.i(sttTag, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.i(sttTag, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.i(sttTag, "onRmsChanged")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.i(sttTag, "onBufferReceived")
            }

            override fun onEndOfSpeech() {
                Log.i(sttTag, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                Toast.makeText(applicationContext, errorHandler(error), Toast.LENGTH_SHORT).show()
                Log.i(sttTag, "onError")
            }

            override fun onResults(results: Bundle?) {
                val data: ArrayList<String>? =
                    results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                editText.text = data!![0]
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.i(sttTag, "onPartialResults")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        })

        binding.iAppBarMain.iMainContent.ibMicButton.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                    editText.hint = getString(R.string.speech_to_text_text_hint)
                }

                MotionEvent.ACTION_DOWN -> {
                    speechRecognizer.startListening(speechRecognizerIntent)
                    editText.text = ""
                    editText.hint = "Listening...\n"

                    // tts の停止
                    onStop()
                }
            }
            false
        }
    }

    // speech to text を使うため。ユーザーにマイク使用の許可を求める。
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                startActivity(intent)
                finish()
                Toast.makeText(this, "Enable Microphone Permission..!!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // speech to text 用 エラー原因をユーザーに知らせる。
    fun errorHandler(errorCode: Int): String {
        when (errorCode) {
            1 -> {
                return "NETWORK TIMEOUT"
            }
            2 -> {
                return "NETWORK ERROR"
            }
            3 -> {
                return "AUDIO ERROR"
            }
            4 -> {
                return "SERVER ERROR"
            }
            5 -> {
                return "CLIENT ERROR"
            }
            6 -> {
                return "SPEECH TIMEOUT"
            }
            7 -> {
                return "NO MATCH"
            }
            8 -> {
                return "RECOGNIZER BUSY"
            }
            9 -> {
                return "INSUFFICIENT_PERMISSIONS"
            }
            10 -> {
                return "TOO MANY REQUESTS"
            }

            11 -> {
                return "SERVER DISCONNECTED"
            }
            else -> {
            }
        }
        return "UNKNOWN ERROR"
    }

}