package com.example.ioenglish.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.ioenglish.R
import com.example.ioenglish.databinding.ActivityCreateCardPhraseBinding
import com.example.ioenglish.dialogs.LabelColorListDialog
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Phrase
import com.example.ioenglish.models.Situation
import com.example.ioenglish.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CreateCardPhraseActivity: BaseActivity() {

    private var mSelectedColor = ""
    private lateinit var mCardSituationDetails: Situation
    private var cardDocumentId = ""
    private var mNoteImageURL: String = ""
    private var mSelectedImageFileUri: Uri? = null


    private lateinit var binding: ActivityCreateCardPhraseBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCardPhraseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //todo try CardPhraseActivity から情報を引き継ぐ
        getIntentData()

        // アクションバーに戻るボタンを付け、タップで前ページへ戻る
        setupActionBar()

        // テキストビューを独自にスクロール
        binding.svCreateCardPhrase.setOnTouchListener { _, _ ->
            binding.etCreatePhraseInJapanese.parent.requestDisallowInterceptTouchEvent(false)
            binding.etCreatePhraseInEnglish.parent.requestDisallowInterceptTouchEvent(false)
            false
        }
        binding.etCreatePhraseInJapanese.setOnTouchListener { _, _ ->
            binding.etCreatePhraseInJapanese.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        binding.etCreatePhraseInEnglish.setOnTouchListener { _, _ ->
            binding.etCreatePhraseInEnglish.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        // 画像ボタンを押した時に出るダイアログ。デバイスのストレージへのアクセス許可を求める。
        binding.ivCardPhraseImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        //todo try label color
        binding.tvCreateColorLabel.setOnClickListener {
            labelColorsListDialog()
        }

    }

    // アクションバーに戻るボタンをつける
    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarCreateCardPhrase)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = resources.getString(R.string.create_card_phrase_title)
        }

        // 前の画面へ戻る
        binding.toolbarCreateCardPhrase.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    // CardPhraseActivity から情報を引き継ぐ
    private fun getIntentData() {

        if (intent.hasExtra(Constants.CARD_DETAIL)) {
            mCardSituationDetails = intent.getParcelableExtra(Constants.CARD_DETAIL)!!
        }

        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            cardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }
    }

    // カードを作成
    private fun addCardToPhraseList() {
        // current user id を取得
        val currentUserID = FirestoreClass().getCurrentUserId() //todo id 取得できていない
        // 現在時刻を取得し、フォーマットを変更
        val timestamp = System.currentTimeMillis()
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.JAPANESE)
        val theDate = simpleDateFormat.format(Date(timestamp))

        val phrase = Phrase(
            binding.etCreatePhraseInJapanese.text.toString(),
            binding.etCreatePhraseInEnglish.text.toString(),
            theDate,
            mNoteImageURL,
            currentUserID,
            binding.etCreateKeyWord.text.toString(),
            mSelectedColor
        )

        mCardSituationDetails.phraseList.add(0, phrase)

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().createCardPhrase(this, mCardSituationDetails)
    }

    // label color
    private fun colorsList(): ArrayList<String> {
        return ArrayList(resources.getStringArray(R.array.label_colors).asList())
    }

    // label color
    private fun setColor() {
        binding.tvCreateColorLabel.setText("")
        binding.tvCreateColorLabel.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    // label color
    private fun labelColorsListDialog() {
        val colorsList: ArrayList<String> = colorsList()

        val listDialog = object : LabelColorListDialog(
            this,
            colorsList,
            resources.getString(R.string.str_select_label_color),
            mSelectedColor
        ) {
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }

    // firebase storage に画像を保存
    private fun uploadNoteImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        val sRef: StorageReference =
            FirebaseStorage.getInstance().reference.child(
                "NOTE_IMAGE" + System.currentTimeMillis()
                        + "." + Constants.getFileExtension(this, mSelectedImageFileUri)
            )

        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
            Log.e(
                "Note Image URL",
                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
            )

            taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                Log.i("Downloadable Image URL", uri.toString())
                mNoteImageURL = uri.toString()

                addCardToPhraseList()
            }
        }.addOnFailureListener {
                exception ->
            Toast.makeText(
                this,
                exception.message,
                Toast.LENGTH_LONG
            ).show()

            hideProgressDialog()
        }
    }

    fun cardCreatedSuccessfully() {
        hideProgressDialog()

        // リロード用
        setResult(Activity.RESULT_OK)

        finish()
    }

    // menu の xml ファイルを貼り付ける
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_note_done, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            //done(check)ボタンを押すと情報が追加される
            R.id.action_create_card_done -> {
                if (mSelectedImageFileUri != null) {
                    uploadNoteImage()
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    addCardToPhraseList()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // デバイスのストレージにアクセス許可を求めた時、デバイスのpermissionの状態がチェックされる
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this)
            }
        } else {
            Toast.makeText(
                this,
                "Oops, you just denied the permission for storage. You can also allow it from settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // 画面の状態
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data

            try {
                Glide
                    .with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.add_screen_image_placeholder)
                    .into(binding.ivCardPhraseImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}