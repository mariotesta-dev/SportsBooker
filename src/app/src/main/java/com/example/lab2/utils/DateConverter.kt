package com.example.lab2.utils

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class DateConverter {

    private val formatterDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromDate(localDate: LocalDate?): String? {
        return localDate?.format(formatterDate)
    }

    @TypeConverter
    fun toLocalDate(sqlDate: String?): LocalDate? {
        return sqlDate?.let { LocalDate.parse(it, formatterDate) }
    }
}