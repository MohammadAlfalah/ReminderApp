package com.example.Purrgenda

import android.animation.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var catImage: ImageView
    private lateinit var appName: TextView
    private lateinit var logoBlock: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Apply saved theme preference BEFORE setContentView
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "Initialized")

        val catImage = findViewById<ImageView>(R.id.catImage)
        val appName = findViewById<TextView>(R.id.MyFApp)
        val logoBlock = findViewById<LinearLayout>(R.id.logoBlock)
        logoBlock.alpha = 0f

        // ✅ Handle deep link for shared reminder
        val data = intent?.data
        if (data?.scheme == "purrgenda" && data.host == "add") {
            val title = data.getQueryParameter("title")
            val datetime = data.getQueryParameter("datetime")

            if (!title.isNullOrBlank() && !datetime.isNullOrBlank()) {
                prefs.edit()
                    .putString("shared_title", title)
                    .putString("shared_datetime", datetime)
                    .apply()
            }
        }

        appName.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                appName.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val fadeIn = ObjectAnimator.ofFloat(logoBlock, "alpha", 0f, 1f).apply {
                    duration = 1000
                }

                val originalY = catImage.y
                val hangY = originalY + 20f
                val climbY = originalY - 40f

                val hangDown = ObjectAnimator.ofFloat(catImage, "y", originalY, hangY).setDuration(300)
                val climbUp = ObjectAnimator.ofFloat(catImage, "y", hangY, climbY).setDuration(300)
                val settle = ObjectAnimator.ofFloat(catImage, "y", climbY, originalY).setDuration(300)

                val shake = ObjectAnimator.ofFloat(catImage, "rotation", -10f, 10f).apply {
                    duration = 100
                    repeatCount = 5
                    repeatMode = ValueAnimator.REVERSE
                }

                val moveSequence = AnimatorSet().apply {
                    playSequentially(hangDown, climbUp, settle, shake)
                }

                AnimatorSet().apply {
                    playTogether(fadeIn, moveSequence)
                    start()
                }

                Handler(mainLooper).postDelayed({
                    val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
                    val nextActivity = if (isLoggedIn) MainActivity::class.java else LoginActivity::class.java
                    startActivity(Intent(this@SplashActivity, nextActivity))
                    finish()
                }, 3000)
            }
        })
    }

}
