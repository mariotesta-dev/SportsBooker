package com.example.lab2.match.create_match

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.entities.Sport
import com.example.lab2.view_models.CalendarVM
import com.example.lab2.view_models.CreateMatchVM
import com.example.lab2.view_models.MainVM
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class CreateMatchActivity : AppCompatActivity() {

    private lateinit var timeslotLayout: TextInputLayout
    private lateinit var timeslotAutocompleteTextView: AutoCompleteTextView
    private lateinit var sportAutoCompleteTV: AutoCompleteTextView
    private lateinit var confirmButton: Button
    private lateinit var backButton: ImageView


    lateinit var calendarVM: CalendarVM

    @Inject
    lateinit var userVM: MainVM

    lateinit var createMatchVM: CreateMatchVM


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_match)

        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.toolbar)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = "Create a match"


        calendarVM = ViewModelProvider(this)[CalendarVM::class.java]
        createMatchVM = ViewModelProvider(this)[CreateMatchVM::class.java]

        timeslotLayout = findViewById(R.id.reservation_card_time)
        timeslotAutocompleteTextView = findViewById(R.id.autoCompleteTextView2)
        sportAutoCompleteTV = findViewById(R.id.autoCompleteTextView)
        confirmButton = findViewById(R.id.confirm_button_confirm_reservation)

        Log.d("CreateMatchActivity", "onCreate: ${userVM.user.value!!.interests}")


        val sportArrayAdapter =
            ArrayAdapter(applicationContext, R.layout.dropdown_item, enumValues<Sport>())
        sportAutoCompleteTV.setAdapter(sportArrayAdapter)

        createMatchVM.getListTimeslots().observe(this) {
            if (it.isNullOrEmpty()) {
                timeslotAutocompleteTextView.setText("No timeslots available")
                timeslotAutocompleteTextView.setHintTextColor(getColor(R.color.example_1_white_light))
                timeslotLayout.isEnabled = false
            } else {
                timeslotLayout.isEnabled = true
                timeslotAutocompleteTextView.setText("")
                timeslotAutocompleteTextView.hint = "Select a timeslot"
                timeslotAutocompleteTextView.setHintTextColor(getColor(R.color.timeslot))
                val timeArrayAdapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, it)
                timeslotAutocompleteTextView.setAdapter(timeArrayAdapter)
            }
        }

        calendarVM.getSelectedDate().observe(this) {
            createMatchVM.filterTimeslots(it)
        }

        confirmButton.setOnClickListener {
            if (createMatchVM.getListTimeslots().value!!.find { it == timeslotAutocompleteTextView.text.toString() } == null
            ) {
                Toast.makeText(this, "Please, fill in all the fields", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val formattedSport =
                sportAutoCompleteTV.text.toString().lowercase().replaceFirstChar { it.uppercase() }
            val time = LocalTime.parse(
                timeslotAutocompleteTextView.text.toString(),
                DateTimeFormatter.ofPattern("HH:mm")
            )
            createMatchVM.createMatch(
                calendarVM.getSelectedDate().value!!,
                time,
                formattedSport,
                userVM.userId
            )
        }

        createMatchVM.getExceptionMessage().observe(this) {
            if (it == "Match created successfully") {
                setResult(Activity.RESULT_OK)
                finish()
            }
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        val arrayAdapter =
            ArrayAdapter(applicationContext, R.layout.dropdown_item, enumValues<Sport>())
        sportAutoCompleteTV.setAdapter(arrayAdapter)
        val arrayAdapter2 = ArrayAdapter(
            applicationContext,
            R.layout.dropdown_item,
            createMatchVM.getListTimeslots().value!!
        )
        timeslotAutocompleteTextView.setAdapter(arrayAdapter2)
    }

}