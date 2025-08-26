package com.example.Purrgenda

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications_enabled", true)) return

        val title = intent.getStringExtra("title") ?: "Reminder"
        val minutesBefore = intent.getIntExtra("minutesBefore", 10)

        // ✅ Debug log
        android.util.Log.d("ReminderReceiver", "Alarm received: $minutesBefore min before \"$title\"")

        val builder = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle("Reminder Incoming")
            .setContentText("In $minutesBefore minutes: $title")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify((System.currentTimeMillis() / 1000).toInt(), builder.build())
        }
    }
}


