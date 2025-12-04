package com.QuickMenu.mobile.root

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.QuickMenu.mobile.auth.AuthActivity
import com.QuickMenu.mobile.databinding.ActivitySplashBinding
import com.QuickMenu.mobile.main.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        checkAuth()


    }

    private fun checkAuth(){

        val isUserLoggedIn = auth.currentUser != null


        val nextActivityClass = if (isUserLoggedIn) {
            MainActivity::class.java

        } else {
            AuthActivity::class.java
        }

        val intent = Intent(this, nextActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()

    }
    override fun onDestroy() {
        super.onDestroy()
    }
}