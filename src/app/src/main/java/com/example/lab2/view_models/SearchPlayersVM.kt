package com.example.lab2.view_models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab2.entities.Match
import com.example.lab2.entities.Result
import com.example.lab2.entities.User
import com.example.lab2.entities.invitationToFirebase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchPlayersVM @Inject constructor() : ViewModel(){

    private val db = FirebaseFirestore.getInstance()

    private val _allPlayers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    private val _filteredPlayers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val allPlayers: LiveData<List<User>> get() = _filteredPlayers

    // UI States
    var error: MutableLiveData<String?> = MutableLiveData()
    var loadingState: MutableLiveData<Boolean> = MutableLiveData(false)
    var invitationSuccess: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        getAllPlayers()
    }

    fun getAllPlayers() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                try{
                    val players = db.collection("players").get().await()
                    val playersList = players.documents.mapNotNull { player ->
                        User.fromFirebase(player)
                    }.toMutableList()
                    Result(playersList, null)
                }catch (err: Exception){
                    Result(null, err)
                }
            }

            if(result.value != null){
                error.value = null
                _allPlayers.value = result.value!!
                _filteredPlayers.value = result.value!!
            }else{
                error.value = result.throwable?.message
            }

            loadingState.value = false
        }
    }


    fun sendInvitation(
        sender: String,
        recipient: User,
        match: Match
    ) {
        viewModelScope.launch {
            val newInvitation = invitationToFirebase(match = match, sentBy = sender, sentTo = recipient)

            val result = withContext(Dispatchers.IO){
                try {
                    val invitations = db.collection("invitations")
                        .whereEqualTo("match", db.document("matches/${match.matchId}"))
                        .whereEqualTo("sentBy", db.document("players/${sender}"))
                        .whereEqualTo("sentTo", db.document("players/${recipient.userId}"))
                        .get().await()

                    if (invitations.documents.isEmpty()) {
                        // Add a new invitation
                        db.collection("invitations").add(newInvitation)
                        Result(true, null)
                    } else {
                        // An invitation already exists
                        Log.i("newinv", "already sent")
                        throw Exception("You have already sent an invitation to this player.")
                    }

                } catch (err: Exception) {
                    Result(false, err)
                }
            }

            if(result.value!!){
                error.value = null
                invitationSuccess.value = true
            }else{
                invitationSuccess.value = false
                error.value = result.throwable?.message
            }

        }
    }


    fun filterPlayers(query: String?) {
        if (query != null) {
            _filteredPlayers.value = _allPlayers.value?.filter {
                it.full_name.lowercase().contains(query.lowercase()) || it.nickname.lowercase()
                    .contains(query.lowercase())
            }
        } else {
            _filteredPlayers.value = _allPlayers.value
        }
    }


}