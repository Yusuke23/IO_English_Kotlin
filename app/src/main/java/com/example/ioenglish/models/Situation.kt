package com.example.ioenglish.models

import android.os.Parcel
import android.os.Parcelable

data class Situation (
    val situation: String = "",
    val date: String = "",
    val imageSituation: String = "",
    val userId: String = "",
    var documentId: String = "",
    var phraseList: ArrayList<Phrase> = ArrayList(),

    ): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Phrase.CREATOR)!!
        )

    override fun writeToParcel(parcel: Parcel, flags: Int) = with(parcel) {
        parcel.writeString(situation)
        parcel.writeString(date)
        parcel.writeString(imageSituation)
        parcel.writeString(userId)
        parcel.writeString(documentId)
        parcel.writeTypedList(phraseList)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Situation> {

        override fun createFromParcel(parcel: Parcel): Situation {
            return Situation(parcel)
        }

        override fun newArray(size: Int): Array<Situation?> {
            return arrayOfNulls(size)
        }
    }
}