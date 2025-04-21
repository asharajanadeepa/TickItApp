package com.example.tickitapp.ui.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tickitapp.R
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.databinding.FragmentBudgetBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class BudgetFragment : Fragment(R.layout.fragment_budget) {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private var currentBudget = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            _binding = FragmentBudgetBinding.bind(view)
            database = AppDatabase.getDatabase(requireContext())
            
            setupBudgetButton()
            observeTransactions()
        } catch (e: Exception) {
            showError("Failed to initialize budget view")
        }
    }

    private fun setupBudgetButton() {
        binding.saveBudgetButton.setOnClickListener {
            val budgetText = binding.budgetInput.text.toString()
            if (budgetText.isNotEmpty()) {
                try {
                    val budget = budgetText.toDouble()
                    currentBudget = budget
                    updateBudgetUI()
                    binding.budgetInput.text?.clear()
                    showMessage("Budget updated successfully")
                } catch (e: NumberFormatException) {
                    showError("Please enter a valid number")
                }
            } else {
                showError("Please enter a budget amount")
            }
        }
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.transactionDao().getAllTransactions().collectLatest { transactions ->
                        withContext(Dispatchers.Main) {
                            var totalIncome = 0.0
                            var totalExpenses = 0.0

                            transactions.forEach { transaction ->
                                if (transaction.isIncome) {
                                    totalIncome += transaction.amount
                                } else {
                                    totalExpenses += transaction.amount
                                }
                            }

                            updateUI(totalIncome, totalExpenses)
                        }
                    }
                }
            } catch (e: Exception) {
                showError("Failed to load transactions")
            }
        }
    }

    private fun updateUI(totalIncome: Double, totalExpenses: Double) {
        try {
            binding.incomeStatus.text = "Total Income: ${currencyFormat.format(totalIncome)}"
            binding.expenseStatus.text = "Total Expenses: ${currencyFormat.format(totalExpenses)}"
            
            val netSavings = totalIncome - totalExpenses
            binding.savingsStatus.text = "Net Savings: ${currencyFormat.format(netSavings)}"
            
            updateBudgetUI(totalExpenses)
        } catch (e: Exception) {
            showError("Failed to update display")
        }
    }

    private fun updateBudgetUI(totalExpenses: Double = 0.0) {
        binding.budgetStatus.text = "Budget: ${currencyFormat.format(currentBudget)}"
        
        val remaining = currentBudget - totalExpenses
        binding.remainingBudget.text = "Remaining: ${currencyFormat.format(remaining)}"
        
        if (currentBudget > 0) {
            val progress = ((totalExpenses / currentBudget) * 100).toInt().coerceIn(0, 100)
            binding.budgetProgress.progress = progress
            
            binding.budgetWarning.visibility = if (totalExpenses > currentBudget) View.VISIBLE else View.GONE
        } else {
            binding.budgetProgress.progress = 0
            binding.budgetWarning.visibility = View.GONE
        }
        
        // Update savings progress
        if (currentBudget > 0) {
            val savingsProgress = ((remaining / currentBudget) * 100).toInt().coerceIn(0, 100)
            binding.savingsProgress.progress = savingsProgress
        } else {
            binding.savingsProgress.progress = 0
        }
    }

    private fun showError(message: String) {
        if (isAdded && !isDetached) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showMessage(message: String) {
        if (isAdded && !isDetached) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 