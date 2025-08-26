package com.example.Purrgenda

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var backToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        usernameInput = findViewById(R.id.registerUsernameInput)
        emailInput = findViewById(R.id.registerEmailInput)
        passwordInput = findViewById(R.id.registerPasswordInput)
        registerButton = findViewById(R.id.registerButton)
        backToLogin = findViewById(R.id.backToLogin)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if username already exists
            db.collection("usernames").document(username).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        Toast.makeText(this, "Username is already taken", Toast.LENGTH_SHORT).show()
                    } else {
                        // Proceed to create user
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                // Save username to Firestore
                                db.collection("usernames").document(username).set(
                                    mapOf("email" to email)
                                ).addOnSuccessListener {
                                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Register failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to check username: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        backToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}