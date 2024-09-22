package com.example.lab2.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.lab2.R
import com.example.lab2.databinding.FragmentLoginBinding
import com.example.lab2.entities.PartialRegistration
import com.example.lab2.launcher.LauncherActivity
import com.example.lab2.reservation.my_reservations.MyReservationsActivity
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.SignupVM
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import kotlin.math.sign


@AndroidEntryPoint
class FragmentLogin : Fragment(R.layout.fragment_login) {

    companion object {
        fun newInstance() = FragmentLogin()
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: FragmentLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    @Inject
    lateinit var mainVM: MainVM
    lateinit var signupVM: SignupVM


    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        binding = FragmentLoginBinding.bind(view)

        firebaseAuth = FirebaseAuth.getInstance()

        signupVM = ViewModelProvider(requireActivity())[SignupVM::class.java]

        navController = findNavController()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser

            // Qui si riceve la callback se l'utente ha loggato
            if (user != null) {
                mainVM.listenToUserUpdates(user.uid)
                observeUserUpdates(this)
            }
        }

        signupVM.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireActivity(), error, Toast.LENGTH_SHORT).show()
        }

        signupVM.loadingState.observe(viewLifecycleOwner) { loadingState ->
            if (loadingState) {
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        view.findViewById<View>(R.id.login).setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signupVM.loadingState.value = true
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                requireActivity(),
                                "Logged in successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            signupVM.loadingState.value = false
                            showValidationError(task.exception)
                        }
                    }
            } else {
                Toast.makeText(requireActivity(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        binding.googleCardView?.setOnClickListener {
            signupVM.loadingState.value = true
            googleSignIn()
        }

        return view
    }


    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            manageResults(task)
        }
    }

    private fun manageResults(task: Task<GoogleSignInAccount>) {
        val account: GoogleSignInAccount? = task.result
        if (account != null) {
            val name = account.givenName
            var surname = account.familyName
            if (surname == null) surname = ""
            val email = account.email
            var photoUrl = account.photoUrl.toString().replace("s96-c", "s400-c")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Log.d("photoUrl", photoUrl)
            if (photoUrl.isEmpty())
                photoUrl = "https://firebasestorage.googleapis.com/v0/b/sportsbooker-mad.appspot.com/o/images%2Fprofile_picture.jpeg?alt=media&token=e5441836-e955-4a13-966b-202f0f3cd210&_gl=1*6spico*_ga*MTk2NjY0NzgxMS4xNjgzMTkzMzEy*_ga_CW55HF8NVT*MTY4NTYyMTM1MS4xNy4xLjE2ODU2MjUzMTcuMC4wLjA."

            signupVM.checkIfPlayerExists(email!!, credential)


            signupVM.userExists.observe(viewLifecycleOwner) {
                if(!it){
                    val bundle = Bundle()
                    bundle.putString("name", name)
                    bundle.putString("surname", surname)
                    bundle.putString("email", email)
                    bundle.putString("photoUrl", photoUrl)
                    bundle.putString("idToken", account.idToken)
                    signupVM.loadingState.value = false // Set loading state to false
                    navController.navigate(R.id.action_login_to_complete_registration_google, bundle)
                }
                else {
                    firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    requireActivity(),
                                    "Logged in successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showValidationError(task.exception)
                            }
                        }
                }

            }
        }
        else {
            Toast.makeText(requireActivity(), task.exception.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    private fun showValidationError(exception: Exception?) {
        if (exception is FirebaseAuthException) {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> binding.email.error = "Invalid email"
                "ERROR_WRONG_PASSWORD" -> Toast.makeText(
                    requireActivity(),
                    "Wrong email and/or password",
                    Toast.LENGTH_SHORT
                ).show()

                else -> Toast.makeText(
                    requireActivity(),
                    "Error: ${exception.errorCode}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                requireActivity(),
                "Error: ${exception?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeUserUpdates(lifecycleOwner: LifecycleOwner) {
        mainVM.user.observe(lifecycleOwner) { user ->
            // Once the user data is received, navigate to HomeActivity
            val intent = Intent(requireActivity(), MyReservationsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            signupVM.loadingState.value = false // Set loading state to false
            startActivity(intent)

        }
    }
}