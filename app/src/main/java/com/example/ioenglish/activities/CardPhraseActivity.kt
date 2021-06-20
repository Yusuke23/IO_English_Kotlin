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
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.example.ioenglish.R
import com.example.ioenglish.adapters.CardPhraseAdapter
import com.example.ioenglish.databinding.ActivityCardPhraseBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Phrase
import com.example.ioenglish.models.Situation
import com.example.ioenglish.utils.Constants
import com.example.ioenglish.utils.SwipeController
import com.example.ioenglish.utils.SwipeControllerActions
import java.util.*
import kotlin.collections.ArrayList


@Suppress("NAME_SHADOWING")
class CardPhraseActivity: BaseActivity(), TextToSpeech.OnInitListener {

    private var mPositionDraggedFrom = -1
    private var mPositionDraggedTo = -1
    private lateinit var mCardDetails: Situation
    private lateinit var mCardDocumentId: String
    private val sttTag = "RecognitionListener"
    private var tts: TextToSpeech? = null
    var swipeController: SwipeController? = null
    private lateinit var mSharedPreferences: SharedPreferences

    companion object {
        const val CREATE_CARD_PHRASE_REQUEST_CODE: Int = 21 // ノートを新しく作った時、メイン画面のリロード用
        const val EDIT_CARD_PHRASE_REQUEST_CODE: Int = 22 // ノートを編集した時、メイン画面のリロード用
    }

    private lateinit var binding: ActivityCardPhraseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardPhraseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MainActivity の情報を受け取る
        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            mCardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }

        // 右下のフローティングボタンを押すとカード（phrase card）を書くページへ遷移
        binding.iAppbarCardPhrase.fabCreateCardPhrase.setOnClickListener {
            val intent = Intent(this, CreateCardPhraseActivity::class.java)
            intent.putExtra(Constants.CARD_DETAIL, mCardDetails)
//            intent.putExtra(Constants.PHRASE_LIST_ITEM_POSITION, mCardPhraseListPosition)
            intent.putExtra(Constants.DOCUMENT_ID, mCardDetails.documentId)
            startActivityForResult(intent,
                CREATE_CARD_PHRASE_REQUEST_CODE)

            // tts の停止
            onStop()
        }

        swipeToShowEditButton()

        // text to speech
        tts = TextToSpeech(this, this)

        mSharedPreferences = this.getSharedPreferences(
            Constants.IOENGLISH_PREFERENCES, Context.MODE_PRIVATE
        )

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getCardDetails(this, mCardDocumentId)
        hideProgressDialog()

        // speech to text 用
        checkPermission()
        startSpeechToText()

    }

    // アクションバーに戻るボタンをつける
    private fun setupActionBar() {
        setSupportActionBar(binding.iAppbarCardPhrase.iMainContentCardPhrase.toolbarMainContentCardPhrase)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = mCardDetails.situation
        }

        // 前の画面へ戻る
        binding.iAppbarCardPhrase.iMainContentCardPhrase.toolbarMainContentCardPhrase.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun cardDetails(situation: Situation) {

        mCardDetails = situation


        hideProgressDialog()
        // アクションバーのタイトル用に情報を firestore から持ってくる
        setupActionBar()

        // 作成されたカードがある場合カードを並べて表示、なければない No cards available とかいた画面を表示
        if (situation.phraseList.size > 0) {
            binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.visibility = View.VISIBLE
            binding.iAppbarCardPhrase.iMainContentCardPhrase.tvNoCardPhraseAvailable.visibility = View.GONE
            binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.setHasFixedSize(true)

            val adapter = CardPhraseAdapter(this, situation.phraseList)
            binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.adapter = adapter

            // 作成されたカードをクリックした時の処理
            adapter.setOnClickListener(object : CardPhraseAdapter.OnClickListener {
                override fun onClick(position: Int, model: Phrase) {

                    //text to speech 画面のカードをタップすると英文を読み上げてくれる
                    speakOut(model.english)
                }
            })

            // drag & drop
            val helper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        dragged: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        val draggedPosition = dragged.adapterPosition
                        val targetPosition = target.adapterPosition

                        if (mPositionDraggedFrom == -1) {
                            mPositionDraggedFrom = draggedPosition
                        }
                        mPositionDraggedTo = targetPosition
                        Collections.swap(
                            mCardDetails.phraseList,
                            draggedPosition, targetPosition
                        )
                        adapter.notifyItemMoved(draggedPosition, targetPosition)
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    }

                    override fun clearView(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder
                    ) {
                        super.clearView(recyclerView, viewHolder)
                        if (mPositionDraggedFrom != -1 && mPositionDraggedTo != -1
                            && mPositionDraggedFrom != mPositionDraggedTo
                        ) {
                            updateCardsInTaskList(
                                mCardDetails.phraseList
                            )
                        }
                        mPositionDraggedFrom = -1
                        mPositionDraggedTo = -1
                    }
                }
            )
            helper.attachToRecyclerView(binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList)

        } else {
            binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.visibility = View.GONE
            binding.iAppbarCardPhrase.iMainContentCardPhrase.tvNoCardPhraseAvailable.visibility = View.VISIBLE
        }
    }

    // recyclerView をスワイプできるようにする。かつ、下層レイヤーにボタンを設置。
    private fun swipeToShowEditButton() {
        swipeController = SwipeController(object : SwipeControllerActions() {

            // viewHolder を左にスワイプして下層レイヤーから出てくる EDIT button を押した時の処理
            override fun onRightClicked(position: Int) {
                val adapter = binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.adapter as CardPhraseAdapter
                adapter.notifyEditCardPhrase(
                    this@CardPhraseActivity,
                    mCardDetails,
                    position,
                    EDIT_CARD_PHRASE_REQUEST_CODE
                )
                // tts の停止
                onStop()
            }

        })

        val itemTouchHelper = ItemTouchHelper(swipeController!!)
        itemTouchHelper.attachToRecyclerView(binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList)

        binding.iAppbarCardPhrase.iMainContentCardPhrase.rvCardPhraseList.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                swipeController!!.onDraw(c)
            }
        })
    }

    fun updateCardsInTaskList(cardPhrase: ArrayList<Phrase>) {
        mCardDetails.phraseList =  cardPhrase

        val phraseListHashMap = HashMap<String, Any>()
        phraseListHashMap[Constants.PHRASE_LIST] = mCardDetails.phraseList

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateCardPhraseData(this, mCardDetails, phraseListHashMap)
    }

    // アップデートが終わると please wait 表示が消える
    fun noteUpdateSuccessfully() {
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // カードを新しく作った時、card phrase 画面のリロード用
        if (resultCode == Activity.RESULT_OK && requestCode == CREATE_CARD_PHRASE_REQUEST_CODE) {
            FirestoreClass().getCardDetails(this, mCardDocumentId)
        }
        // カードを編集した時、card phrase 画面のリロード用
        else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_CARD_PHRASE_REQUEST_CODE) {
            FirestoreClass().getCardDetails(this, mCardDocumentId)
        } else {
            Log.e("Cancelled", "Cancelled")
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
        val editText = binding.iAppbarCardPhrase.iMainContentCardPhrase.etShowSpeechToText

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

        binding.iAppbarCardPhrase.iMainContentCardPhrase.ibMicButton.setOnTouchListener { _, motionEvent ->
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