package com.example.lab2.reservation.book_reservation

import androidx.recyclerview.widget.DiffUtil
import com.example.lab2.entities.Court
import com.example.lab2.entities.Match

class CourtTimeslotDiffCallback(
    private val listCourts: Map<Court, List<Match>>,
    private val newListCourts: Map<Court, List<Match>>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = listCourts.size

    override fun getNewListSize(): Int = newListCourts.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return listCourts.entries.elementAt(oldItemPosition).key.courtId == newListCourts.entries.elementAt(
            newItemPosition
        ).key.courtId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = listCourts.entries.elementAt(oldItemPosition)
        val newEntry = newListCourts.entries.elementAt(newItemPosition)
        return oldEntry == newEntry
    }
}