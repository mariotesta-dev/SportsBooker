package com.example.lab2.reservation.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R

class AdapterRVSportFilter(
    private var listOfSport: List<String?>,
    val setFilter: (input: String?) -> Unit
) : RecyclerView.Adapter<AdapterRVSportFilter.ViewHolderFilter>() {

    var selectedPosition = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFilter {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.filter_button, parent, false)
        return ViewHolderFilter(v)
    }

    override fun getItemCount(): Int = listOfSport.size

    override fun onBindViewHolder(holder: ViewHolderFilter, position: Int) {
        val name = listOfSport[position]

        holder.selectionIndicator.visibility =
            if (selectedPosition == position) View.VISIBLE else View.INVISIBLE

        holder.name.text = name ?: "All"
        holder.layout.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            setFilter(name)
        }
    }

    fun setFilters(newFilters: List<String?>) {

        val diffs = DiffUtil.calculateDiff(
            FilterDiffCallback(listOfSport, newFilters)
        )
        listOfSport = newFilters
        diffs.dispatchUpdatesTo(this)
    }

    inner class ViewHolderFilter(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.filter_name)
        val layout: ConstraintLayout = v.findViewById(R.id.filter_button_layout)
        val selectionIndicator: View = v.findViewById(R.id.selectionIndicator)
    }

}