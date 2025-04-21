package com.example.tickitapp.utils

import android.content.Context
import com.example.tickitapp.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)

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