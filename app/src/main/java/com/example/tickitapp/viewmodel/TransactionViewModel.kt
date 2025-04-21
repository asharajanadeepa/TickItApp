package com.example.tickitapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.model.Transaction
import com.example.tickitapp.utils.BudgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val budgetManager = BudgetManager(application)

    private val _currentMonthExpenses = MutableLiveData<Double>()
    val currentMonthExpenses: LiveData<Double> = _currentMonthExpenses

    private val _currentMonthIncome = MutableLiveData<Double>()
    val currentMonthIncome: LiveData<Double> = _currentMonthIncome

    private val _monthlyBudget = MutableLiveData<Double>()
    val monthlyBudget: LiveData<Double> = _monthlyBudget

    private val _budgetStatus = MutableLiveData<BudgetStatus>()
    val budgetStatus: LiveData<BudgetStatus> = _budgetStatus

    private val _allTransactions = transactionDao.getAllTransactions()
    val allTransactions: LiveData<List<Transaction>> = _allTransactions.asLiveData(viewModelScope.coroutineContext)

    init {
        updateCurrentMonthExpenses()
        updateCurrentMonthIncome()
        _monthlyBudget.value = budgetManager.monthlyBudget
    }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
            updateCurrentMonthExpenses()
            updateCurrentMonthIncome()
            if (!transaction.isIncome) {
                checkBudgetStatus()
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
            updateCurrentMonthExpenses()
            updateCurrentMonthIncome()
            checkBudgetStatus()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
            updateCurrentMonthExpenses()
            updateCurrentMonthIncome()
            checkBudgetStatus()
        }
    }

    private fun updateCurrentMonthExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            val transactions = transactionDao.getAllTransactionsList()
            val monthlyExpenses = transactions
                .filter { transaction ->
                    val transactionCalendar = Calendar.getInstance().apply {
                        time = transaction.date
                    }
                    transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear &&
                    !transaction.isIncome
                }
                .sumOf { it.amount }
            
            _currentMonthExpenses.postValue(monthlyExpenses)
            checkBudgetStatus()
        }
    }

    private fun updateCurrentMonthIncome() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            val transactions = transactionDao.getAllTransactionsList()
            val monthlyIncome = transactions
                .filter { transaction ->
                    val transactionCalendar = Calendar.getInstance().apply {
                        time = transaction.date
                    }
                    transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                    transactionCalendar.get(Calendar.YEAR) == currentYear &&
                    transaction.isIncome
                }
                .sumOf { it.amount }
            
            _currentMonthIncome.postValue(monthlyIncome)
        }
    }

    fun setMonthlyBudget(amount: Double) {
        budgetManager.monthlyBudget = amount
        _monthlyBudget.value = amount
        checkBudgetStatus()
    }

    fun getMonthlyBudget(): Double = budgetManager.monthlyBudget

    private fun checkBudgetStatus() {
        val expenses = _currentMonthExpenses.value ?: 0.0
        val budget = _monthlyBudget.value ?: 0.0
        
        val status = when {
            budget <= 0 -> BudgetStatus.NOT_SET
            expenses >= budget -> BudgetStatus.EXCEEDED
            expenses >= budget * 0.9 -> BudgetStatus.WARNING
            else -> BudgetStatus.GOOD
        }
        
        _budgetStatus.postValue(status)
        budgetManager.checkBudgetStatus(expenses)
    }

    fun setReminderEnabled(enabled: Boolean) {
        budgetManager.isReminderEnabled = enabled
    }

    fun setReminderTime(hour: Int, minute: Int) {
        budgetManager.reminderTime = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    fun checkDailyBudgetReminder() {
        budgetManager.checkDailyReminder()
    }

    enum class BudgetStatus {
        NOT_SET,
        GOOD,
        WARNING,
        EXCEEDED
    }
} 