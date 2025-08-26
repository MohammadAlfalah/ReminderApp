package com.example.Purrgenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReminderViewModel : ViewModel() {
    private val _reminders = MutableLiveData<MutableList<Reminder>>(mutableListOf())
    val reminders: LiveData<MutableList<Reminder>> get() = _reminders

    fun addReminder(reminder: Reminder) {
        _reminders.value?.add(reminder)
        _reminders.value = _reminders.value // trigger LiveData update
    }

    fun clearReminders() {
        _reminders.value = mutableListOf()
    }
}
