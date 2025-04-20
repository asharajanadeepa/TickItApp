package com.example.tickitapp.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tickitapp.R
import com.example.tickitapp.adapter.TransactionAdapter
import com.example.tickitapp.databinding.FragmentHomeBinding
import com.example.tickitapp.model.Transaction
import java.util.Date

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val transactions = mutableListOf<Transaction>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Set up Spinner
        val categories = resources.getStringArray(R.array.categories)
        binding.categorySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Set up RecyclerView
        binding.transactionList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TransactionAdapter(transactions)
        }

        // Save transaction
        binding.saveButton.setOnClickListener {
            val title = binding.titleInput.text.toString()
            val amount = binding.amountInput.text.toString()
            val category = binding.categorySpinner.selectedItem.toString()
            val isIncome = binding.incomeRadio.isChecked

            if (title.isEmpty() || amount.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amountValue = amount.toDoubleOrNull()
            if (amountValue == null || amountValue <= 0) {
                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = Transaction(title, amountValue, category, Date(), isIncome)
            transactions.add(transaction)
            binding.transactionList.adapter?.notifyItemInserted(transactions.size - 1)
            clearForm()
        }
    }

    private fun clearForm() {
        binding.titleInput.text.clear()
        binding.amountInput.text.clear()
        binding.categorySpinner.setSelection(0)
        binding.incomeRadio.isChecked = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}