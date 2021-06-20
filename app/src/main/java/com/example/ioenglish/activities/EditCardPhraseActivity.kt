package com.example.ioenglish.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import com.example.ioenglish.databinding.ActivityEditCardPhraseBinding
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

class EditCardPhraseActivity : BaseActivity() {

    private var mSelectedColor = ""
    private var mNoteImageURL: String = ""
    private var mCardPhraseListPosition = -1
    private lateinit var mCardDetails: Situation
    private var mSelectedImageFileUri: Uri? =  null // アップロード用
    private lateinit var binding: ActivityEditCardPhraseBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCardPhraseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setCardDetailsInUI(mCardDetails)

        setupActionBar()

        // テキストビューを独自にスクロール
        binding.svEditCardPhrase.setOnTouchListener { _, _ ->
            binding.etEditPhraseInJapanese.parent.requestDisallowInterceptTouchEvent(false)

            binding.etEditPhraseInEnglish.parent.requestDisallowInterceptTouchEvent(false)
            false
        }
        binding.etEditPhraseInJapanese.setOnTouchListener { _, _ ->
            binding.etEditPhraseInJapanese.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        binding.etEditPhraseInEnglish.setOnTouchListener { _, _ ->
            binding.etEditPhraseInEnglish.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        // デバイスの permission の状態を確認
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

        // label color
        binding.tvEditColorLabel.setOnClickListener {
            labelColorsListDialog()
        }

    }

    // アクションバーに戻るボタンをつける
    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarEditCardPhrase)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = resources.getString(R.string.edit_card_phrase_title)
        }

        // 前の画面へ戻る
        binding.toolbarEditCardPhrase.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    // CardPhraseActivity の情報を引き継ぐ
    private fun getIntentData() {
        if (intent.hasExtra(Constants.CARD_DETAIL)) {
            mCardDetails = intent.getParcelableExtra(Constants.CARD_DETAIL)!!
        }
        if (intent.hasExtra(Constants.PHRASE_LIST_ITEM_POSITION)) {
            mCardPhraseListPosition = intent.getIntExtra(
                Constants.PHRASE_LIST_ITEM_POSITION, -1
            )
        }
    }

    //　カードの情報を画面に貼り付ける
    private fun setCardDetailsInUI(situation: Situation?) {

        mCardDetails = situation!!

        Glide
            .with(this)
            .load(situation.phraseList[mCardPhraseListPosition].imagePhrase)
            .centerCrop()
            .placeholder(R.drawable.add_screen_image_placeholder)
            .into(binding.ivCardPhraseImage)

        binding.etEditPhraseInJapanese.setText(situation.phraseList[mCardPhraseListPosition].japanese)
        binding.etEditPhraseInEnglish.setText(situation.phraseList[mCardPhraseListPosition].english)
        binding.etEditKeyWord.setText(situation.phraseList[mCardPhraseListPosition].keyWord)

        if (situation.phraseList[mCardPhraseListPosition].labelColor.isNotEmpty()) {
            binding.tvEditColorLabel.setBackgroundColor(Color.parseColor(
                situation.phraseList[mCardPhraseListPosition].labelColor))}
    }

    // この Edit ページに飛んだ際、phraseList の画像情報はすでにあり、他の情報だけ更新して画像はそのまま触らずに保存したい時の処理。
    // 又、カラーラベルも同様。
    private fun setImageAndLabelColor() {
        if (mNoteImageURL.isEmpty()) {
            mNoteImageURL = mCardDetails.phraseList[mCardPhraseListPosition].imagePhrase
        }
        if (mSelectedColor.isEmpty()) {
            mSelectedColor = mCardDetails.phraseList[mCardPhraseListPosition].labelColor
        }
        updateEditNoteData()
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

    // firebase storage に画像を保存
    private fun uploadEditNoteImage() {
        showProgressDialog(resources.getString(R.string.please_wait))
        if (mSelectedImageFileUri != null) {

            val sRef: StorageReference =
                FirebaseStorage.getInstance().reference.child(
                    "USER_IMAGE" + System.currentTimeMillis()
                            + "." + Constants.getFileExtension(
                        this, mSelectedImageFileUri)
                )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                    taskSnapshot ->
                Log.i(
                    "Firebase Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        uri ->
                    Log.i("Downloadable Image URL", uri.toString())
                    mNoteImageURL = uri.toString()

                    updateEditNoteData()
                }
            }.addOnFailureListener {
                    e ->
                Toast.makeText(
                    this@EditCardPhraseActivity,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()

                hideProgressDialog()
            }
        }
    }

    // アップデートが終わると please wait 表示が消える
    fun noteUpdateSuccessfully() {
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }

    // 記入、または設定された情報をfirestoreに保存
    private fun updateEditNoteData() {
        // current user id を取得
        val currentUserID = FirestoreClass().getCurrentUserId() //todo id 取得できていない
        // 現在時刻を取得し、フォーマットを変更
        val timestamp = System.currentTimeMillis()
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.JAPANESE)
        val theDate = simpleDateFormat.format(Date(timestamp))

        val phrase = Phrase(
            binding.etEditPhraseInJapanese.text.toString(),
            binding.etEditPhraseInEnglish.text.toString(),
            theDate,
            mNoteImageURL,
            currentUserID,
            binding.etEditKeyWord.text.toString(),
            mSelectedColor
        )

        val phraseListHashMap = HashMap<String, Any>()
        phraseListHashMap[Constants.PHRASE_LIST] = mCardDetails.phraseList

        mCardDetails.phraseList[mCardPhraseListPosition] = phrase

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateCardPhraseData(this, mCardDetails, phraseListHashMap)
    }

    // label color
    private fun colorsList(): ArrayList<String> {
        return ArrayList(resources.getStringArray(R.array.label_colors).asList())
    }

    // label color
    private fun setColor() {
        binding.tvEditColorLabel.setText("")
        binding.tvEditColorLabel.setBackgroundColor(Color.parseColor(mSelectedColor))
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

    // menu の xml ファイルを貼り付ける
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_note, menu)
        menuInflater.inflate(R.menu.menu_edit_note_done, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            //deleteボタンを押した時にアラートを出してから消去
            R.id.action_delete_note -> {
                alertDialogForDeleteCard()
                return true
            }
            //done(check)ボタンを押すと情報が更新される
            R.id.action_edit_note_done -> {
                if (mSelectedImageFileUri != null) {
                    uploadEditNoteImage()
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    setImageAndLabelColor()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 選択した画像を画面に表示
        if (resultCode == Activity.RESULT_OK
            && requestCode == Constants.PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            mSelectedImageFileUri = data.data

            try {
                Glide
                    .with(this@EditCardPhraseActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.add_screen_image_placeholder)
                    .into(binding.ivCardPhraseImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // firestore カードを削除する
    private fun deleteCardPhrase() {
        mCardDetails.phraseList.removeAt(mCardPhraseListPosition)

        val phraseListHashMap = HashMap<String, Any>()
        phraseListHashMap[Constants.PHRASE_LIST] = mCardDetails.phraseList

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateCardPhraseData(this, mCardDetails, phraseListHashMap)

    }

    // 削除ボタンを押した時、本当に削除していいのか確認される
    private fun alertDialogForDeleteCard() {
        val builder = AlertDialog.Builder(this)
        //set title for alert dialog
        builder.setTitle(resources.getString(R.string.alert))
        //set message for alert dialog
        builder.setMessage(
            resources.getString(
                R.string.confirmation_message_to_delete_note
            )
        )
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        //performing positive action
        builder.setPositiveButton(resources.getString(R.string.yes)) { dialogInterface, _ ->
            dialogInterface.dismiss() // Dialog will be dismissed
            deleteCardPhrase()
        }
        //performing negative action
        builder.setNegativeButton(resources.getString(R.string.no)) { dialogInterface, _ ->
            dialogInterface.dismiss() // Dialog will be dismissed
        }
        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false) // Will not allow user to cancel after clicking on remaining screen area.
        alertDialog.show()  // show the dialog to UI
    }
}