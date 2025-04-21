package com.example.tickitapp.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class BudgetManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    private val notificationManager = NotificationManager(context)

    companion object {
        const val KEY_MONTHLY_BUDGET = "monthly_budget"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_TIME = "reminder_time"
        const val KEY_LAST_NOTIFICATION_DATE = "last_notification_date"
        const val BUDGET_WARNING_THRESHOLD = 0.8 // 80% of budget
    }

    var monthlyBudget: Double
        get() = prefs.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_MONTHLY_BUDGET, value.toFloat()).apply()

    var isReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var reminderTime: String
        get() = prefs.getString(KEY_REMINDER_TIME, "20:00") ?: "20:00"
        set(value) = prefs.edit().putString(KEY_REMINDER_TIME, value).apply()

    private var lastNotificationDate: Long
        get() = prefs.getLong(KEY_LAST_NOTIFICATION_DATE, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFICATION_DATE, value).apply()

    fun checkBudgetStatus(currentSpending: Double) {
        if (monthlyBudget <= 0) return

        when {
            currentSpending > monthlyBudget -> {
                notificationManager.showBudgetAlert(currentSpending, monthlyBudget, false)
            }
            currentSpending >= monthlyBudget * BUDGET_WARNING_THRESHOLD -> {
                notificationManager.showBudgetAlert(currentSpending, monthlyBudget, true)
            }
        }
    }

    fun checkDailyReminder() {
        if (!isReminderEnabled) return

        val currentDate = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, reminderTime.split(":")[0].toInt())
            set(Calendar.MINUTE, reminderTime.split(":")[1].toInt())
        }

        val lastNotification = Calendar.getInstance().apply {
            timeInMillis = lastNotificationDate
        }

        if (currentDate.get(Calendar.DAY_OF_YEAR) != lastNotification.get(Calendar.DAY_OF_YEAR) ||
            currentDate.get(Calendar.YEAR) != lastNotification.get(Calendar.YEAR)) {
            notificationManager.showExpenseReminder()
            lastNotificationDate = System.currentTimeMillis()
        }
    }
} 