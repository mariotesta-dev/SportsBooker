package com.example.lab2.login

import android.os.Bundle
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
import com.example.lab2.databinding.FragmentSignupBinding
import com.example.lab2.view_models.SignupVM
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class FragmentSignup : Fragment(R.layout.fragment_signup) {

    companion object {
        fun newInstance() = FragmentSignup()
    }

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signupVM: SignupVM
    private lateinit var binding: FragmentSignupBinding
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        firebaseAuth = FirebaseAuth.getInstance()

        navController = findNavController()
        signupVM = ViewModelProvider(requireActivity())[SignupVM::class.java]

        signupVM.loadingState.observe(viewLifecycleOwner) {
            if (it) {
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        }

        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        binding = FragmentSignupBinding.bind(view)

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
        val selectedTab = requireActivity().findViewById<View>(R.id.selected_view)
        val rightGuideLine = requireActivity().findViewById<Guideline>(R.id.guideline5)

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
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val name = binding.nameEditText.text.toString()
            val surname = binding.surnameEditText.text.toString()
            val dateOfBirth = binding.dateOfBirthEditText.text.toString()

            //TODO it would be good if we used a component that searches for real locations/cities, to avoid typos
            val location = binding.locationEditText.text.toString()

            val username = binding.usernameEditText.text.toString()

            signupVM.checkIfUsernameAlreadyExists(username)

            binding.name.error = null
            binding.surname.error = null
            binding.dateOfBirth.error = null
            binding.location.error = null
            binding.username.error = null
            binding.email.error = null

            //TODO if passwords do not match,
            // the error exclamation mark must not overlap/replace the eye icon to show/hide
            // the password, this can be fixed either enlarging the view or moving the eye icon on
            // the left or creating a custom layout
            binding.password.error = null
            binding.confirmPassword.error = null


            if (email.isNotEmpty()
                && password.isNotEmpty() && confirmPassword.isNotEmpty()
                && name.isNotEmpty() && surname.isNotEmpty()
                && dateOfBirth.isNotEmpty() && location.isNotEmpty() && username.isNotEmpty()
            ) {

                if (password.length < 6) {
                    signupVM.valid.value = false
                    binding.password.error = "Password must be at least 6 characters long"
                    return@setOnClickListener
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    signupVM.valid.value = false
                    binding.email.error = "Invalid email"
                    return@setOnClickListener
                }

                //userId: String, name: String, surname: String, username: String, email: String, dateOfBirth: String, location: String, selectedInterests: MutableList<Sport>
                if (password == confirmPassword) {
                    signupVM.valid.value = true
                } else {
                    signupVM.valid.value = false
                    //TODO if passwords do not match,
                    // the error exclamation mark must not overlap/replace the eye icon to show/hide
                    // the password, this can be fixed either enlarging the view or moving the eye icon on
                    // the left or creating a custom layout
                    binding.password.error = "Passwords do not match"
                    binding.confirmPassword.error = "Passwords do not match"
                }
            } else {
                signupVM.valid.value = false
                Toast.makeText(requireActivity(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        signupVM.validUsername.observe(viewLifecycleOwner){ usernameIsValid ->
            if(usernameIsValid && signupVM.valid.value == true){
                processSignup(
                    name = binding.nameEditText.text.toString(),
                    surname = binding.surnameEditText.text.toString(),
                    password = binding.passwordEditText.text.toString(),
                    email = binding.emailEditText.text.toString(),
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

    private fun processSignup(name: String, surname: String, email: String, password: String, username: String, dateOfBirth: String, location: String ) {
        signupVM.loadingState.value = true
        val defaultPhoto = "https://firebasestorage.googleapis.com/v0/b/sportsbooker-mad.appspot.com/o/images%2Fprofile_picture.jpeg?alt=media&token=e5441836-e955-4a13-966b-202f0f3cd210"

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    signupVM.createPlayer(
                        userId = task.result.user?.uid!!,
                        name = name,
                        surname = surname,
                        username = username,
                        email = email,
                        dateOfBirth = dateOfBirth,
                        location = location,
                        selectedInterests = mutableListOf(),
                        photoUrl = defaultPhoto
                    )
                    val bundle = Bundle()
                    bundle.putString("uid", task.result.user?.uid)
                    signupVM.loadingState.value = false
                    navController.navigate(
                        R.id.action_signup_to_select_interests,
                        bundle
                    )

                } else {
                    showValidationError(task.exception)
                }
            }
    }



    private fun showValidationError(exception: Exception?) {
        if (exception is FirebaseAuthException) {
            when (exception.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> binding.email.error = "Email already in use"
                "ERROR_WEAK_PASSWORD" -> binding.password.error =
                    "Password must be at least 6 characters long"

                "ERROR_INVALID_EMAIL" -> binding.email.error = "Invalid email"
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


}