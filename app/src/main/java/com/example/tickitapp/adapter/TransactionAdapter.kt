package com.example.tickitapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tickitapp.R
import com.example.tickitapp.model.Transaction
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.transaction_title)
        val amount: TextView = itemView.findViewById(R.id.transaction_amount)
        val category: TextView = itemView.findViewById(R.id.transaction_category)
        val date: TextView = itemView.findViewById(R.id.transaction_date)
        val typeIcon: ImageView = itemView.findViewById(R.id.transaction_type_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.title.text = transaction.title
        val formatter = DecimalFormat("$#,##0.00")
        holder.amount.text = formatter.format(transaction.amount)
        holder.category.text = transaction.category
        holder.date.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaction.date)
        holder.typeIcon.setImageResource(
            if (transaction.isIncome) R.drawable.ic_income else R.drawable.ic_expense
        )
    }

    override fun getItemCount(): Int = transactions.size
}