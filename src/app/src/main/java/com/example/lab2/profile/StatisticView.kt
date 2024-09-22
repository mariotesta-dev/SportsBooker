package com.example.lab2.profile

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.lab2.R
import com.example.lab2.entities.Sport
import com.example.lab2.entities.Statistic

class StatisticView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val statistic: Statistic,
    private val score: Long?
) : LinearLayout(context, attrs, defStyleAttr) {
    private val playedCount: TextView
    private val scoreCount: TextView
    private val image: ImageView


    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.statistic_layout, this, true)

        playedCount = view.findViewById(R.id.statistic_played_count)
        scoreCount = view.findViewById(R.id.mvp_count)
        image = view.findViewById(R.id.statistic_image)

        Log.i("score", score.toString())
        playedCount.text = "${statistic.gamesPlayed}"
        scoreCount.text = "${score ?: 0}pt"


        when (statistic.sport) {
            Sport.FOOTBALL -> image.setImageResource(R.drawable.sport_football)
            Sport.GOLF -> image.setImageResource(R.drawable.sport_golf)
            Sport.TENNIS -> image.setImageResource(R.drawable.sport_tennis)
            Sport.BASEBALL -> image.setImageResource(R.drawable.sport_baseball)
            Sport.BASKETBALL -> image.setImageResource(R.drawable.sport_basketball)
            Sport.PADEL -> image.setImageResource(R.drawable.sport_padel)
        }

    }

}
