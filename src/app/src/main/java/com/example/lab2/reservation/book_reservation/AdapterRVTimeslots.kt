package com.example.lab2.reservation.book_reservation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R
import com.example.lab2.entities.Court
import com.example.lab2.entities.Match
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AdapterRVTimeslots(
    private val court: Court,
    private val listMatches: List<Match>,
    private val listener: OnClickTimeslot
) :
    RecyclerView.Adapter<AdapterRVTimeslots.ViewHolder>() {

    interface OnClickTimeslot {
        fun onClickTimeslot(timeslot: LocalTime)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeslotTextView: TextView = view.findViewById(R.id.tvTimeslot)
        val numPlayersLeftTV: TextView = view.findViewById(R.id.tvPlayersLeft)
        val context = view.context
        val userIcon: ImageView = view.findViewById(R.id.user_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.timeslot_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = listMatches[position]
        holder.timeslotTextView.text = list.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        holder.numPlayersLeftTV.text = "${court.maxNumberOfPlayers?.minus(list.numOfPlayers)} left"
        if (court.maxNumberOfPlayers?.minus(list.numOfPlayers) == 0.toLong()) {
            holder.numPlayersLeftTV.setTextColor(holder.context.getColor(R.color.bright_red))
            holder.userIcon.setColorFilter(
                ContextCompat.getColor(
                    holder.context,
                    R.color.bright_red
                )
            )
        }
        holder.itemView.setOnClickListener {
            listener.onClickTimeslot(list.time)
        }
    }

    override fun getItemCount(): Int = listMatches.size
}