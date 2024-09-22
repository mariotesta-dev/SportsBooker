package com.example.lab2.utils

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class TimestampUtil {
    companion object {
        fun timestampToLocalDate(timestamp: Timestamp): LocalDate {
            val instant = timestamp.toDate().toInstant()
            val zoneId = ZoneId.systemDefault() // Or choose a specific ZoneId
            val zonedDateTime = instant?.atZone(zoneId)
            val localDate = zonedDateTime?.toLocalDate()
            return localDate!!
        }

        fun timestampToLocalTime(timestamp: Timestamp): LocalTime {
            val instant = timestamp.toDate().toInstant()
            val zoneId = ZoneId.systemDefault() // Or choose a specific ZoneId
            val zonedDateTime = instant?.atZone(zoneId)
            val localTime = zonedDateTime?.toLocalTime()
            return localTime!!
        }
    }
}

fun LocalDateTime.toTimestamp(): Timestamp {
    val epochSeconds = this.minusHours(2).toEpochSecond(ZoneOffset.UTC)
    val nanoseconds = this.nano
    return Timestamp(epochSeconds, nanoseconds)
}