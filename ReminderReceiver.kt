package com.disciplinequest.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("discipline","Discipline reminders",NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
        val n = NotificationCompat.Builder(context,"discipline")
            .setSmallIcon(com.disciplinequest.app.R.drawable.app_icon)
            .setContentTitle("DISCIPLINE QUEST")
            .setContentText("Check today's mission and tasks.")
            .setAutoCancel(true)
            .build()
        manager.notify(9001,n)
    }
}
