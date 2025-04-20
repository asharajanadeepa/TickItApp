package com.example.tickitapp.ui.analysis

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tickitapp.R
import com.example.tickitapp.adapter.CategoryTotal
import com.example.tickitapp.adapter.CategoryAdapter
import com.example.tickitapp.databinding.FragmentAnalysisBinding

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

        // Sample data (replace with real transaction data later)
        val categoryTotals = listOf(
            CategoryTotal("Food", 50.0),
            CategoryTotal("Transport", 20.0),
            CategoryTotal("Bills", 100.0),
            CategoryTotal("Entertainment", 30.0),
            CategoryTotal("Other", 10.0)
        )

        binding.categoryList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CategoryAdapter(categoryTotals)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}