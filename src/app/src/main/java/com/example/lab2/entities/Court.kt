package com.example.lab2.entities

import com.google.firebase.firestore.DocumentSnapshot

@kotlinx.serialization.Serializable
data class Court(
    val courtId: String,
    val description: String?,
    val maxNumberOfPlayers: Long?,
    val name: String?,
    val sport: String?,
    val basePrice: Double?,
    val image: String?
)


fun firebaseToCourt(d: DocumentSnapshot): Court {
    return Court(
        d.id,
        d.getString("description"),
        d.getLong("maxNumOfPlayers"),
        d.getString("name"),
        d.getString("sport"),
        d.getDouble("basePrice"),
        d.getString("image")
    )
}