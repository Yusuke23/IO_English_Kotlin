package com.example.ioenglish.activities

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.example.ioenglish.R
import com.example.ioenglish.databinding.ActivitySignUpBinding
import com.example.ioenglish.firebase.FirestoreClass
import com.example.ioenglish.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : BaseActivity() {
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
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
        }

        setupActionBar()
    }


    private fun setupActionBar() {
        // アクションバーに戻るボタンをつける
        setSupportActionBar(binding.toolbarSignUpActivity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        // 前の画面へ戻る
        binding.toolbarSignUpActivity.setNavigationOnClickListener {
            onBackPressed()
        }

        // Sign Up ボタンが押されたらユーザーとして登録
        binding.btnSignUp.setOnClickListener {
            registerUser()
        }
    }

    // name email password の情報とともにユーザー登録する
    private fun registerUser() {
        val name: String = binding.etNameSignup.text.toString().trim { it <= ' ' }
        val email: String = binding.etEmailSignup.text.toString().trim { it <= ' ' }
        val password: String = binding.etPasswordSignup.text.toString().trim { it <= ' ' }

        // 記入情報のチェックの間に表示されるダイアログ
        if (validateForm(name, email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    hideProgressDialog()

                    // 記入された情報をfirebaseにユーザーとして登録する
                    if (task.isSuccessful) {
                        val firebaseUser: FirebaseUser = task.result!!.user!!
                        val registeredEmail = firebaseUser.email!!
                        val user = User(firebaseUser.uid, name, registeredEmail)

                        FirestoreClass().registerUser(this, user)
                    } else {
                        Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                    }
                }

        }
    }

    // 必要事項が正確されているかチェック
    private fun validateForm(name: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please enter a name")
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter an email")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter a password")
                false
            } else -> {
                true
            }
        }
    }

    fun userRegisteredSuccess() {
        Toast.makeText(
            this,
            "You have " +
                    "successfully registered.",
            Toast.LENGTH_SHORT
        ).show()
        hideProgressDialog()
        FirebaseAuth.getInstance().signOut()
        // サインアップ画面を閉じる
        finish()
    }
}