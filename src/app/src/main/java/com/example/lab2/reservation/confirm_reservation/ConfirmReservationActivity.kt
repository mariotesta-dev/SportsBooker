package com.example.lab2.reservation.confirm_reservation

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.entities.Equipment
import com.example.lab2.entities.Court
import com.example.lab2.entities.Match
import com.example.lab2.entities.MatchWithCourt
import com.example.lab2.entities.MatchWithCourtAndEquipments
import com.example.lab2.view_models.ConfirmReservationVM
import com.example.lab2.view_models.EquipmentsVM
import com.example.lab2.view_models.MainVM
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.concurrent.thread

@AndroidEntryPoint
class ConfirmReservationActivity : AppCompatActivity() {

    private lateinit var matchWithCourt: MatchWithCourt
    private lateinit var match: Match
    private lateinit var court: Court
    private lateinit var sport_name: TextView
    private lateinit var court_name_confirm_reservation: TextView
    private lateinit var location_confirm_reservation: TextView
    private lateinit var date_confirm_reservation: TextView
    private lateinit var time_confirm_reservation: TextView
    private lateinit var equipments_card_title: TextView
    private lateinit var confirmButton: Button
    private lateinit var priceText: TextView
    private lateinit var backButton: ImageView
    private lateinit var checkboxContainer: LinearLayout
    private lateinit var playerId: String

    lateinit var confirmReservationVM: ConfirmReservationVM

    lateinit var equipmentsVM: EquipmentsVM

    @Inject
    lateinit var vm: MainVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_booking)
        setSupportActionBar()
        findViews()

        equipmentsVM = ViewModelProvider(this)[EquipmentsVM::class.java]
        confirmReservationVM = ViewModelProvider(this)[ConfirmReservationVM::class.java]

        playerId = vm.userId

        val matchWithCourtString = intent.getStringExtra("jsonMatch")
        matchWithCourt = Json.decodeFromString(MatchWithCourt.serializer(), matchWithCourtString!!)
        match = matchWithCourt.match
        court = matchWithCourt.court


        updateContent()

        confirmReservationVM.error.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        confirmReservationVM.submitConfirmSuccess.observe(this) {
            if (it == true) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }


    }

    private fun updateContent() {
        sport_name.text = "${court.sport}"
        court_name_confirm_reservation.text = "${court.name}"
        location_confirm_reservation.text = "Via Giovanni Magni, 32"
        val dayMonth = match.date.format(DateTimeFormatter.ofPattern("dd MMM")).split(" ")
        val day = dayMonth[0]
        val month = dayMonth[1].replaceFirstChar { it.uppercase() }
        val formattedDate = "$day $month"
        date_confirm_reservation.text = formattedDate
        time_confirm_reservation.text =
            match.time.format(DateTimeFormatter.ofPattern("HH:mm")).toString()

        //val equipments = equipmentsBySport(court.sport)
        //TODO setup checkboxes needs a price, which is the starting base price for that sport
        setupCheckboxes(court.basePrice!!)

        confirmButton.setOnClickListener {
            val listEquipments = mutableListOf<Equipment>()
            for (i in 0 until checkboxContainer.childCount) {
                val view = checkboxContainer.getChildAt(i)
                if (view is CheckBox) {
                    if (view.isChecked) {
                        val text = view.text.toString()
                        val parts = text.split(" - €")
                        val name = parts[0]
                        val price = parts[1]
                        listEquipments.add(Equipment(name, price.toDouble()))
                    }
                }
            }
                if (match.numOfPlayers < court.maxNumberOfPlayers!!) {
                        confirmReservationVM.addReservation(
                            MatchWithCourtAndEquipments(
                                match = match,
                                court = court,
                                equipments = listEquipments,
                                finalPrice = equipmentsVM.getPersonalPrice().value!!
                            )
                        )
                } else {
                    MainScope().launch {
                        Toast.makeText(
                            applicationContext,
                            "The maximum number of players is reached.",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        }
    }

    private fun setupCheckboxes(startingPrice: Double) {

        equipmentsVM.setPersonalPrice(startingPrice)
        val equipments = equipmentsVM.getListEquipments(court.sport!!)

        if (equipments.isEmpty()) {
            equipments_card_title.text = "No additional equipment available."
        }

        for (e in equipments) {
            val checkbox = CheckBox(this)
            checkbox.text = "${e.name} - €${e.price}"
            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    equipmentsVM.setPersonalPrice(equipmentsVM.getPersonalPrice().value?.plus(e.price)!!)
                } else {
                    equipmentsVM.setPersonalPrice(equipmentsVM.getPersonalPrice().value?.minus(e.price)!!)
                }
            }
            checkboxContainer.addView(checkbox)
        }

        equipmentsVM.getPersonalPrice().observe(this) {
            priceText.text = "You will pay €${
                String.format(
                    "%.02f",
                    equipmentsVM.getPersonalPrice().value
                )
            } locally."
        }
    }

    private fun findViews() {
        sport_name = findViewById(R.id.sport_name_confirm_reservation)
        court_name_confirm_reservation = findViewById(R.id.court_name_confirm_reservation)
        location_confirm_reservation = findViewById(R.id.location_confirm_reservation)
        date_confirm_reservation = findViewById(R.id.date_confirm_reservation)
        time_confirm_reservation = findViewById(R.id.time_confirm_reservation)
        checkboxContainer = findViewById(R.id.checkbox_container)
        confirmButton = findViewById(R.id.confirm_button_confirm_reservation)
        priceText = findViewById(R.id.local_price_confirm_reservation2)
        equipments_card_title = findViewById(R.id.equipments_title)
    }

    private fun setSupportActionBar() {
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.elevation = 0F
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.setCustomView(R.layout.toolbar)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = "Confirm Reservation"

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}