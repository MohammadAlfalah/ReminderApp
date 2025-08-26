package com.example.Purrgenda

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditReminderActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var dateTimeInput: EditText
    private lateinit var updateButton: Button

    private var reminderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reminder)

        titleInput = findViewById(R.id.editTitleInput)
        dateTimeInput = findViewById(R.id.editDateTimeInput)
        updateButton = findViewById(R.id.updateReminderBtn)

        // Get data passed from adapter
        reminderId = intent.getStringExtra("id") ?: ""
        titleInput.setText(intent.getStringExtra("title") ?: "")
        dateTimeInput.setText(intent.getStringExtra("dateTime") ?: "")

        updateButton.setOnClickListener {
            val newTitle = titleInput.text.toString().trim()
            val newDateTime = dateTimeInput.text.toString().trim()

            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update Firestore and wait for confirmation
            FirebaseFirestore.getInstance()
                .collection("reminders")
                .document(reminderId)
                .update(
                    mapOf(
                        "title" to newTitle,
                        "dateTime" to newDateTime
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Reminder updated!", Toast.LENGTH_SHORT).show()
                    finish() // ✅ Only finish after success
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update reminder", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
