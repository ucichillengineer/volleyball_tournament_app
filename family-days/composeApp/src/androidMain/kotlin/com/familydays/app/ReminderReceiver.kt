package com.familydays.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_NAME) ?: return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "Birthday"
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tomorrow: $type for $name 🎉")
            .setContentText("Prepare a greeting tonight so you can wish them first thing in the morning.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(intent.getIntExtra(EXTRA_ID, name.hashCode()), notification)

        ReminderScheduler.schedule(
            context,
            name,
            type,
            intent.getIntExtra(EXTRA_MONTH, 1),
            intent.getIntExtra(EXTRA_DAY, 1),
            Calendar.getInstance().get(Calendar.YEAR) + 1
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Family Days reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}

object ReminderScheduler {
    fun scheduleAll(context: Context, csv: String) {
        LegacyCsvParser.parse(csv).forEach { event ->
            schedule(context, event.name, event.type.label, event.month, event.day)
        }
    }

    fun schedule(context: Context, name: String, type: String, month: Int, day: Int, year: Int = Calendar.getInstance().get(Calendar.YEAR)) {
        val trigger = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (trigger.timeInMillis <= System.currentTimeMillis()) {
            schedule(context, name, type, month, day, year + 1)
            return
        }
        val id = "$name-$type-$month-$day".hashCode()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_MONTH, month)
            putExtra(EXTRA_DAY, day)
            putExtra(EXTRA_ID, id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
    }
}

private const val CHANNEL_ID = "family_days_reminders"
private const val EXTRA_NAME = "name"
private const val EXTRA_TYPE = "type"
private const val EXTRA_MONTH = "month"
private const val EXTRA_DAY = "day"
private const val EXTRA_ID = "id"
