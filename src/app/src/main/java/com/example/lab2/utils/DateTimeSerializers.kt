package com.example.lab2.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class DateTimeSerializers {

    class LocalDateSerializer : KSerializer<LocalDate> {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        override val descriptor = serializer<String>().descriptor

        override fun serialize(encoder: Encoder, value: LocalDate) {
            encoder.encodeString(formatter.format(value))
        }

        override fun deserialize(decoder: Decoder): LocalDate {
            val value = decoder.decodeString()
            return LocalDate.parse(value, formatter)
        }
    }

    class LocalTimeSerializer : KSerializer<LocalTime> {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

        override val descriptor = serializer<String>().descriptor

        override fun serialize(encoder: Encoder, value: LocalTime) {
            encoder.encodeString(formatter.format(value))
        }

        override fun deserialize(decoder: Decoder): LocalTime {
            val value = decoder.decodeString()
            return LocalTime.parse(value, formatter)
        }
    }


}