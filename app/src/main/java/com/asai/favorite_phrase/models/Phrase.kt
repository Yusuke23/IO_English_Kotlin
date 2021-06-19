package com.example.ioenglish.models

import android.os.Parcel
import android.os.Parcelable

data class Phrase (
    val japanese: String = "",
    val english: String = "",
    val date: String = "",
    val imagePhrase: String = "",
    val userId: String = "",
    val keyWord: String = "",
    val labelColor: String = "",
    var documentId: String = "",


): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,

    )

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest){
        writeString(japanese)
        writeString(english)
        writeString(date)
        writeString(imagePhrase)
        writeString(userId)
        writeString(keyWord)
        writeString(labelColor)
        writeString(documentId)


    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Phrase> {
        override fun createFromParcel(parcel: Parcel): Phrase {
            return Phrase(parcel)
        }

        override fun newArray(size: Int): Array<Phrase?> {
            return arrayOfNulls(size)
        }
    }

//    companion object {
//        @JvmField
//        val CREATOR: Parcelable.Creator<Phrase> = object : Parcelable.Creator<Phrase> {
//            override fun createFromParcel(source: Parcel): Phrase = Phrase(source)
//            override fun newArray(size: Int): Array<Phrase?> = arrayOfNulls(size)
//        }
//    }
}