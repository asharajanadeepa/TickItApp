package com.example.tickitapp.ui.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tickitapp.R
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.databinding.FragmentAnalysisBinding
import com.example.tickitapp.model.Transaction
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val categories = listOf("Food", "Transport", "Bills", "Entertainment", "Other")
    private val tag = "AnalysisFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            _binding = FragmentAnalysisBinding.bind(view)
            database = AppDatabase.getDatabase(requireContext())

            setupPieChart()
            observeTransactions()
        } catch (e: Exception) {
            Log.e(tag, "Error in onViewCreated", e)
            showError(getString(R.string.error_init_analysis))
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
                setCenterText(getString(R.string.expenses_by_category))
                setCenterTextSize(16f)
                setTouchEnabled(true)
                setHighlightPerTapEnabled(true)
                setDrawEntryLabels(false)
                setExtraOffsets(20f, 20f, 20f, 20f)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in setupPieChart", e)
            showError(getString(R.string.error_init_analysis))
        }
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            try {
                val transactionsFlow: Flow<List<Transaction>> = database.transactionDao().getAllTransactions()
                transactionsFlow
                    .catch { e ->
                        Log.e(tag, "Error collecting transactions", e)
                        showError(getString(R.string.error_loading_transactions))
                    }
                    .collectLatest { transactions ->
                        if (isAdded && !isDetached) {
                            updateUI(transactions)
                        }
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in observeTransactions", e)
                showError(getString(R.string.error_loading_transactions))
            }
        }
    }

    private fun updateUI(transactions: List<Transaction>) {
        if (!isAdded || isDetached) return

        try {
            var totalIncome = 0.0
            var totalExpenses = 0.0
            val categoryExpenses = categories.associateWith { 0.0 }.toMutableMap()

            // Process transactions
            for (transaction in transactions) {
                if (transaction.isIncome) {
                    totalIncome += transaction.amount
                } else {
                    totalExpenses += transaction.amount
                    val category = transaction.category.takeIf { it in categories } ?: "Other"
                    categoryExpenses[category] = (categoryExpenses[category] ?: 0.0) + transaction.amount
                }
            }

            // Update text views with currency formatted values
            binding.apply {
                totalIncomeText.text = currencyFormat.format(totalIncome)
                totalExpensesText.text = currencyFormat.format(totalExpenses)
                balanceText.text = currencyFormat.format(totalIncome - totalExpenses)
            }

            // Update category amounts in the LinearLayout
            updateCategoryAmounts(categoryExpenses)

            // Update pie chart with category expenses
            updatePieChartData(categoryExpenses)
        } catch (e: Exception) {
            Log.e(tag, "Error in updateUI", e)
            showError(getString(R.string.error_update_display))
        }
    }

    private fun updateCategoryAmounts(categoryExpenses: Map<String, Double>) {
        binding.categoryAmounts.removeAllViews()
        
        categoryExpenses.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .forEach { (category, amount) ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_category_amount,
                    binding.categoryAmounts,
                    false
                )
                
                itemView.findViewById<TextView>(R.id.categoryName).text = category
                itemView.findViewById<TextView>(R.id.categoryAmount).text = currencyFormat.format(amount)
                
                binding.categoryAmounts.addView(itemView)
            }
    }

    private fun updatePieChartData(categoryExpenses: Map<String, Double>) {
        val entries = categoryExpenses
            .filter { it.value > 0 }
            .map { (category, amount) -> PieEntry(amount.toFloat(), category) }

        if (entries.isEmpty()) {
            binding.pieChart.setNoDataText(getString(R.string.no_expenses_recorded))
            binding.pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, getString(R.string.expenses_by_category))
        dataSet.colors = entries.map { getCategoryColor(it.label) }
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return currencyFormat.format(value.toDouble())
            }
        }
        dataSet.setDrawValues(true)
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        val pieData = PieData(dataSet)
        pieData.setValueTextSize(14f)
        pieData.setValueTextColor(Color.BLACK)

        binding.pieChart.data = pieData
        binding.pieChart.invalidate()
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
                Log.e(tag, "Error showing error message", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 