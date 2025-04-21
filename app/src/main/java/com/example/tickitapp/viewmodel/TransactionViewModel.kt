package com.example.tickitapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.model.Transaction
import com.example.tickitapp.utils.BudgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val budgetManager = BudgetManager(application)

    private val _currentMonthExpenses = MutableLiveData<Double>()
    val currentMonthExpenses: LiveData<Double> = _currentMonthExpenses

    val allTransactions: LiveData<List<Transaction>> by lazy {
        database.transactionDao().getAllTransactionsLiveData()
    }

    init {
        updateCurrentMonthExpenses()
    }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
            updateCurrentMonthExpenses()
            if (transaction.type == "Expense") {
                budgetManager.checkBudgetStatus(_currentMonthExpenses.value ?: 0.0)
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
            updateCurrentMonthExpenses()
            budgetManager.checkBudgetStatus(_currentMonthExpenses.value ?: 0.0)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
            updateCurrentMonthExpenses()
            budgetManager.checkBudgetStatus(_currentMonthExpenses.value ?: 0.0)
        }
    }

    private fun updateCurrentMonthExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            val monthlyExpenses = transactionDao.getTransactionsByMonth(currentMonth + 1, currentYear)
                .filter { it.type == "Expense" }
                .sumOf { it.amount }
            
            _currentMonthExpenses.postValue(monthlyExpenses)
        }
    }

    fun checkDailyBudgetReminder() {
        budgetManager.checkDailyReminder()
    }

    fun setMonthlyBudget(amount: Double) {
        budgetManager.monthlyBudget = amount
        budgetManager.checkBudgetStatus(_currentMonthExpenses.value ?: 0.0)
    }

    fun getMonthlyBudget(): Double = budgetManager.monthlyBudget

    fun setReminderEnabled(enabled: Boolean) {
        budgetManager.isReminderEnabled = enabled
    }

    fun setReminderTime(hour: Int, minute: Int) {
        budgetManager.reminderTime = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
} 