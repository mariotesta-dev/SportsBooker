package com.example.lab2.entities

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class PlayerMVPVote(
    val reviewer: User,
    val mvp: User,
    val match: Match
)

fun playerMVPVoteToFirebase(mvpUserId: String, match: Match): Map<String, Any> {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val map: MutableMap<String, Any> = mutableMapOf()
    map["match"] = db.document("matches/${match.matchId}")
    map["reviewer"] = db.document("players/${auth.currentUser!!.uid}")
    map["mvp"] = db.document("players/${mvpUserId}")
    return map
}
