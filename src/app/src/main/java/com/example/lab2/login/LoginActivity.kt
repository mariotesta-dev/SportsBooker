package com.example.lab2.login

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.lab2.R
import com.example.lab2.databinding.ActivityLoginBinding
import com.example.lab2.reservation.my_reservations.MyReservationsActivity
import com.example.lab2.view_models.MainVM
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.toolbar_login)

        navController = (
                supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerViewAuthentication) as NavHostFragment
                ).navController

        val selectedTab = binding.selectedView
        val loginTab = binding.loginTextView
        val signupTab = binding.signupTextView
        val leftGuideline = binding.guideline4
        val rightGuideline = binding.guideline5
        signupTab.setOnClickListener {
            selectedTab.animate()
                .x(rightGuideline.x)
                .setDuration(500)
                .start()
            if (navController.currentDestination?.id == R.id.login
                || navController.currentDestination?.id == R.id.complete_registration_google
                || navController.currentDestination?.id == R.id.select_interests) {
                navController.navigate(R.id.action_to_signup)
            }
        }
        loginTab.setOnClickListener {
            selectedTab.animate()
                .x(leftGuideline.x)
                .setDuration(500)
                .start()
            if (navController.currentDestination?.id == R.id.signup
                || navController.currentDestination?.id == R.id.select_interests
                || navController.currentDestination?.id == R.id.complete_registration_google) {
                navController.navigate(R.id.action_to_login)
            }
        }

    }

}

