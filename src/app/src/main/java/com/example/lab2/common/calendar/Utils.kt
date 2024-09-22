package com.example.lab2.common.calendar

import android.content.Context
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.yearMonth
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal fun Context.getColorCompat(@ColorRes color: Int) =
    ContextCompat.getColor(this, color)

internal fun TextView.setTextColorRes(@ColorRes color: Int) =
    setTextColor(context.getColorCompat(color))

fun YearMonth.displayText(short: Boolean = false): String {
    return "${this.month.displayText(short = short)} ${this.year}"
}

fun Month.displayText(short: Boolean = true): String {
    val style = if (short) TextStyle.SHORT else TextStyle.FULL
    return getDisplayName(style, Locale.ENGLISH)
}

fun DayOfWeek.displayText(uppercase: Boolean = false): String {
    return getDisplayName(TextStyle.SHORT, Locale.ENGLISH).let { value ->
        if (uppercase) value.uppercase(Locale.ENGLISH) else value
    }
}

fun getWeekPageTitle(week: Week): String {
    val firstDate = week.days.first().date
    val lastDate = week.days.last().date
    return when {
        firstDate.yearMonth == lastDate.yearMonth -> {
            firstDate.yearMonth.displayText()
        }

        firstDate.year == lastDate.year -> {
            "${firstDate.month.displayText(short = false)} - ${lastDate.yearMonth.displayText()}"
        }

        else -> {
            "${firstDate.yearMonth.displayText()} - ${lastDate.yearMonth.displayText()}"
        }
    }
}

fun setupTimeslots(): MutableList<LocalTime> {
    val startTime = LocalTime.of(8, 30) // start time is 9:00
    val endTime = LocalTime.of(21, 0) // end time is 22:00
    val hoursList = mutableListOf<LocalTime>() // create an empty list to store the hours

    var time = startTime // set the initial time to the start time
    hoursList.add(time)

    while (time.isBefore(endTime.plusHours(1))) { // add an extra hour to the end time to include it in the list
        val endTimeForSport = time.plusMinutes(
            Duration.ofMinutes(90).toMinutes()
        ) // calculate the end time for the current sport
        if (endTimeForSport.isBefore(endTime.plusMinutes(1))) { // add an extra minute to the end time to prevent overlap
            hoursList.add(endTimeForSport) // add the current time to the list
        }
        time = time.plusHours(1) // increment the time by one hour
    }
    return hoursList
}
