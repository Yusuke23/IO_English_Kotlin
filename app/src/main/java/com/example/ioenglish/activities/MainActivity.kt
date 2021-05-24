package com.example.ioenglish.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ioenglish.databinding.ActivityMainBinding

class MainActivity: AppCompatActivity() {

    companion object {
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }

    //todo ボタンを押した時の処理　mUserName
    private lateinit var mUserName: String

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.iAppBarMain.fabCreateBoard.setOnClickListener {
            val intent = Intent(this, AddNotesActivity::class.java)
            //todo ボタンを押した時の処理　mUserName
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }
    }

}