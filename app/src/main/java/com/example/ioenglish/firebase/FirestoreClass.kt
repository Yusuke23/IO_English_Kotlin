package com.example.ioenglish.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.ioenglish.activities.*
import com.example.ioenglish.models.Phrase
import com.example.ioenglish.models.Situation
import com.example.ioenglish.models.User
import com.example.ioenglish.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {

    private val mFirestore = FirebaseFirestore.getInstance()

    // ユーザー情報を firestore に保存
    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener { e ->
                Log.e(
                    activity.javaClass.simpleName,
                    "Error writing document",
                    e
                )
            }
    }

    // firebase auth で登録した時に自動的に作成されたID(User UID) を取得
    fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    // ユーザーの情報を取得する
    fun loadUserData(activity: Activity, readNotesList: Boolean = false) {
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)

                when (activity) {
                    is SignInActivity -> {
                        activity.signInSuccess(loggedInUser)
                    }
                    is MainActivity -> {
                        activity.updateNavigationUserDetails(loggedInUser, readNotesList)
                    }
                    is MyAccountActivity -> {
                        activity.setUserDataInUI(loggedInUser)
                    }
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyAccountActivity -> {
                        activity.hideProgressDialog()
                    }
                    is EditCardPhraseActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e("SignInUser", "Error writing document", e)
            }
    }

    // ユーザーの情報をアップデートする
    fun updateUserAccountData(
        activity: Activity,
        userHashMap: HashMap<String, Any>
    ) {
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Profile Data updated successfully!")

                Toast.makeText(activity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                when(activity) {
                    is MyAccountActivity -> {
                        activity.profileUpdateSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity) {
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyAccountActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a note.",
                    e
                )
            }
    }

    // create card situation で作成した情報を Firestore に保存
    fun createCardSituation(activity: CreateCardSituationActivity, situation: Situation) {
        mFirestore.collection(Constants.CARD_SITUATION)
            .document()
            .set(situation, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Card situation created successfully.")

                Toast.makeText(activity, "Card created successfully.", Toast.LENGTH_SHORT).show()

                activity.noteCreatedSuccessfully()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a card situation.",
                    e
                )
            }
    }

    // create card phrase で作成した情報を Firestore に保存
    fun createCardPhrase(activity: CreateCardPhraseActivity, situation: Situation) {
        val phraseListHashMap = HashMap<String, Any>()
        phraseListHashMap[Constants.PHRASE_LIST] = situation.phraseList

        mFirestore.collection(Constants.CARD_SITUATION)
            .document(situation.documentId)
            .update(phraseListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "PhraseList created successfully.")

                Toast.makeText(activity, "PhraseList created successfully.", Toast.LENGTH_SHORT).show()

                activity.cardCreatedSuccessfully()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a card.",
                    e
                )
            }
    }

    // firestore に保存されたカードの情報を取得
    fun getCardList(activity: Activity) {
        mFirestore.collection(Constants.CARD_SITUATION)
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())

                when(activity) {
                    is MainActivity -> {
                        val cardList: ArrayList<Situation> = ArrayList()
                        for (i in document.documents) {
                            val card = i.toObject(Situation::class.java)!!
                            card.documentId = i.id
                            cardList.add(card)
                        }
                        activity.populateNotesListToUI(cardList)
                    }
                }
            }
            .addOnFailureListener { e ->

                when(activity) {
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is CardPhraseActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(activity.javaClass.simpleName, "Error while creating a note.", e)
            }
    }

    // firestore に保存されている card situation の情報を取得
    fun getCardDetails(activity: Activity, documentId: String) {
        mFirestore.collection(Constants.CARD_SITUATION)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())

                when(activity) {
                    is EditCardSituationActivity -> {
                        activity.cardDetails(document.toObject(Situation::class.java)!!)
                    }
                    is CardPhraseActivity -> {
                        val situationCard = document.toObject(Situation::class.java)!!
                        situationCard.documentId = document.id
                        activity.cardDetails(situationCard)
                    }
                }
            }
            .addOnFailureListener { e ->

                when(activity) {
                    is EditCardSituationActivity -> {
                        activity.hideProgressDialog()
                    }
                    is CardPhraseActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(activity.javaClass.simpleName, "Error while creating a card situation.", e)
            }
    }

    // card phrase を削除する
    fun deleteCardSituation(activity: EditCardSituationActivity, documentId: String) {
        mFirestore.collection(Constants.CARD_SITUATION)
            .document(documentId)
            .delete()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())
            }
            .addOnFailureListener { e ->

                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while deleting a card situation.", e)
            }
    }

    // カードの情報をアップデートする
    fun updateCardData(
        activity: Activity, documentId: String,
        noteHashMap: HashMap<String, Any>
    ) {
        mFirestore.collection(Constants.CARD_SITUATION)
            .document(documentId)
            .update(noteHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Card Data updated successfully!")

                Toast.makeText(activity, "Card updated successfully!", Toast.LENGTH_SHORT).show()

                when(activity) {
                    is EditCardSituationActivity -> {
                        activity.noteUpdateSuccessfully()
                    }
                    is EditCardPhraseActivity -> {
                        activity.noteUpdateSuccessfully()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity) {
                    is EditCardSituationActivity -> {
                        activity.hideProgressDialog()
                    }
                    is EditCardPhraseActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a card.",
                    e
                )
            }
    }

    // カードの情報をアップデートする
    fun updateCardPhraseData(
        activity: Activity, situation: Situation, phraseListHashMap: HashMap<String, Any>
    ) {
        mFirestore.collection(Constants.CARD_SITUATION)
            .document(situation.documentId)
            .update(phraseListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Card Data updated successfully!")

                when(activity) {
                    is EditCardSituationActivity -> {
                        activity.noteUpdateSuccessfully()
                    }
                    is EditCardPhraseActivity -> {
                        activity.noteUpdateSuccessfully()
                    }
                    is CardPhraseActivity -> {
                        activity.noteUpdateSuccessfully()
                    }
                }
            }
            .addOnFailureListener { e ->
                when(activity) {
                    is EditCardSituationActivity -> {
                        activity.hideProgressDialog()
                    }
                    is EditCardPhraseActivity -> {
                        activity.hideProgressDialog()
                    }
                    is CardPhraseActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a card.",
                    e
                )
            }
    }

}