package com.example.tickitapp.ui.budget

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tickitapp.R
import com.example.tickitapp.databinding.FragmentBudgetBinding
import com.example.tickitapp.utils.NotificationUtils

class BudgetFragment : Fragment(R.layout.fragment_budget) {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private var budget: Double = 0.0
    private var totalSpending: Double = 0.0 // Sample, replace with real data

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBudgetBinding.bind(view)

        binding.saveBudgetButton.setOnClickListener {
            val budgetInput = binding.budgetInput.text.toString()
            if (budgetInput.isEmpty()) {
                Toast.makeText(context, "Please enter a budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            budget = budgetInput.toDoubleOrNull() ?: 0.0
            if (budget <= 0) {
                Toast.makeText(context, "Enter a valid budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateBudgetStatus()
        }

        // Sample spending (replace with real transaction sum)
        totalSpending = 150.0
        updateBudgetStatus()
    }

    private fun updateBudgetStatus() {
        binding.budgetStatus.text = "Spending: $${String.format("%.2f", totalSpending)} / $${String.format("%.2f", budget)}"
        if (totalSpending > budget && budget > 0) {
            binding.budgetWarning.visibility = View.VISIBLE
            NotificationUtils.showBudgetNotification(requireContext(), budget, totalSpending)
        } else {
            binding.budgetWarning.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}