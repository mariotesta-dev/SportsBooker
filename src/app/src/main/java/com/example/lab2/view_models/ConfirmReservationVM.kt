package com.example.lab2.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab2.entities.MatchWithCourtAndEquipments
import com.example.lab2.entities.MatchWithCourtAndEquipmentsToFirebase
import com.example.lab2.entities.Result
import com.example.lab2.utils.toTimestamp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ConfirmReservationVM @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _exceptionMessage = MutableLiveData<String>()
    val exceptionMessage: LiveData<String> = _exceptionMessage

    private val _submitConfirmSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    val submitConfirmSuccess: LiveData<Boolean> = _submitConfirmSuccess

    var error: MutableLiveData<String?> = MutableLiveData()


    fun addReservation(newMatch: MatchWithCourtAndEquipments) {
        viewModelScope.launch {
            val startTimestamp =
                LocalDateTime.of(newMatch.match.date, newMatch.match.time).minusMinutes(1)
                    .toTimestamp()
            val endTimestamp =
                LocalDateTime.of(newMatch.match.date, newMatch.match.time).plusMinutes(1)
                    .toTimestamp()
            val playerRef = db.collection("players").document(auth.currentUser!!.uid)
            val playerMatchQuery = db.collection("matches")
                .whereArrayContains("listOfPlayers", playerRef)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)

            val result = withContext(Dispatchers.IO) {
               try {
                   val matches = getMatches(playerRef, startTimestamp, endTimestamp)
                   if (matches.isEmpty) {
                       addMatch(playerRef, newMatch)
                   } else {
                       throw Exception("Player already has a match at that timestamp")
                   }
                   Result(value = true, throwable = null)
               } catch (err: Exception) {
                   Result(value = false, throwable = err)
               }
            }
            if(result.value!!){
                _submitConfirmSuccess.value = true
            }else{
                _submitConfirmSuccess.value = false
                error.value = result.throwable?.message
            }
        }
    }

    private suspend fun getMatches(playerRef: DocumentReference, startTimestamp: Timestamp, endTimestamp: Timestamp): QuerySnapshot {
        val res = db.collection("matches")
            .whereArrayContains("listOfPlayers", playerRef)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThanOrEqualTo("timestamp", endTimestamp)
            .get().await()
        return if(res is QuerySnapshot) res else throw Exception("Couldn't check player's matches")

    }
    private suspend fun addMatch(playerRef: DocumentReference ,newMatch: MatchWithCourtAndEquipments) {
        val matchRef = db.collection("matches").document(newMatch.match.matchId)
        matchRef.update("listOfPlayers", FieldValue.arrayUnion(playerRef)).await()
        matchRef.update("numOfPlayers", FieldValue.increment(1)).await()
        db.collection("reservations").add(
            MatchWithCourtAndEquipmentsToFirebase(
                auth.currentUser!!.uid,
                newMatch
            )
        ).await()

    }
}