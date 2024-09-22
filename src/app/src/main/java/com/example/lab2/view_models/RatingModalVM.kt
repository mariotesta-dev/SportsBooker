package com.example.lab2.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab2.entities.CourtReview
import com.example.lab2.entities.Court
import com.example.lab2.entities.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RatingModalVM @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _selectedMVP = MutableLiveData<String>()
    val selectedMVP: LiveData<String> = _selectedMVP
    fun setSelectedMVP(mvpUserId: String) {
        _selectedMVP.value = mvpUserId
    }

    fun submitReview(review: CourtReview) {
        db.collection("court_reviews")
            .whereEqualTo("reviewer", db.document("players/${auth.currentUser!!.uid}"))
            .whereEqualTo("court", db.document("courts/${review.courtId}"))
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    db.collection("court_reviews")
                        .add(review)
                        .addOnSuccessListener {
                            Log.d("RatingModalVM", "Review added")
                        }
                        .addOnFailureListener {
                            Log.d("RatingModalVM", "Review failed")
                        }
                } else {
                    it.documents[0].reference.update("review", review.review)
                        .addOnSuccessListener {
                            Log.d("RatingModalVM", "Review updated")
                        }
                        .addOnFailureListener {
                            Log.d("RatingModalVM", "Review update failed")
                        }
                }
            }
    }

    fun incrementMVPScore(court: Court) {
        val mvpId = _selectedMVP.value
        val sportFieldPath = "score.${court.sport}"

        val updateMap = hashMapOf<String, Any>(
            sportFieldPath to FieldValue.increment(3)
        )

        if(mvpId != null){
            db.collection("players")
                .document(mvpId)
                .update(updateMap)
        }
    }

    fun deleteReviewNotification(notificationId: String, matchId: String, _notifications: MutableLiveData<MutableList<Notification>>) {

        val entry = mutableMapOf<String, Any>()
        entry["reviewer"] = db.document("players/${auth.currentUser?.uid!!}")
        entry["match"] = db.document("matches/${matchId}")

        viewModelScope.launch {
            withContext(Dispatchers.IO){
                db.collection("player_rating_mvp").add(entry)
            }
            _notifications.value = _notifications.value?.filter { it.id != notificationId }?.toMutableList()
        }
    }
}