package com.example.ioenglish.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap

object Constants {

    const val USERS: String = "users"
    const val NAME: String = "name"
    const val IMAGE: String = "image"
    const val IMAGE_SITUATION: String = "imageSituation"
    const val CARD_SITUATION: String = "cardSituation"
    const val SITUATION: String = "situation"
    const val PHRASE_LIST_ITEM_POSITION: String = "phrase_list_item_position"
    const val PHRASE_LIST: String = "phraseList"
    const val CARD_DETAIL: String = "card_detail"
    const val DOCUMENT_ID: String = "documentId"
    const val IOENGLISH_PREFERENCES = "IOEnglishPrefs"
    const val READ_STORAGE_PERMISSION_CODE = 1
    const val PICK_IMAGE_REQUEST_CODE = 2

    // デバイスの画像選択の画面を表示
    fun showImageChooser(activity: Activity) {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        activity.startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
    }

    fun getFileExtension(activity: Activity, uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }
}