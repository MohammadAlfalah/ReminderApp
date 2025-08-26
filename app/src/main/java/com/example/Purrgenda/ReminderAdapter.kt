package com.example.Purrgenda

import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ReminderAdapter(private val reminders: MutableList<Reminder>) :
    RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.reminderTitle)
        val dateTime: TextView = itemView.findViewById(R.id.reminderDateTime)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxComplete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]

        holder.title.text = reminder.title
        holder.dateTime.text = reminder.dateTime

        holder.title.paintFlags = if (reminder.isCompleted)
            holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()


        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = reminder.isCompleted

        // ✅ Update Firestore when checkbox toggled
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            reminder.isCompleted = isChecked
            if (reminder.id.isNotBlank()) {
                FirebaseFirestore.getInstance()
                    .collection("reminders")
                    .document(reminder.id)
                    .update("isCompleted", isChecked)
            }
        }

        // ✅ Tap to Edit Reminder
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditReminderActivity::class.java).apply {
                putExtra("id", reminder.id)
                putExtra("title", reminder.title)
                putExtra("dateTime", reminder.dateTime)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = reminders.size

    fun updateData(newList: List<Reminder>) {
        reminders.clear()
        reminders.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        reminders.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItem(position: Int): Reminder = reminders[position]
}
