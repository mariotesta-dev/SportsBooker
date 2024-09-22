package com.example.lab2.entities

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

data class MatchFirebase(
    val Court: DocumentReference,
    val numOfPlayers: Long?,
    val timestamp: Timestamp?,
    val listOfPlayers: List<DocumentReference>?
)

