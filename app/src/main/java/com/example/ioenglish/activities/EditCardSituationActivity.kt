package com.example.ioenglish.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import com.example.ioenglish.databinding.ActivityEditCardSituationBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Situation
import com.example.ioenglish.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class EditCardSituationActivity : BaseActivity() {

    private lateinit var mSituationDetails: Situation
    private var mSelectedImageFileUri: Uri? =  null // アップロード用
    private var mProfileImageURL: String = "" // ダウンロード用
    private var cardDocumentId = ""

    private lateinit var binding: ActivityEditCardSituationBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCardSituationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()

        // メイン画面のノートに表示されている情報を持ってくる
//        var noteDocumentId = ""
        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            cardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getCardDetails(this, cardDocumentId)
        // Card の情報を表示
        FirestoreClass().loadUserData(this)

        // デバイスの permission の状態を確認
        binding.ivEditCardSituationImage.setOnClickListener {
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

    }

    // アクションバーに戻るボタンをつける
    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarEditCardSituation)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = resources.getString(R.string.edit_card_situation_title)
        }

        // 前の画面へ戻る
        binding.toolbarEditCardSituation.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    // カードの情報
    fun cardDetails(situation: Situation) {
        hideProgressDialog()
        setCardDetailsInUI(situation)
    }

    //　カードの情報を画面に貼り付ける
    private fun setCardDetailsInUI(situation: Situation?) {

        mSituationDetails = situation!!

        Glide
            .with(this)
            .load(situation.imageSituation)
            .centerCrop()
            .placeholder(R.drawable.add_screen_image_placeholder)
            .into(binding.ivEditCardSituationImage)

        binding.etEditCardSituation.setText(situation.situation)
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
                    mProfileImageURL = uri.toString()

                    updateEditCardSituationData()
                }
            }.addOnFailureListener {
                    e ->
                Toast.makeText(
                    this@EditCardSituationActivity,
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

    // 記入、設定された情報をfirestoreに保存
    private fun updateEditCardSituationData() {
        val noteHashMap = HashMap<String, Any>()

        if (binding.etEditCardSituation.text.toString() != mSituationDetails.situation) {
            noteHashMap[Constants.SITUATION] = binding.etEditCardSituation.text.toString()
        }

        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mSituationDetails.imageSituation) {
            noteHashMap[Constants.IMAGE_SITUATION] = mProfileImageURL
        }

        FirestoreClass().updateCardData(this, cardDocumentId, noteHashMap)
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

                    updateEditCardSituationData()
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
                    .with(this@EditCardSituationActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.add_screen_image_placeholder)
                    .into(binding.ivEditCardSituationImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // firestore cardSituation を削除する
    private fun deleteCard() {
        FirestoreClass().deleteCardSituation(this, cardDocumentId)

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
            deleteCard()
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