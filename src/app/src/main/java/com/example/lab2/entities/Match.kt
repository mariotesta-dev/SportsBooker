package com.example.lab2.entities

import com.example.lab2.utils.DateTimeSerializers
import com.example.lab2.utils.TimestampUtil
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate
import java.time.LocalTime

@kotlinx.serialization.Serializable
data class Match(
    val matchId: String,
    val numOfPlayers: Long,
    @kotlinx.serialization.Serializable(with = DateTimeSerializers.LocalDateSerializer::class)
    val date: LocalDate,
    @kotlinx.serialization.Serializable(with = DateTimeSerializers.LocalTimeSerializer::class)
    var time: LocalTime,
    var listOfPlayers: MutableList<String>
)


fun firebaseToMatch(d: DocumentSnapshot): Match {

    val mappedPlayers = (d.get("listOfPlayers") as List<DocumentReference>).map {
        it.id
    }.toMutableList()

    return Match(
        d.id,
        d.getLong("numOfPlayers")!!,
        TimestampUtil.timestampToLocalDate(d.getTimestamp("timestamp")!!),
        TimestampUtil.timestampToLocalTime(d.getTimestamp("timestamp")!!),
        mappedPlayers
    )
}