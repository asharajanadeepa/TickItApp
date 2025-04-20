package com.example.tickitapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tickitapp.R
import java.text.DecimalFormat

data class CategoryTotal(val category: String, val total: Double)

class CategoryAdapter(private val categoryTotals: List<CategoryTotal>) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        val categoryAmount: TextView = itemView.findViewById(R.id.category_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryTotal = categoryTotals[position]
        holder.categoryName.text = categoryTotal.category
        val formatter = DecimalFormat("$#,##0.00")
        holder.categoryAmount.text = formatter.format(categoryTotal.total)
    }

    override fun getItemCount(): Int = categoryTotals.size
}