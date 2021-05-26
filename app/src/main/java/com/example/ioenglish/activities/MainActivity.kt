package com.example.ioenglish.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ioenglish.R
import com.example.ioenglish.adapters.NoteItemsAdapter
import com.example.ioenglish.databinding.ActivityMainBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.Note
import com.example.ioenglish.models.User
import com.example.ioenglish.utils.Constants
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity: BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_NOTE_REQUEST_CODE: Int = 12 // ノートを新しく作った時、メイン画面のリロード用
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 右下のフローティングボタンを押すとノートを書くページへ遷移
        binding.iAppBarMain.fabCreateNote.setOnClickListener {
            startActivityForResult(
                Intent(this, CreateNoteActivity::class.java),
                CREATE_NOTE_REQUEST_CODE)
        }

        setupActionBar()

        binding.navView.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(
            Constants.IOENGLISH_PREFERENCES, Context.MODE_PRIVATE
        )

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this)
        FirestoreClass().getNotesList(this)
        hideProgressDialog()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // ナビゲーションドローワーの画像をロードして表示
        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this)
        }
        // ノートを新しく作った時、メイン画面のリロード用
        else if (resultCode == Activity.RESULT_OK && requestCode == CREATE_NOTE_REQUEST_CODE) {
            FirestoreClass().getNotesList(this)
        }
//todo ↑ ↓ どっちか
//        if (resultCode == Activity.RESULT_OK && requestCode == CREATE_NOTE_REQUEST_CODE) {
//            FirestoreClass().getNotesList(this)
//        }

        else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    // view binding 別のXMLから参照 /
    private fun setupActionBar() {
        setSupportActionBar(binding.iAppBarMain.toolbarMainActivity)
        binding.iAppBarMain.toolbarMainActivity.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        binding.iAppBarMain.toolbarMainActivity.setNavigationOnClickListener {
            toggleDrawer()
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

    // ドローワーを閉じる or アプリを閉じる
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)

        } else {
            doubleBackToExit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                startActivityForResult(
                    Intent(this, MyProfileActivity::class.java),
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
            //todo tag用のコード
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    // メイン画面にノート or 何もない画面表示
    fun populateNotesListToUI(notesList: ArrayList<Note>) {
        hideProgressDialog()

        // 作成されたノートがある場合ノートを並べて表示、なければない No notes available とかいた画面を表示
        if (notesList.size > 0) {
            binding.iAppBarMain.iMainContent.rvNotesList.visibility = View.VISIBLE
            binding.iAppBarMain.iMainContent.tvNoNotesAvailable.visibility = View.GONE
            binding.iAppBarMain.iMainContent.rvNotesList.layoutManager = LinearLayoutManager(this)
            binding.iAppBarMain.iMainContent.rvNotesList.setHasFixedSize(true)

            val adapter = NoteItemsAdapter(this, notesList)
            binding.iAppBarMain.iMainContent.rvNotesList.adapter = adapter



            // 作成されたボードをクリックすると遷移
//            adapter.setOnClickListener(object : NoteItemsAdapter.OnClickListener {
//                override fun onClick(position: Int, model: Note) {
//                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
//                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
//                    startActivity(intent)
//                }
//            })

        } else {
            binding.iAppBarMain.iMainContent.rvNotesList.visibility = View.GONE
            binding.iAppBarMain.iMainContent.tvNoNotesAvailable.visibility = View.VISIBLE
        }
    }

    fun updateNavigationUserDetails(user: User?, readNotesList: Boolean) {
        // todo user name

        Glide
            .with(this)
            .load(user!!.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(binding.iNavHeaderMain.navUserImage)

        if (readNotesList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getNotesList(this)
        }
    }

}