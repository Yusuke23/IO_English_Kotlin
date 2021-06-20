package com.example.ioenglish.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowInsets
import android.view.WindowManager
import com.example.ioenglish.databinding.ActivitySplashBinding
import com.example.ioenglish.firebase.FirestoreClass

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // タイトル表示の際ステータスバーを表示せず、フルスクリーンにする。
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            // フォントを指定
            val typeFace = Typeface.createFromAsset(assets, "DancingScript-Bold.ttf")
            binding.tvAppName.typeface = typeFace

            // 自動で次の画面へ遷移
            Handler().postDelayed({

                // 一度サインアップをしているユーザーは自動ログインされる
                val currentUserID = FirestoreClass().getCurrentUserId()
                    if (currentUserID.isNotEmpty()) {
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this, IntroActivity::class.java))
                    }
                finish()
            }, 500)
        }
    }
}