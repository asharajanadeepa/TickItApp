package com.example.tickitapp.ui.budget

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tickitapp.R
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.databinding.FragmentBudgetBinding
import com.example.tickitapp.utils.NotificationUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BudgetFragment : Fragment(R.layout.fragment_budget) {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBudgetBinding.bind(view)
        database = AppDatabase.getDatabase(requireContext())

        // Load saved budget
        val sharedPrefs = requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val savedBudget = sharedPrefs.getFloat("monthly_budget", 0f)
        if (savedBudget > 0) {
            binding.budgetInput.setText(String.format("%.2f", savedBudget.toDouble()))
        }

        // Observe transactions
        lifecycleScope.launch {
            var totalIncome = 0.0
            var totalExpenses = 0.0
            val currentBudget = binding.budgetInput.text.toString().toDoubleOrNull() ?: 0.0

            // Observe income and expenses
            launch {
                database.transactionDao().getAllTransactions().collectLatest { transactions ->
                    totalIncome = 0.0
                    totalExpenses = 0.0

                    // Calculate totals
                    transactions.forEach { transaction ->
                        if (transaction.isIncome) {
                            totalIncome += transaction.amount
                        } else {
                            totalExpenses += transaction.amount
                        }
                    }

                    updateBudgetStatus(currentBudget, totalIncome, totalExpenses)
                }
            }
        }

        binding.saveBudgetButton.setOnClickListener {
            val budgetInput = binding.budgetInput.text.toString()
            if (budgetInput.isEmpty()) {
                Toast.makeText(context, "Please enter a budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val budget = budgetInput.toDoubleOrNull() ?: 0.0
            if (budget <= 0) {
                Toast.makeText(context, "Enter a valid budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save budget to SharedPreferences
            sharedPrefs.edit().putFloat("monthly_budget", budget.toFloat()).apply()
            
            Toast.makeText(context, "Budget saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBudgetStatus(budget: Double, income: Double, expenses: Double) {
        // Update income status
        binding.incomeStatus.text = "Total Income: $${String.format("%.2f", income)}"
        
        // Update expense status
        binding.expenseStatus.text = "Total Expenses: $${String.format("%.2f", expenses)}"
        
        // Update budget status
        binding.budgetStatus.text = "Budget: $${String.format("%.2f", budget)}"
        
        // Calculate remaining budget
        val remaining = budget - expenses
        binding.remainingBudget.text = "Remaining: $${String.format("%.2f", remaining)}"
        binding.remainingBudget.setTextColor(
            if (remaining < 0) requireContext().getColor(R.color.expense_red)
            else requireContext().getColor(R.color.income_green)
        )

        // Calculate savings
        val savings = income - expenses
        binding.savingsStatus.text = "Net Savings: $${String.format("%.2f", savings)}"
        binding.savingsStatus.setTextColor(
            if (savings < 0) requireContext().getColor(R.color.expense_red)
            else requireContext().getColor(R.color.income_green)
        )

        if (expenses > budget && budget > 0) {
            binding.budgetWarning.visibility = View.VISIBLE
            binding.budgetWarning.text = "Warning: Budget Exceeded by $${String.format("%.2f", -remaining)}!"
            NotificationUtils.showBudgetNotification(requireContext(), budget, expenses)
        } else {
            binding.budgetWarning.visibility = View.GONE
        }

        // Update progress bars
        val budgetProgress = if (budget > 0) ((expenses / budget) * 100).toInt() else 0
        binding.budgetProgress.progress = budgetProgress.coerceIn(0, 100)
        binding.budgetProgress.setIndicatorColor(
            if (budgetProgress > 100) requireContext().getColor(R.color.expense_red)
            else requireContext().getColor(R.color.income_green)
        )

        val savingsProgress = if (income > 0) ((savings / income) * 100).toInt() else 0
        binding.savingsProgress.progress = savingsProgress.coerceIn(0, 100)
        binding.savingsProgress.setIndicatorColor(
            if (savingsProgress < 0) requireContext().getColor(R.color.expense_red)
            else requireContext().getColor(R.color.income_green)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}