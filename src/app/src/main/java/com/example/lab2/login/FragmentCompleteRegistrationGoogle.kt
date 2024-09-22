package com.example.lab2.login

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.constraintlayout.widget.Guideline
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.lab2.R
import com.example.lab2.databinding.FragmentCompleteRegistrationGoogleBinding
import com.example.lab2.entities.PartialRegistration
import com.example.lab2.view_models.SignupVM
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class FragmentCompleteRegistrationGoogle : Fragment(R.layout.fragment_complete_registration_google) {

    companion object {
        fun newInstance() = FragmentCompleteRegistrationGoogle()
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signupVM: SignupVM
    private lateinit var binding: FragmentCompleteRegistrationGoogleBinding
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        firebaseAuth = FirebaseAuth.getInstance()

        navController = findNavController()
        signupVM = ViewModelProvider(requireActivity())[SignupVM::class.java]


        val view = inflater.inflate(R.layout.fragment_complete_registration_google, container, false)
        binding = FragmentCompleteRegistrationGoogleBinding.bind(view)

        signupVM.loadingState.observe(viewLifecycleOwner) {
            if (it) {
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        signupVM.registrationFinished.observe(viewLifecycleOwner) {
            if (it) {
                navController.navigate(R.id.action_complete_registration_google_to_select_interests)
            }
        }

        binding.dateOfBirthEditText.setOnClickListener {

            val constraintsBuilder =
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build()

            Locale.setDefault(Locale.ENGLISH)
            val datePicker =
                MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setCalendarConstraints(constraintsBuilder)
                    .build()

            datePicker.addOnPositiveButtonClickListener {
                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utc.timeInMillis = it
                val format = SimpleDateFormat("yyyy-MM-dd")
                val formatted: String = format.format(utc.time)
                binding.dateOfBirthEditText.setText(formatted)
            }
            datePicker.show(childFragmentManager, "datePicker")
        }

        val leftGuideline = requireActivity().findViewById<Guideline>(R.id.guideline4)
        val rightGuideLine = requireActivity().findViewById<Guideline>(R.id.guideline5)
        val selectedTab = requireActivity().findViewById<View>(R.id.selected_view)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            selectedTab.animate()
                .x(leftGuideline.x)
                .setDuration(500)
                .start()
            if (navController.currentDestination?.id == R.id.signup
                || navController.currentDestination?.id == R.id.complete_registration_google
                || navController.currentDestination?.id == R.id.select_interests) {
                navController.navigate(R.id.action_to_login)
            }
        }
        val loginTab = requireActivity().findViewById<View>(R.id.login_text_view)
        val signupTab = requireActivity().findViewById<View>(R.id.signup_text_view)
        loginTab.setOnClickListener {
            selectedTab.animate()
                .x(leftGuideline.x)
                .setDuration(500)
                .start()
            navController.navigate(R.id.action_to_login)
        }
        signupTab.setOnClickListener {
            selectedTab.animate()
                .x(rightGuideLine.x)
                .setDuration(500)
                .start()
            navController.navigate(R.id.action_to_signup)
        }

        binding.signup.setOnClickListener {
            val dateOfBirth = binding.dateOfBirthEditText.text.toString()
            val location = binding.locationEditText.text.toString()
            val username = binding.usernameEditText.text.toString()

            signupVM.checkIfUsernameAlreadyExists(username)

            binding.dateOfBirth.error = null
            binding.location.error = null
            binding.username.error = null

            if (dateOfBirth.isNotEmpty() && location.isNotEmpty() && username.isNotEmpty()) {
                signupVM.valid.value = true
            }
            else {
                signupVM.valid.value = false
                Toast.makeText(requireActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        signupVM.validUsername.observe(viewLifecycleOwner){ usernameIsValid ->
            if(usernameIsValid && signupVM.valid.value == true){
                processSignup(
                    username = binding.usernameEditText.text.toString(),
                    dateOfBirth = binding.dateOfBirthEditText.text.toString(),
                    location = binding.locationEditText.text.toString()
                )
            }
        }

        signupVM.error.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.username.error = it
            }
        }

        return view
    }

    private fun processSignup(username: String, dateOfBirth: String, location: String ) {

        val name = arguments?.getString("name")
        val surname = arguments?.getString("surname")
        val email = arguments?.getString("email")
        val photoUrl = arguments?.getString("photoUrl")
        val idToken = arguments?.getString("idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        signupVM.loginWithGoogle(
            name = name!!,
            surname = surname!!,
            credential = credential,
            email = email!!,
            photoUrl = photoUrl!!,
            username = username,
            dateOfBirth = dateOfBirth,
            location = location
        )
    }
}