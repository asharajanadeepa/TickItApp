package com.example.tickitapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tickitapp.MainActivity
import com.example.tickitapp.R
import java.text.NumberFormat
import java.util.*

class NotificationManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    companion object {
        const val CHANNEL_ID_BUDGET = "budget_alerts"
        const val CHANNEL_ID_REMINDER = "expense_reminders"
        const val NOTIFICATION_ID_BUDGET = 1
        const val NOTIFICATION_ID_REMINDER = 2
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget alerts"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to record expenses"
            }

            notificationManager.createNotificationChannels(listOf(budgetChannel, reminderChannel))
        }
    }

    fun showBudgetAlert(spent: Double, budget: Double, isNearLimit: Boolean) {
        val title = if (isNearLimit) "Approaching Budget Limit" else "Budget Exceeded"
        val message = if (isNearLimit) {
            "You've spent ${currencyFormatter.format(spent)} of your ${currencyFormatter.format(budget)} budget"
        } else {
            "You've exceeded your monthly budget of ${currencyFormatter.format(budget)}"
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BUDGET, notification)
    }

    fun showExpenseReminder() {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Expense Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_REMINDER, notification)
    }
} 