package com.example.lab2.entities

import kotlinx.serialization.Serializable

@Serializable
data class Statistic(
    val sport: Sport,
    val gamesPlayed: Int,
    //val gamesWon: Int,
    //val gamesLost: Int,
    //val gamesDrawn: Int? = null
)
