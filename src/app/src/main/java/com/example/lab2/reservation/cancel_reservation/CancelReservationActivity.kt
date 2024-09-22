package com.example.lab2.reservation.cancel_reservation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.view_models.EditReservationViewModel
import com.example.lab2.view_models.MainVM
import com.example.lab2.entities.MatchWithCourtAndEquipments
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@AndroidEntryPoint
class CancelReservationActivity : AppCompatActivity() {


    private lateinit var reservation: MatchWithCourtAndEquipments

    private lateinit var sport_name: TextView
    private lateinit var court_name_cancel_reservation: TextView
    private lateinit var location_cancel_reservation: TextView
    private lateinit var date_cancel_reservation: TextView
    private lateinit var time_cancel_reservation: TextView
    private lateinit var cancelButton: Button
    private lateinit var menuItem: MenuItem
    private lateinit var backButton: ImageView

    private lateinit var cancelContainer: ConstraintLayout
    private lateinit var loadingContainer: ConstraintLayout


    @Inject
    lateinit var mainVM: MainVM

    private lateinit var editReservationVM: EditReservationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Cancel)
        setContentView(R.layout.activity_cancel_reservation)
        setSupportActionBar()
        findViews()

        editReservationVM = ViewModelProvider(this)[EditReservationViewModel::class.java]

        val reservationString = intent.getStringExtra("resString")
        reservation = Json.decodeFromString(MatchWithCourtAndEquipments.serializer(), reservationString!!)

        updateContent()

        editReservationVM.error.observe(this) {
            if (it != null) {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
                editReservationVM.cancelReservation(mainVM.userId, reservation)
        }

        editReservationVM.submitEditSuccess.observe(this){
            if(it) {
                val res: Intent = Intent()
                res.putExtra("result", true)
                setResult(Activity.RESULT_OK, res)
                finish()
            }
        }

        editReservationVM.loadingState.observe(this){
            setLoadingScreen(it)
        }
    }

    private fun setLoadingScreen(state: Boolean) {
        if(state) { //is Loading
            cancelContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }else{ // is not loading
            loadingContainer.visibility = View.GONE
            cancelContainer.visibility = View.VISIBLE
        }
    }

    private fun findViews() {
        sport_name = findViewById(R.id.sport_name_cancel_reservation)
        court_name_cancel_reservation = findViewById(R.id.court_name_cancel_reservation)
        location_cancel_reservation = findViewById(R.id.location_cancel_reservation)
        date_cancel_reservation = findViewById(R.id.date_cancel_reservation)
        time_cancel_reservation = findViewById(R.id.time_cancel_reservation)
        cancelButton = findViewById(R.id.cancel_button_cancel_reservation)

        cancelContainer = findViewById(R.id.cancel_container)
        loadingContainer = findViewById(R.id.loading_cancel)
    }

    private fun setSupportActionBar() {
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.toolbar)
        supportActionBar?.elevation = 0f
        supportActionBar?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.color.bright_red))
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = "Cancel my reservation"

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }


    private fun updateContent() {
        sport_name.text = "${reservation.court.sport}"
        court_name_cancel_reservation.text = "${reservation.court.name}"
        location_cancel_reservation.text = "Via Giovanni Magni, 32"
        val dayMonth =
            reservation.match.date.format(DateTimeFormatter.ofPattern("dd MMM")).split(" ")
        val day = dayMonth[0]
        val month = dayMonth[1].replaceFirstChar { it.uppercase() }
        val formattedDate = "$day $month"
        date_cancel_reservation.text = formattedDate
        time_cancel_reservation.text =
            reservation.match.time.format(DateTimeFormatter.ofPattern("HH:mm")).toString()
    }
}