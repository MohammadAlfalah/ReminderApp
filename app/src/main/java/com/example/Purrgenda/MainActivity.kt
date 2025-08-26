package com.example.Purrgenda

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: ReminderAdapter
    private lateinit var viewModel: ReminderViewModel
    private var hideCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        recyclerView = findViewById(R.id.reminderRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        fab = findViewById(R.id.addReminderFab)

        viewModel = ViewModelProvider(this)[ReminderViewModel::class.java]
        adapter = ReminderAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val reminder = adapter.getItem(position)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to delete \"${reminder.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        if (reminder.id.isNotBlank()) {
                            FirebaseFirestore.getInstance()
                                .collection("reminders")
                                .document(reminder.id)
                                .delete()
                        }
                        adapter.removeItem(position)
                        updateUI()
                    }
                    .setNegativeButton("Cancel", null)
                    .setOnDismissListener {
                        adapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
                    .show()
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        fab.setOnClickListener {
            startActivity(Intent(this, AddReminderActivity::class.java))
        }

        viewModel.reminders.observe(this) { updateUI() }

        // ✅ Firestore snapshot listener
        FirebaseFirestore.getInstance().collection("reminders")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid.isNullOrBlank()) return@addSnapshotListener

                val updatedList = mutableListOf<Reminder>()

                for (doc in snapshots.documents) {
                    val reminder = doc.toObject(Reminder::class.java)
                    if (reminder != null && reminder.uid == uid) {
                        updatedList.add(reminder.copy(id = doc.id))
                    }
                }

                viewModel.clearReminders()
                updatedList.forEach { viewModel.addReminder(it) }
            }


        // ✅ Handle shared reminder (via deep link)
        val sharedTitle = prefs.getString("shared_title", null)
        val sharedDateTime = prefs.getString("shared_datetime", null)

        if (!sharedTitle.isNullOrBlank() && !sharedDateTime.isNullOrBlank()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val reminder = hashMapOf(
                    "a" to sharedTitle,
                    "dateTime" to sharedDateTime,
                    "isCompleted" to false,
                    "uid" to uid
                )
                FirebaseFirestore.getInstance().collection("reminders")
                    .add(reminder)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Shared reminder added!", Toast.LENGTH_SHORT).show()
                        prefs.edit().remove("shared_title").remove("shared_datetime").apply()
                    }
            }
        }
    }

    private fun updateUI() {
        val currentList = viewModel.reminders.value ?: listOf()
        val filtered = if (hideCompleted) currentList.filter { !it.isCompleted } else currentList

        adapter.updateData(filtered)

        emptyStateText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_toggle_hide_completed -> {
                hideCompleted = !hideCompleted
                item.title = if (hideCompleted)
                    getString(R.string.menu_show_completed)
                else
                    getString(R.string.menu_hide_completed)
                updateUI()
                true
            }
            R.id.action_show_only_completed -> {
                val completedOnly = viewModel.reminders.value?.filter { it.isCompleted } ?: emptyList()
                adapter.updateData(completedOnly)
                emptyStateText.visibility = if (completedOnly.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (completedOnly.isEmpty()) View.GONE else View.VISIBLE
                Toast.makeText(this, "Showing only completed reminders", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_share -> {
                shareReminderDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        if (menu.javaClass.simpleName == "MenuBuilder") {
            try {
                val method = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(menu, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val spanString = SpannableString(item.title)
            spanString.setSpan(ForegroundColorSpan(Color.BLACK), 0, spanString.length, 0)
            item.title = spanString
        }

        return super.onMenuOpened(featureId, menu)
    }

    private fun shareReminderDialog() {
        val reminders = viewModel.reminders.value ?: return

        if (reminders.isEmpty()) {
            Toast.makeText(this, "No reminders to share", Toast.LENGTH_SHORT).show()
            return
        }

        val titles = reminders.map { it.title }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select a reminder to share")
            .setItems(titles) { _, which ->
                val selected = reminders[which]
                val encodedTitle = Uri.encode(selected.title)
                val encodedDateTime = Uri.encode(selected.dateTime)
                val link = "purrgenda://add?title=$encodedTitle&datetime=$encodedDateTime"

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Tap to add this reminder:\n$link")
                }
                startActivity(Intent.createChooser(shareIntent, "Share reminder via"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
