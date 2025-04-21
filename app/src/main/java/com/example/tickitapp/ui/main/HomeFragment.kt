package com.example.tickitapp.ui.main

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tickitapp.R
import com.example.tickitapp.adapter.TransactionAdapter
import com.example.tickitapp.data.AppDatabase
import com.example.tickitapp.databinding.DialogEditTransactionBinding
import com.example.tickitapp.databinding.FragmentHomeBinding
import com.example.tickitapp.model.Transaction
import com.example.tickitapp.utils.BackupManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.android.material.snackbar.Snackbar
import java.util.*

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var transactionAdapter: TransactionAdapter? = null
    private lateinit var database: AppDatabase
    private lateinit var backupManager: BackupManager
    private val TAG = "HomeFragment"
    private val categories: Array<String> by lazy {
        resources.getStringArray(R.array.categories)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                backupData()
                true
            }
            R.id.action_restore -> {
                showRestoreDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")
        
        try {
            _binding = FragmentHomeBinding.bind(view)
            Log.d(TAG, "View binding successful")

            // Initialize database and backup manager
            database = AppDatabase.getDatabase(requireContext())
            backupManager = BackupManager(requireContext())

            // Set up category dropdown
            Log.d(TAG, "Categories loaded: ${categories.size} items")
            
            val dropdownAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.categorySpinner.apply {
                setAdapter(dropdownAdapter)
                setText(categories[0], false)
                threshold = 1
            }
            Log.d(TAG, "Category dropdown setup complete")

            // Set up RecyclerView with empty list initially
            transactionAdapter = TransactionAdapter(
                emptyList(),
                onEditClick = { transaction -> showEditDialog(transaction) },
                onDeleteClick = { transaction -> showDeleteConfirmation(transaction) }
            )
            binding.transactionList.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = transactionAdapter
            }
            Log.d(TAG, "RecyclerView set up")

            // Save transaction
            binding.saveButton.setOnClickListener {
                val title = binding.titleInput.text.toString().trim()
                val amountStr = binding.amountInput.text.toString().trim()
                val selectedCategory = binding.categorySpinner.text.toString()
                val isIncome = binding.incomeRadio.isChecked

                if (title.isEmpty()) {
                    binding.titleInputLayout.error = "Please enter a title"
                    return@setOnClickListener
                }

                if (amountStr.isEmpty()) {
                    binding.amountInputLayout.error = "Please enter an amount"
                    return@setOnClickListener
                }

                val amount = try {
                    amountStr.toDouble()
                } catch (e: NumberFormatException) {
                    binding.amountInputLayout.error = "Please enter a valid amount"
                    return@setOnClickListener
                }

                if (amount <= 0) {
                    binding.amountInputLayout.error = "Amount must be greater than 0"
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    try {
                        val transaction = Transaction(
                            title = title,
                            amount = amount,
                            category = selectedCategory,
                            date = Date(System.currentTimeMillis()),
                            isIncome = isIncome
                        )
                        
                        withContext(Dispatchers.IO) {
                            database.transactionDao().insertTransaction(transaction)
                        }

                        // Update UI
                        clearForm()
                        
                        // Show success message
                        Snackbar.make(
                            binding.root,
                            "${if (isIncome) "Income" else "Expense"} saved successfully",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving transaction: ${e.message}", e)
                        // Show error message
                        Snackbar.make(
                            binding.root,
                            "Error saving transaction: ${e.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // Observe transactions
            lifecycleScope.launch {
                try {
                    database.transactionDao().getAllTransactions().collect { transactions ->
                        Log.d(TAG, "Received ${transactions.size} transactions from DB")
                        transactionAdapter?.updateTransactions(transactions)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error observing transactions: ${e.message}", e)
                    Snackbar.make(
                        binding.root,
                        "Error loading transactions: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated: ${e.message}", e)
        }
    }

    private fun showEditDialog(transaction: Transaction) {
        val dialogBinding = DialogEditTransactionBinding.inflate(layoutInflater)
        
        // Set up category spinner
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dialogBinding.categorySpinner.adapter = spinnerAdapter

        // Set initial values
        dialogBinding.titleInput.setText(transaction.title)
        dialogBinding.amountInput.setText(transaction.amount.toString())
        
        // Set category selection
        val categoryPosition = categories.indexOf(transaction.category)
        if (categoryPosition != -1) {
            dialogBinding.categorySpinner.setSelection(categoryPosition)
        }
        
        // Set transaction type
        if (transaction.isIncome) {
            dialogBinding.incomeRadio.isChecked = true
        } else {
            dialogBinding.expenseRadio.isChecked = true
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Transaction")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val title = dialogBinding.titleInput.text.toString()
                val amount = dialogBinding.amountInput.text.toString()
                val category = dialogBinding.categorySpinner.selectedItem?.toString() ?: categories[0]
                val isIncome = dialogBinding.incomeRadio.isChecked

                if (title.isEmpty() || amount.isEmpty()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val amountValue = amount.toDoubleOrNull()
                if (amountValue == null || amountValue <= 0) {
                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedTransaction = transaction.copy(
                    id = transaction.id,  // Preserve the original ID
                    title = title,
                    amount = amountValue,
                    category = category,
                    isIncome = isIncome,
                    date = transaction.date  // Preserve the original date
                )

                lifecycleScope.launch {
                    try {
                        database.transactionDao().updateTransaction(updatedTransaction)
                        Log.d(TAG, "Transaction updated successfully: ${updatedTransaction.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating transaction: ${e.message}", e)
                        Toast.makeText(context, "Error updating transaction", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            database.transactionDao().deleteTransaction(transaction)
                        }
                        Snackbar.make(
                            binding.root,
                            "Transaction deleted successfully",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting transaction: ${e.message}", e)
                        Snackbar.make(
                            binding.root,
                            "Error deleting transaction: ${e.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        binding.titleInput.text?.clear()
        binding.amountInput.text?.clear()
        // Reset category spinner with adapter
        val dropdownAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.categorySpinner.apply {
            setAdapter(dropdownAdapter)
            setText(categories[0], false)
            threshold = 1
        }
        binding.expenseRadio.isChecked = true
    }

    private fun backupData() {
        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    database.transactionDao().getAllTransactionsList()
                }
                val backupPath = backupManager.exportData(transactions)
                Snackbar.make(
                    binding.root,
                    "Backup saved to: $backupPath",
                    Snackbar.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed: ${e.message}", e)
                Snackbar.make(
                    binding.root,
                    "Backup failed: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showRestoreDialog() {
        val backupFiles = backupManager.getBackupFiles()
        if (backupFiles.isEmpty()) {
            Snackbar.make(
                binding.root,
                "No backup files found",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val fileNames = backupFiles.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Backup to Restore")
            .setItems(fileNames) { _, which ->
                restoreData(fileNames[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreData(fileName: String) {
        lifecycleScope.launch {
            try {
                val transactions = backupManager.importData(fileName)
                withContext(Dispatchers.IO) {
                    database.transactionDao().deleteAllTransactions()
                    transactions.forEach { transaction ->
                        database.transactionDao().insertTransaction(transaction)
                    }
                }
                Snackbar.make(
                    binding.root,
                    "Data restored successfully",
                    Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed: ${e.message}", e)
                Snackbar.make(
                    binding.root,
                    "Restore failed: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        transactionAdapter = null
    }
}