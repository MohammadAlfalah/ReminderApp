package com.example.Purrgenda

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("reminders")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        val reminder = doc.toObject(Reminder::class.java)
                        val title = reminder.title
                        val dateTime = reminder.dateTime
                        val parts = dateTime.split(" at ")
                        if (parts.size == 2) {
                            scheduleNotification(context, title, parts[0], parts[1])
                        }
                    }
                }
        }
    }

    private fun scheduleNotification(context: Context, title: String, date: String, time: String) {
        val formatter = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
        val dateTime = formatter.parse("$date $time") ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val times = listOf(10, 5)
        for (minutesBefore in times) {
            val trigger = dateTime.time - minutesBefore * 60 * 1000
            if (trigger > System.currentTimeMillis()) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("title", title)
                    putExtra("minutesBefore", minutesBefore)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (System.currentTimeMillis() / 1000 + minutesBefore).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
            }
        }
    }
}
