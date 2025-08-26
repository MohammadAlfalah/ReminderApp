package com.example.Purrgenda

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPassword: TextView
    private lateinit var goToRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rememberMeCheck: CheckBox

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        currentFocus?.let { view ->
            val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun loginWithEmail(email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
                    if (rememberMeCheck.isChecked) {
                        prefs.edit().putBoolean("isLoggedIn", true).apply()
                    }
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Email or password is incorrect", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Apply saved dark theme before UI loads
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        goToRegister = findViewById(R.id.goToRegister)
        progressBar = findViewById(R.id.loginProgressBar)
        rememberMeCheck = findViewById(R.id.rememberMeCheck) // ✅ make sure added to layout!

        loginButton.setOnClickListener {
            val input = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email/Username and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (input.contains("@")) {
                // Email login
                loginWithEmail(input, password)
            } else {
                // Username → email lookup
                FirebaseFirestore.getInstance().collection("usernames")
                    .document(input).get()
                    .addOnSuccessListener { doc ->
                        val email = doc.getString("email")
                        if (email != null) {
                            loginWithEmail(email, password)
                        } else {
                            Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
