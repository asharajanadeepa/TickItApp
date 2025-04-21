package com.example.tickitapp.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.tickitapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val transactionDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    fun exportAsText(transactions: List<Transaction>): String {
        try {
            val fileName = "tickitapp_export_${dateFormat.format(Date())}.txt"
            val stringBuilder = StringBuilder()
            
            // Add header
            stringBuilder.append("TickItApp Transaction Export\n")
            stringBuilder.append("Generated on: ${dateFormat.format(Date())}\n")
            stringBuilder.append("-".repeat(50)).append("\n\n")

            var totalIncome = 0.0
            var totalExpenses = 0.0

            // Add transactions
            transactions.forEach { transaction ->
                stringBuilder.append("Date: ${transactionDateFormat.format(transaction.date)}\n")
                stringBuilder.append("Title: ${transaction.title}\n")
                stringBuilder.append("Category: ${transaction.category}\n")
                stringBuilder.append("Amount: ${currencyFormatter.format(transaction.amount)}\n")
                stringBuilder.append("Type: ${if (transaction.isIncome) "Income" else "Expense"}\n")
                stringBuilder.append("-".repeat(30)).append("\n\n")

                if (transaction.isIncome) totalIncome += transaction.amount
                else totalExpenses += transaction.amount
            }

            // Add summary
            stringBuilder.append("\nSUMMARY\n")
            stringBuilder.append("-".repeat(20)).append("\n")
            stringBuilder.append("Total Income: ${currencyFormatter.format(totalIncome)}\n")
            stringBuilder.append("Total Expenses: ${currencyFormatter.format(totalExpenses)}\n")
            stringBuilder.append("Balance: ${currencyFormatter.format(totalIncome - totalExpenses)}\n")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw IOException("Failed to create file")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(stringBuilder.toString().toByteArray())
            } ?: throw IOException("Failed to write file")

            return fileName
        } catch (e: IOException) {
            throw IOException("Failed to export text file: ${e.message}")
        }
    }

    fun exportData(transactions: List<Transaction>): String {
        try {
            val fileName = "tickitapp_backup_${dateFormat.format(Date())}.json"
            val jsonData = gson.toJson(transactions)
            
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
                stream.write(jsonData.toByteArray())
            }
            
            val file = File(context.filesDir, fileName)
            return file.absolutePath
        } catch (e: IOException) {
            throw IOException("Failed to export data: ${e.message}")
        }
    }

    fun importData(fileName: String): List<Transaction> {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                throw IOException("Backup file not found")
            }

            val jsonData = file.readText()
            val type = object : TypeToken<List<Transaction>>() {}.type
            return gson.fromJson(jsonData, type)
        } catch (e: Exception) {
            throw IOException("Failed to import data: ${e.message}")
        }
    }

    fun getBackupFiles(): List<File> {
        return context.filesDir.listFiles { file ->
            file.name.startsWith("tickitapp_backup_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }
} 