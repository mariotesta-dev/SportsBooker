package com.example.lab2.entities

enum class Sport {
    TENNIS,
    PADEL,
    FOOTBALL,
    BASKETBALL,
    BASEBALL,
    GOLF
}

fun getSportFromString(sport: String): Sport {
    return when (sport.lowercase()) {
        "Tennis" -> Sport.TENNIS
        "Padel" -> Sport.PADEL
        "Football" -> Sport.FOOTBALL
        "Basketball" -> Sport.BASKETBALL
        "Baseball" -> Sport.BASEBALL
        "Golf" -> Sport.GOLF
        else -> Sport.TENNIS
    }
}
