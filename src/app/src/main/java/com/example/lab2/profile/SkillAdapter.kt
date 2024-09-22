package com.example.lab2.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.example.lab2.R
import com.example.lab2.entities.BadgeType

class SkillAdapter(private val context: Context, private val badges: Map<BadgeType, Int>) :
    BaseAdapter() {
    override fun getCount(): Int {
        return badges.size
    }

    override fun getItem(i: Int): Map.Entry<BadgeType, Int> {
        return badges.entries.toList()[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {

        val view =
            convertView
                ?: LayoutInflater.from(context)
                    .inflate(R.layout.skill_layout, parent, false)

        val skillImage = view.findViewById<ImageView>(R.id.skill_image)
        val skillText = view.findViewById<TextView>(R.id.skill_label)
        val skillRating = view.findViewById<RatingBar>(R.id.rating)
        when (getItem(i).key) {
            BadgeType.SPEED -> {
                skillImage.setImageResource(R.drawable.badge_speed)
                skillText.text = "Speed"
            }

            BadgeType.PRECISION -> {
                skillImage.setImageResource(R.drawable.badge_precision)
                skillText.text = "Precision"
            }

            BadgeType.TEAM_WORK -> {
                skillImage.setImageResource(R.drawable.badge_team)
                skillText.text = "Team Work"
            }

            BadgeType.STRATEGY -> {
                skillImage.setImageResource(R.drawable.badge_strategy)
                skillText.text = "Strategy"
            }

            BadgeType.ENDURANCE -> {
                skillImage.setImageResource(R.drawable.badge_endurance)
                skillText.text = "Endurance"
            }
        }
        skillRating.rating = getItem(i).value.toFloat()

        return view
    }
}
