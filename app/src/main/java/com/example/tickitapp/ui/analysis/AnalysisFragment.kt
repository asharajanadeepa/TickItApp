package com.example.tickitapp.ui.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tickitapp.R
import com.example.tickitapp.databinding.FragmentAnalysisBinding
import com.example.tickitapp.model.Transaction
import com.example.tickitapp.viewmodel.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.*

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionViewModel
    private val categories: Array<String> by lazy {
        resources.getStringArray(R.array.categories)
    }
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[TransactionViewModel::class.java]

        setupMainPieChart()
        setupCategoryPieChart()
        observeTransactions()
    }

    private fun setupMainPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Income vs\nExpenses"
            setUsePercentValues(true)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000, Easing.EaseInOutQuad)
        }
    }

    private fun setupCategoryPieChart() {
        binding.categoryPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Expenses by\nCategory"
            setUsePercentValues(true)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000, Easing.EaseInOutQuad)
        }
    }

    private fun observeTransactions() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions != null) {
                var totalIncome = 0.0
                var totalExpenses = 0.0
                val categoryAmounts = mutableMapOf<String, Double>()
                categories.forEach { categoryAmounts[it] = 0.0 }

                transactions.forEach { transaction ->
                    if (transaction.isIncome) {
                        totalIncome += transaction.amount
                    } else {
                        totalExpenses += transaction.amount
                        categoryAmounts[transaction.category] = 
                            categoryAmounts[transaction.category]!! + transaction.amount
                    }
                }

                updateMainChart(totalIncome, totalExpenses)
                updateCategoryChart(categoryAmounts)
                updateTextViews(totalIncome, totalExpenses)
                updateCategoryAmounts(categoryAmounts)
            }
        }
    }

    private fun updateMainChart(totalIncome: Double, totalExpenses: Double) {
        val entries = mutableListOf<PieEntry>()
        
        if (totalIncome > 0) {
            entries.add(PieEntry(totalIncome.toFloat(), "Income"))
        }
        if (totalExpenses > 0) {
            entries.add(PieEntry(totalExpenses.toFloat(), "Expenses"))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ColorTemplate.rgb("#4CAF50"),  // Green for income
            ColorTemplate.rgb("#F44336")   // Red for expenses
        )
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)

        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }

    private fun updateCategoryChart(categoryAmounts: Map<String, Double>) {
        val entries = mutableListOf<PieEntry>()
        
        categoryAmounts.forEach { (category, amount) ->
            if (amount > 0) {
                entries.add(PieEntry(amount.toFloat(), category))
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)

        binding.categoryPieChart.data = data
        binding.categoryPieChart.invalidate()
    }

    private fun updateTextViews(totalIncome: Double, totalExpenses: Double) {
        binding.totalIncomeText.text = currencyFormatter.format(totalIncome)
        binding.totalExpensesText.text = currencyFormatter.format(totalExpenses)
        binding.balanceText.text = currencyFormatter.format(totalIncome - totalExpenses)
    }

    private fun updateCategoryAmounts(categoryAmounts: Map<String, Double>) {
        // Update individual category TextViews
        binding.foodExpensesText.text = "Food: ${currencyFormatter.format(categoryAmounts["Food"] ?: 0.0)}"
        binding.transportExpensesText.text = "Transport: ${currencyFormatter.format(categoryAmounts["Transport"] ?: 0.0)}"
        binding.billsExpensesText.text = "Bills: ${currencyFormatter.format(categoryAmounts["Bills"] ?: 0.0)}"
        binding.entertainmentExpensesText.text = "Entertainment: ${currencyFormatter.format(categoryAmounts["Entertainment"] ?: 0.0)}"
        binding.otherExpensesText.text = "Other: ${currencyFormatter.format(categoryAmounts["Other"] ?: 0.0)}"

        // Update the dynamic list
        binding.categoryAmounts.removeAllViews()
        
        categoryAmounts.forEach { (category, amount) ->
            if (amount > 0) {
                val itemView = layoutInflater.inflate(
                    android.R.layout.simple_list_item_2,
                    binding.categoryAmounts,
                    false
                )
                
                itemView.findViewById<TextView>(android.R.id.text1).apply {
                    text = category
                    textSize = 16f
                }
                
                itemView.findViewById<TextView>(android.R.id.text2).apply {
                    text = currencyFormatter.format(amount)
                    textSize = 14f
                }

                binding.categoryAmounts.addView(itemView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}