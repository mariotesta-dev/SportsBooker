package com.example.lab2.reservation.my_reservations

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.lab2.R
import com.example.lab2.common.calendar.setTextColorRes
import com.example.lab2.entities.MatchWithCourtAndEquipments
import com.example.lab2.entities.formatPrice
import com.example.lab2.reservation.details.DetailsActivity
import java.time.format.DateTimeFormatter

class AdapterRVMyReservations(
    private var list: List<MatchWithCourtAndEquipments>,
    private val listener: OnEditClickListener
) : RecyclerView.Adapter<AdapterRVMyReservations.ViewHolderCard>() {

    interface OnEditClickListener {
        fun onEditClick(reservation: MatchWithCourtAndEquipments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCard {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.reservation_card_layout, parent, false)
        return ViewHolderCard(v)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolderCard, position: Int) {
        holder.name.text = "${list[position].court.name}"
        holder.location.text = "Via Giovanni Magni, 32"
        holder.price.text = "€ ${list[position].formatPrice()}"
        if (list[position].match.numOfPlayers == list[position].court.maxNumberOfPlayers) {
            holder.currentNumberOfPlayers.setTextColorRes(R.color.darker_blue)
        } else {
            holder.currentNumberOfPlayers.setTextColorRes(R.color.example_1_bg)
        }
        holder.currentNumberOfPlayers.text = "${list[position].match.numOfPlayers}"
        if (list[position].match.numOfPlayers == list[position].court.maxNumberOfPlayers) holder.currentNumberOfPlayers.setTextColor(
            holder.context.getColor(
                R.color.bright_red
            )
        )
        holder.maxNumberOfPlayers.text = "/${list[position].court.maxNumberOfPlayers}"
        holder.time.text =
            list[position].match.time.format(DateTimeFormatter.ofPattern("HH:mm")).toString()
        holder.sport.text = "${list[position].court.sport}"

        holder.editButton.setOnClickListener { listener.onEditClick(list[holder.bindingAdapterPosition]) }
        holder.detailsButton.setOnClickListener {
            val intent = Intent(holder.context, DetailsActivity::class.java)
            intent.putExtra("reservationId", list[holder.bindingAdapterPosition].reservationId)
            holder.context.startActivity(intent)
        }
    }

    fun setReservations(newReservations: List<MatchWithCourtAndEquipments>) {

        val diffs = DiffUtil.calculateDiff(
            ReservationDiffCallback(list, newReservations)
        )
        list = newReservations
        diffs.dispatchUpdatesTo(this)
    }

    private fun deepCopy(list: List<MatchWithCourtAndEquipments>): List<MatchWithCourtAndEquipments> {
        val newList = mutableListOf<MatchWithCourtAndEquipments>()
        for (item in list) {
            newList.add(MatchWithCourtAndEquipments(
                item.reservationId,
                item.match,
                item.court,
                item.equipments,
                item.finalPrice
            ))
        }
        return newList
    }

    inner class ViewHolderCard(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.court_name_reservation)
        val location: TextView = v.findViewById(R.id.location_reservation)
        val currentNumberOfPlayers: TextView = v.findViewById(R.id.current_number_of_players)
        val price: TextView = v.findViewById(R.id.price_reservation)
        val maxNumberOfPlayers: TextView = v.findViewById(R.id.max_number_players)
        val time: TextView = v.findViewById(R.id.time_reservation)
        val editButton: ImageButton = v.findViewById(R.id.edit_reservation_button)
        val sport: TextView = v.findViewById(R.id.sport_name)
        val detailsButton: Button = v.findViewById(R.id.detailReservationButton)
        val context: Context = v.context
        val maxNumber: TextView = v.findViewById(R.id.max_number_players)
    }
}