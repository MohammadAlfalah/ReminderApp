package com.example.Purrgenda

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Reminder(
    val id: String = "",
    val title: String = "a",
    val dateTime: String = "",
    var isCompleted: Boolean = false,
    var uid: String = ""
)
