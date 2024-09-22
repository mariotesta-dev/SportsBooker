package com.example.lab2.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab2.entities.MatchFirebase
import com.example.lab2.entities.MatchWithCourtAndEquipmentsToFirebase
import com.example.lab2.entities.ReservationFirebase
import com.example.lab2.utils.toTimestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CreateMatchVM @Inject constructor() : ViewModel() {

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance()

    private var listTimeslots: MutableLiveData<List<String>> = MutableLiveData(
        listOf(
            "08:30", "10:00", "11:30", "13:00", "14:30", "16:00", "17:30",
            "19:00", "20:30", "22:00"
        )
    )

    fun getListTimeslots(): LiveData<List<String>> {
        return listTimeslots
    }

    private var exceptionMessage = MutableLiveData<String>()
    fun getExceptionMessage(): LiveData<String> {
        return exceptionMessage
    }


    fun createMatch(
        date: LocalDate,
        time: LocalTime,
        sport: String,
        playerId: String
    ) {
        viewModelScope.launch {
            val startTimestamp = LocalDateTime.of(date, time).toTimestamp()
            val endTimestamp = LocalDateTime.of(date, time).plusNanos(999_999_999).toTimestamp()

            val listOfBookedCourts = mutableListOf<String>()
            //Populate the list of already booked courts for the given timeslot
            val playerRef = db.collection("players").document(userId.uid!!)
            val playerMatchQuery = db.collection("matches")
                .whereArrayContains("listOfPlayers", playerRef)
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)

            playerMatchQuery.get().addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    //create match
                    db.collection("matches")
                        .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                        .whereLessThan("timestamp", endTimestamp)
                        .get().addOnSuccessListener { matches ->
                            CoroutineScope(Dispatchers.IO).launch {
                                for (match in matches) {
                                    val q = match.getDocumentReference("court")?.get()?.await()
                                    listOfBookedCourts.add(q!!.id)
                                }
                                if (listOfBookedCourts.isEmpty()) listOfBookedCourts.add((-1).toString())
                                //Extract the first available court for the given timeslot and sport
                                db.collection("courts")
                                    .whereNotIn(FieldPath.documentId(), listOfBookedCourts)
                                    .whereEqualTo("sport", sport).limit(1).get()
                                    .addOnSuccessListener { court ->
                                        if (court.isEmpty) {
                                            exceptionMessage.postValue("No available courts at this time")
                                        } else {
                                            db.collection("matches").add(
                                                MatchFirebase(
                                                    court.first().reference,
                                                    1,
                                                    LocalDateTime.of(date, time).toTimestamp(),
                                                    listOf(db.collection("players").document(playerId))
                                                )
                                            ).addOnSuccessListener { matchAdded ->
                                                db.collection("reservations").add(
                                                    ReservationFirebase(
                                                        matchAdded,
                                                        db.collection("players").document(playerId),
                                                        emptyList(),
                                                        court.first().getLong("basePrice")!!
                                                    )
                                                )
                                            }
                                            exceptionMessage.postValue("Match created successfully")
                                        }
                                    }
                            }
                        }
                } else {
                    // Player already has a match at the specified timestamp
                    exceptionMessage.postValue("Player already has a match at that timestamp")
                }
            }.addOnFailureListener { exception ->
                exceptionMessage.postValue("Couldn't check player's matches")
                throw exception
            }

        }
    }

    fun filterTimeslots(date: LocalDate) {
        if (date > LocalDate.now()) {
            listTimeslots.value = listOf(
                "08:30", "10:00", "11:30", "13:00", "14:30", "16:00", "17:30",
                "19:00", "20:30", "22:00"
            )
        } else {
            listTimeslots.value = listTimeslots.value!!.filter {
                val time = LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                time.isAfter(LocalTime.now())
            }
        }
    }
}