package com.example.lab2.profile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.lab2.R
import com.example.lab2.entities.Sport

class InterestView(
    context: Context,
    attrs: AttributeSet? = null,
    private val sport: Sport,
) : LinearLayout(context, attrs) {
    private val name: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.interest_layout, this, true)

        name = view.findViewById(R.id.interest_text)

        when (sport) {
            Sport.FOOTBALL -> name.text = "Football"
            Sport.GOLF -> name.text = "Golf"
            Sport.TENNIS -> name.text = "Tennis"
            Sport.BASEBALL -> name.text = "Baseball"
            Sport.BASKETBALL -> name.text = "Basketball"
            Sport.PADEL -> name.text = "Padel"
        }


    }

}
