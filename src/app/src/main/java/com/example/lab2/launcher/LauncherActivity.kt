package com.example.lab2.launcher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lab2.R
import com.example.lab2.login.LoginActivity
import com.example.lab2.reservation.my_reservations.MyReservationsActivity
import com.example.lab2.view_models.MainVM
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LauncherActivity : AppCompatActivity() {

    @Inject
    lateinit var vm: MainVM

    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        auth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in, listen to user updates
                vm.listenToUserUpdates(user.uid)
                observeUserUpdates()
            } else {
                // User is signed out, navigate to LoginActivity
                navigateToLoginActivity()
            }
        }
    }

    private fun observeUserUpdates() {
        vm.user.observe(this) { user ->
            // Once the user data is received, navigate to HomeActivity
            navigateToHomeActivity()
        }
    }

    private fun navigateToLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, MyReservationsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener)
        }
    }
}
