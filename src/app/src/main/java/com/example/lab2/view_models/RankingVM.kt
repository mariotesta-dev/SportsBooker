package com.example.lab2.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab2.entities.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.lab2.entities.Result

@HiltViewModel
class RankingVM @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val rankFilter: MutableLiveData<String> = MutableLiveData()
    private val _allPlayers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val allPlayers: LiveData<List<User>> get() = _allPlayers
    private val _ranking: MutableLiveData<List<Pair<User, Long>>> = MutableLiveData(emptyList())
    val ranking: LiveData<List<Pair<User, Long>>> get() = _ranking


    // UI states
    var error: MutableLiveData<String?> = MutableLiveData()
    var loadingState: MutableLiveData<Boolean> = MutableLiveData(false)

    fun getAllPlayers() {
        viewModelScope.launch {

            val noPoints : Long = 0
            loadingState.value = true

            val result = withContext(Dispatchers.IO) {
                try {
                    val players = db.collection("players").get().await()
                    val listOfPlayers = players.documents.mapNotNull { p ->
                        User.fromFirebase(p)
                    }.toMutableList()
                    Result(listOfPlayers, null)
                } catch (err: Exception) {
                    Result(null, err)
                }
            }

            if(result.value != null){
                error.value = null
                _allPlayers.postValue(result.value!!)
                _ranking.postValue(result.value.map { u -> Pair(u, u.score?.get("Tennis") ?: 0) }.toList().sortedByDescending { it.second })
            }else{
                error.value = result.throwable?.message
            }

            loadingState.value = false
        }
    }

    fun setRanking(filter: String?){
        viewModelScope.launch {
            if(filter != null){
                rankFilter.value = filter!!
            }else{
                throw Exception("Unable to get ranking, try again later.")
            }

            loadingState.value = true

            val result = withContext(Dispatchers.Default) {
                try{
                    val newRanking = allPlayers.value
                        ?.map { u -> Pair(u, u.score?.get(filter) ?: 0) }?.toList()
                        ?.sortedByDescending { pair -> pair.second }
                    Result(newRanking, null)
                } catch (err: Exception) {
                    Result(null, err)
                }
            }

            if(result.value != null){
                error.value = null
                _ranking.value = result.value!!
            }else{
                error.value = result.throwable?.message
            }

            loadingState.value = false

        }
    }


}