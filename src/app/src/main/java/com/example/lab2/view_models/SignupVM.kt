package com.example.lab2.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab2.entities.BadgeType
import com.example.lab2.entities.PartialRegistration
import com.example.lab2.entities.Result
import com.example.lab2.entities.Sport
import com.example.lab2.entities.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignupVM @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    var valid: MutableLiveData<Boolean> = MutableLiveData(false)
    var validUsername: MutableLiveData<Boolean> = MutableLiveData(false)
    var error: MutableLiveData<String?> = MutableLiveData()
    var loadingState: MutableLiveData<Boolean> = MutableLiveData(false)

    var registrationFinished: MutableLiveData<Boolean> = MutableLiveData(false)
    var userExists: MutableLiveData<Boolean> = MutableLiveData()


    fun createPlayer(
        userId: String,
        name: String,
        surname: String,
        username: String,
        email: String,
        dateOfBirth: String,
        location: String,
        selectedInterests: MutableList<Sport>,
        photoUrl: String
    ) {
        val user = User.toFirebase(
            User(
                userId = userId,
                full_name = "$name $surname",
                nickname = username,
                email = email,
                description = "",
                birthday = LocalDate.parse(dateOfBirth),
                address = location,
                interests = selectedInterests,
                badges = BadgeType.values().associateWith { 0 },
                image = photoUrl
            )
        )
        db.collection("players").document(userId).set(user)
    }

    fun updatePlayer(userId: String, sports: MutableList<Sport>) {
        db.collection("players").document(userId).update("interests", sports)
    }

    fun loginWithGoogle(
        name: String,
        surname: String,
        email: String,
        credential: AuthCredential,
        photoUrl: String,
        username: String,
        dateOfBirth: String,
        location: String,
    ) {
        viewModelScope.launch {
            loadingState.value = true // Set loading state to true

            try {
                val result = signInWithCredential(credential)
                if (result.value != null) {
                    val querySnapshot = db.collection("players")
                        .whereEqualTo("email",email)
                        .get()
                        .await()
                    if (!querySnapshot.isEmpty) {
                        // User already exists in the "players" collection
                        loadingState.value = false // Set loading state to false
                        // Handle successful login
                    } else {
                        // User does not exist, proceed with registration
                        createPlayer(
                            userId = result.value!!.uid,
                            name = name,
                            surname = surname,
                            username = username,
                            email = email,
                            location = location,
                            dateOfBirth = dateOfBirth,
                            selectedInterests = mutableListOf(),
                            photoUrl = photoUrl
                        )
                        loadingState.value = false // Set loading state to false
                        registrationFinished.value = true
                        // Handle successful registration
                        // Navigate to the appropriate screen
                    }

                } else {
                    loadingState.value = false // Set loading state to false
                    // Handle Google sign-in failure
                    error.value = result.throwable?.message
                }
            } catch (e: Exception) {
                loadingState.value = false // Set loading state to false
                // Handle any exceptions
                error.value = e.message
            }
        }
    }

    private suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        return withContext(Dispatchers.IO) {
            try {
                val result = firebaseAuth.signInWithCredential(credential).await()
                Result(result.user, null)
            } catch (e: Exception) {
                Result(null, e)
            }
        }
    }

    fun checkIfUsernameAlreadyExists(username: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                val querySnapshot = db.collection("players")
                                .whereEqualTo("username", username)
                                .get()
                                .await()
                if(querySnapshot.documents.isNotEmpty())
                    // Username exists
                    Result(false, Exception("Username already exists"))
                else
                    // Username doesn't exist
                    Result(true, null)
            }

            if(result.value!!){
                validUsername.value = true
            }else{
                 validUsername.value = false
                 error.value = result.throwable?.message
            }

        }
    }

    fun checkIfPlayerExists(email: String, credential: AuthCredential) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                val querySnapshot = db.collection("players")
                                .whereEqualTo("email", email)
                                .get()
                                .await()
                if(querySnapshot.documents.isNotEmpty())
                    // Player exists
                    Result(true, null)
                else
                    // Player doesn't exist
                    Result(false, null)
            }

            userExists.value = result.value!!
        }
    }
}
