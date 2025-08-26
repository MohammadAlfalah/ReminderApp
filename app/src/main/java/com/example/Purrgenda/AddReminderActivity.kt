package com.example.Purrgenda

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddReminderActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var saveButton: Button

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_reminder)

        titleInput = findViewById(R.id.titleInput)
        dateButton = findViewById(R.id.dateButton)
        timeButton = findViewById(R.id.timeButton)
        saveButton = findViewById(R.id.saveButton)

        val calendar = Calendar.getInstance()

        dateButton.setOnClickListener {
            val dp = DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                dateButton.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            dp.show()
        }

        timeButton.setOnClickListener {
            val tp = TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                timeButton.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            tp.show()
        }

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateTime = if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty())
                "$selectedDate at $selectedTime"
            else
                "No date/time set"

            // 🔐 Request permission to schedule exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(AlarmManager::class.java)
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Enable exact alarms permission", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                    return@setOnClickListener
                }
            }

            // 🔔 Notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }

            // Save to Firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val reminder = hashMapOf(
                "title" to title,
                "dateTime" to dateTime,
                "isCompleted" to false,
                "uid" to uid
            )

            FirebaseFirestore.getInstance().collection("reminders")
                .add(reminder)
                .addOnSuccessListener {
                    scheduleNotification(title, selectedDate, selectedTime)
                    Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save reminder", Toast.LENGTH_SHORT).show()
                }

        }
    }

    private fun scheduleNotification(title: String, date: String, time: String) {
        if (date.isEmpty() || time.isEmpty()) return

        val formatter = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
        val dateTime = formatter.parse("$date $time") ?: return
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val currentTime = System.currentTimeMillis()
        val times = listOf(10, 5)

        for (minutesBefore in times) {
            val triggerTime = dateTime.time - minutesBefore * 60 * 1000
            if (triggerTime > currentTime) {
                val intent = Intent(this, ReminderReceiver::class.java).apply {
                    putExtra("title", title)
                    putExtra("minutesBefore", minutesBefore)
                }

                val requestCode = (title + minutesBefore).hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }


}
