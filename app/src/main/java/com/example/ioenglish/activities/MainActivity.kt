package com.example.ioenglish.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bumptech.glide.Glide
import com.example.ioenglish.R
import com.example.ioenglish.adapters.CardSituationAdapter
import com.example.ioenglish.databinding.ActivityMainBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Situation
import com.example.ioenglish.models.User
import com.example.ioenglish.utils.Constants
import com.example.ioenglish.utils.SwipeController
import com.example.ioenglish.utils.SwipeControllerActions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.*


@Suppress("NAME_SHADOWING")
class MainActivity: BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    var swipeController: SwipeController? = null

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_CARD_SITUATION_REQUEST_CODE: Int = 12 // ノートを新しく作った時、メイン画面のリロード用
        const val EDIT_CARD_SITUATION_REQUEST_CODE: Int = 13 // ノートを編集した時、メイン画面のリロード用
    }

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 右下のフローティングボタンを押すと card situation を書くページへ遷移
        binding.iAppbarMain.fabCreateCardSituation.setOnClickListener {
            startActivityForResult(
                Intent(this, CreateCardSituationActivity::class.java),
                CREATE_CARD_SITUATION_REQUEST_CODE)
        }


        setupActionBar()

        binding.navView.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(
            Constants.IOENGLISH_PREFERENCES, Context.MODE_PRIVATE
        )

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this)
        FirestoreClass().getCardList(this)
        hideProgressDialog()

    }

    private fun setupActionBar() {
        setSupportActionBar(binding.iAppbarMain.iMainContentCardSituation.toolbarMain)
        binding.iAppbarMain.iMainContentCardSituation.toolbarMain.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        binding.iAppbarMain.iMainContentCardSituation.toolbarMain.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // ナビゲーションドローワーの画像をロードして表示
        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this)
        }
        // ノートを新しく作った時、メイン画面のリロード用
        else if (resultCode == Activity.RESULT_OK && requestCode == CREATE_CARD_SITUATION_REQUEST_CODE) {
            FirestoreClass().getCardList(this)
        }
        // ノートを編集した時、メイン画面のリロード用
        else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_CARD_SITUATION_REQUEST_CODE) {
            FirestoreClass().getCardList(this)
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
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    // メイン画面にノート or 何もない画面表示
    fun populateNotesListToUI(cardList: ArrayList<Situation>) {

        hideProgressDialog()

        // 作成されたノートがある場合ノートを並べて表示、なければない No notes available とかいた画面を表示
        if (cardList.size > 0) {
            binding.iAppbarMain.iMainContentCardSituation.rvSituationList.visibility = View.VISIBLE
            binding.iAppbarMain.iMainContentCardSituation.tvNoSituationListAvailable.visibility = View.GONE
            binding.iAppbarMain.iMainContentCardSituation.rvSituationList.layoutManager = LinearLayoutManager(this)
            binding.iAppbarMain.iMainContentCardSituation.rvSituationList.setHasFixedSize(true)

            val adapter = CardSituationAdapter(this, cardList)
            binding.iAppbarMain.iMainContentCardSituation.rvSituationList.adapter = adapter

            swipeController = SwipeController(object : SwipeControllerActions() {

                // viewHolder を左にスワイプして下層レイヤーから出てくる EDIT button を押した時の処理
                override fun onRightClicked(position: Int) {
                    val adapter = binding.iAppbarMain.iMainContentCardSituation.rvSituationList.adapter as CardSituationAdapter
                    adapter.notifyEditCardSituation(
                        this@MainActivity,
                        position,
                        EDIT_CARD_SITUATION_REQUEST_CODE
                    )
                }

            })

            val itemTouchHelper = ItemTouchHelper(swipeController!!)
            itemTouchHelper.attachToRecyclerView(binding.iAppbarMain.iMainContentCardSituation.rvSituationList)

            binding.iAppbarMain.iMainContentCardSituation.rvSituationList.addItemDecoration(object : ItemDecoration() {
                override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    swipeController!!.onDraw(c)
                }
            })

            // 作成されたノートをクリックした時の処理
            adapter.setOnClickListener(object : CardSituationAdapter.OnClickListener {
                override fun onClick(position: Int, model: Situation) {
                    val intent = Intent(this@MainActivity, CardPhraseActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })

        } else {
            binding.iAppbarMain.iMainContentCardSituation.rvSituationList.visibility = View.GONE
            binding.iAppbarMain.iMainContentCardSituation.tvNoSituationListAvailable.visibility = View.VISIBLE
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
            FirestoreClass().getCardList(this)
        }
    }

}