package com.example.tickitapp.ui.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tickitapp.R
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.databinding.FragmentAnalysisBinding
import com.example.tickitapp.model.Transaction
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Other")
    private val TAG = "AnalysisFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            _binding = FragmentAnalysisBinding.bind(view)
            database = AppDatabase.getDatabase(requireContext())

            setupPieChart()
            observeTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
            showError("Failed to initialize analysis view")
        }
    }

    private fun setupPieChart() {
        try {
            binding.pieChart.apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(Color.WHITE)
                setDrawEntryLabels(true)
                setEntryLabelTextSize(12f)
                setEntryLabelColor(Color.BLACK)
                legend.isEnabled = true
                legend.textSize = 12f
                legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                legend.setDrawInside(false)
                setUsePercentValues(true)
                setCenterText("Expenses by Category")
                setCenterTextSize(16f)
                setTouchEnabled(true)
                setHighlightPerTapEnabled(true)
                setDrawEntryLabels(false)
                setExtraOffsets(20f, 20f, 20f, 20f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupPieChart", e)
            showError("Failed to setup chart")
        }
    }

    private fun observeTransactions() {
        if (!isAdded || isDetached) return

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.transactionDao().getAllTransactions().collectLatest { transactions ->
                        if (isAdded && !isDetached) {
                            withContext(Dispatchers.Main) {
                                updateUI(transactions)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in observeTransactions", e)
                showError("Failed to load transactions")
            }
        }
    }

    private fun updateUI(transactions: List<Transaction>) {
        if (!isAdded || isDetached) return

        try {
            var totalIncome = 0.0
            var totalExpenses = 0.0
            val categoryExpenses = mutableMapOf<String, Double>()

            // Initialize all categories with 0.0
            categories.forEach { category ->
                categoryExpenses[category] = 0.0
            }

            // Process transactions
            transactions.forEach { transaction ->
                if (transaction.isIncome) {
                    totalIncome += transaction.amount
                } else {
                    totalExpenses += transaction.amount
                    val category = if (transaction.category in categories) transaction.category else "Other"
                    categoryExpenses[category] = (categoryExpenses[category] ?: 0.0) + transaction.amount
                }
            }

            // Update text views
            binding.totalIncomeText.text = currencyFormat.format(totalIncome)
            binding.totalExpensesText.text = currencyFormat.format(totalExpenses)
            binding.balanceText.text = currencyFormat.format(totalIncome - totalExpenses)

            // Update category text views
            binding.foodExpensesText.text = currencyFormat.format(categoryExpenses["Food"])
            binding.transportExpensesText.text = currencyFormat.format(categoryExpenses["Transport"])
            binding.billsExpensesText.text = currencyFormat.format(categoryExpenses["Bills"])
            binding.entertainmentExpensesText.text = currencyFormat.format(categoryExpenses["Entertainment"])
            binding.otherExpensesText.text = currencyFormat.format(categoryExpenses["Other"])

            // Update pie chart with category expenses
            updatePieChart(categoryExpenses)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateUI", e)
            showError("Failed to update display")
        }
    }

    private fun updatePieChart(categoryExpenses: Map<String, Double>) {
        try {
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()
            
            // Add non-zero categories
            categoryExpenses.forEach { (category, amount) ->
                if (amount > 0) {
                    entries.add(PieEntry(amount.toFloat(), category))
                    colors.add(getCategoryColor(category))
                }
            }

            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "Expenses by Category").apply {
                    this.colors = colors
                    valueTextSize = 14f
                    valueTextColor = Color.BLACK
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return currencyFormat.format(value.toDouble())
                        }
                    }
                    setDrawValues(true)
                    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                }

                binding.pieChart.data = PieData(dataSet).apply {
                    setValueTextSize(14f)
                    setValueTextColor(Color.BLACK)
                }
                binding.pieChart.invalidate()
            } else {
                binding.pieChart.clear()
                binding.pieChart.invalidate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updatePieChart", e)
            showError("Failed to update chart")
        }
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "Food" -> Color.rgb(76, 175, 80)      // Green
            "Transport" -> Color.rgb(33, 150, 243) // Blue
            "Bills" -> Color.rgb(156, 39, 176)    // Purple
            "Entertainment" -> Color.rgb(255, 152, 0) // Orange
            else -> Color.rgb(158, 158, 158)      // Grey for Other
        }
    }

    private fun showError(message: String) {
        if (isAdded && !isDetached) {
            try {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing error message", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 