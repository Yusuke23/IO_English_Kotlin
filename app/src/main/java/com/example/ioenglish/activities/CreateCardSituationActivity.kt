package com.example.ioenglish.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import com.example.ioenglish.databinding.ActivityCreateCardSituationBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Phrase
import com.example.ioenglish.models.Situation
import com.example.ioenglish.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CreateCardSituationActivity: BaseActivity() {

    private var mCardSituationImageURL: String = ""
    private var mSelectedImageFileUri: Uri? = null


    private lateinit var binding: ActivityCreateCardSituationBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCardSituationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アクションバーに戻るボタンを付け、タップで前ページへ戻る
        setupActionBar()

        // 画像ボタンを押した時に出るダイアログ。デバイスのストレージへのアクセス許可を求める。
        binding.ivCreateCardSituationImage.setOnClickListener {
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
        setSupportActionBar(binding.toolbarCreateCardSituation)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = resources.getString(R.string.create_card_situation_title)
        }

        // 前の画面へ戻る
        binding.toolbarCreateCardSituation.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    // シチュエーションカードを作成
    private fun createCardSituation() {

        // current user id を取得
        val currentUserID = FirestoreClass().getCurrentUserId()
        // 現在時刻を取得し、フォーマットを変更
        val timestamp = System.currentTimeMillis()
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.JAPANESE)
        val theDate = simpleDateFormat.format(Date(timestamp))

        val situation = Situation(
            binding.etCreateCardSituation.text.toString(),
            theDate,
            mCardSituationImageURL,
            currentUserID,
        )

        FirestoreClass().createCardSituation(this, situation)
    }

    // firebase storage に画像を保存
    private fun uploadCardSituationImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        val sRef: StorageReference =
            FirebaseStorage.getInstance().reference.child(
                "CARD_SITUATION_IMAGE" + System.currentTimeMillis()
                        + "." + Constants.getFileExtension(this, mSelectedImageFileUri)
            )

        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
            Log.e(
                "Situation Image URL",
                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
            )

            taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                Log.i("Downloadable Image URL", uri.toString())
                mCardSituationImageURL = uri.toString()

                createCardSituation()
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

    fun noteCreatedSuccessfully() {
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
                    uploadCardSituationImage()
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    createCardSituation()
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
                    .into(binding.ivCreateCardSituationImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}