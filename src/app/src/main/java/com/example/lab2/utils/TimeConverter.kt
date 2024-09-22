package com.example.lab2.utils

import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class TimeConverter {

    private val formatterTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @TypeConverter
    fun fromTime(localTime: LocalTime?): String? {
        return localTime?.format(formatterTime)
    }

    @TypeConverter
    fun toLocalTime(sqlTime: String?): LocalTime? {
        return sqlTime?.let { LocalTime.parse(it, formatterTime) }
    }
}