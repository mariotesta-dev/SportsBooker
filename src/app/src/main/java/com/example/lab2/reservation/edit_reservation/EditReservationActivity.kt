package com.example.lab2.reservation.edit_reservation

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.entities.Equipment
import com.example.lab2.reservation.cancel_reservation.CancelReservationActivity
import com.example.lab2.view_models.EditReservationViewModel
import com.example.lab2.view_models.EquipmentsVM
import com.example.lab2.view_models.MainVM
import com.example.lab2.entities.MatchWithCourtAndEquipments
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class EditReservationActivity : AppCompatActivity() {

    @Inject
    lateinit var mainVM: MainVM

    private lateinit var reservation: MatchWithCourtAndEquipments

    private lateinit var court_name_edit_reservation: TextView
    private lateinit var location_edit_reservation: TextView
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView
    private lateinit var priceText: TextView
    private lateinit var sport_name: TextView

    private lateinit var editContainer: ConstraintLayout
    private lateinit var loadingContainer: ConstraintLayout

    private lateinit var chipGroup: ChipGroup
    var selectedText: String = ""
    var time: String? = ""
    private var noTimeslotSelected = false

    lateinit var equipmentsVM: EquipmentsVM
    lateinit var editReservationVM: EditReservationViewModel

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { processResponse(it) }

    private fun processResponse(response: androidx.activity.result.ActivityResult) {
        if (response.resultCode == RESULT_OK) {
            val data: Intent? = response.data
            val cancel = data?.getBooleanExtra("result", false)
            if (cancel == true) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reservation)
        setSupportActionBar()
        findViews()

        equipmentsVM = ViewModelProvider(this)[EquipmentsVM::class.java]
        editReservationVM = ViewModelProvider(this)[EditReservationViewModel::class.java]


        cancelButton.setOnClickListener {
            val intent = Intent(this, CancelReservationActivity::class.java)

            val resString =
                Json.encodeToString(MatchWithCourtAndEquipments.serializer(), reservation)
            intent.putExtra("resString", resString)

            launcher.launch(intent)
        }

        val reservationString = intent.getStringExtra("reservationJson")
        reservation =
            Json.decodeFromString(MatchWithCourtAndEquipments.serializer(), reservationString!!)

        val equipments: MutableList<Equipment> = reservation.equipments.toMutableList()
        equipmentsVM.setEquipments(equipments)

        sport_name.text = reservation.court.sport
        court_name_edit_reservation.text = reservation.court.name
        location_edit_reservation.text = "Via Giovanni Magni, 32"

        // Fetch from the db the other reservations for the same court
        editReservationVM.fetchAvailableMatches(
            date = reservation.match.date,
            playerId = mainVM.userId,
            courtId = reservation.court.courtId,
            currentTimeslot = reservation.match.time
        )

        editReservationVM.setEditedReservation(
            MatchWithCourtAndEquipments(
                reservation.reservationId,
                reservation.match,
                reservation.court,
                reservation.equipments,
                reservation.finalPrice
            )
        )

        editReservationVM.getAvailableMatches().observe(this) {
            updateContent()
        }

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        saveButton.setOnClickListener {
            if (noTimeslotSelected) {
                Toast.makeText(applicationContext, "Please, select a timeslot", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            submitChanges()
        }

        editReservationVM.error.observe(this) {
            if (it != null) {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
            }
        }

        editReservationVM.submitEditSuccess.observe(this){
            if(it && !reservation.isEqualTo(editReservationVM.getEditedReservation().value)) {
                setResult(Activity.RESULT_OK)
                finish()
            }else if(it){
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        editReservationVM.loadingState.observe(this){
            setLoadingScreen(it)
        }

    }

    private fun findViews() {
        sport_name = findViewById(R.id.sport_name_edit_reservation)
        court_name_edit_reservation = findViewById(R.id.court_name_edit_reservation)
        location_edit_reservation = findViewById(R.id.location_edit_reservation)
        priceText = findViewById(R.id.local_price_edit_reservation)
        saveButton = findViewById(R.id.save_button_edit_reservation)
        chipGroup = findViewById(R.id.time_slots_chip_group_edit_reservation)
        cancelButton = findViewById(R.id.cancel_button_edit_reservation)
        editContainer = findViewById(R.id.edit_container)
        loadingContainer = findViewById(R.id.loading_edit)
    }

    private fun setLoadingScreen(state: Boolean) {
        if(state) { //is Loading
            editContainer.visibility = View.GONE
            loadingContainer.visibility = View.VISIBLE
        }else{ // is not loading
            loadingContainer.visibility = View.GONE
            editContainer.visibility = View.VISIBLE
        }
    }

    private fun submitChanges() {
        editReservationVM.setEditedEquipment(equipmentsVM.getEquipments().value!!)
        editReservationVM.setEditedPrice(equipmentsVM.getPersonalPrice().value!!)
        editReservationVM.submitUpdate(mainVM.userId, oldReservation = reservation)
    }

    private fun updateContent() {

        setupCheckboxes(editReservationVM.getEditedReservation().value!!.finalPrice)
        chipGroup.removeAllViews()
        for (r in editReservationVM.getAvailableMatches().value!!) {
            val inflater = LayoutInflater.from(chipGroup.context)
            val chip = inflater.inflate(R.layout.timeslot_chip, chipGroup, false) as Chip
            chip.isClickable = true
            chip.isCheckable = true
            chip.text = "${r.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"

            if (chip.text == editReservationVM.getEditedReservation().value?.match?.time?.truncatedTo(
                    ChronoUnit.MINUTES
                ).toString()
            ) {
                chip.isChecked = true
                chip.setChipBackgroundColorResource(R.color.darker_blue)
            } else {
                chip.setChipBackgroundColorResource(androidx.appcompat.R.color.material_blue_grey_800)
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.setChipBackgroundColorResource(R.color.darker_blue)
                } else {
                    chip.setChipBackgroundColorResource(androidx.appcompat.R.color.material_blue_grey_800)
                }
            }
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = findViewById<Chip>(checkedIds.first())
                selectedText = selectedChip.text.toString()
                setNewMatchByTimeslot()
                noTimeslotSelected = false
            } else {
                noTimeslotSelected = true
            }
        }
    }

    private fun setNewMatchByTimeslot() {
        val foundMatch = editReservationVM.getAvailableMatches().value
            ?.find { it.time.truncatedTo(ChronoUnit.MINUTES) == LocalTime.parse(selectedText) }
        editReservationVM.setEditedMatch(foundMatch!!)
    }

    private fun setupCheckboxes(startingPrice: Double) {

        val checkboxContainer = findViewById<LinearLayout>(R.id.checkbox_container)

        checkboxContainer.removeAllViews()

        equipmentsVM.setPersonalPrice(startingPrice)

        val listEquipments = equipmentsVM.getListEquipments(reservation.court.sport!!)

        for (e in listEquipments) {
            val checkbox = CheckBox(this)
            val price = String.format("%.02f", e.price)
            checkbox.text = "${e.name} - €$price"

            if (editReservationVM.getEditedReservation().value!!.equipments.any { it.name == e.name })
                checkbox.isChecked = true

            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    equipmentsVM.setPersonalPrice(equipmentsVM.getPersonalPrice().value?.plus(e.price)!!)
                    equipmentsVM.addEquipment(e)
                } else {
                    equipmentsVM.setPersonalPrice(equipmentsVM.getPersonalPrice().value?.minus(e.price)!!)
                    equipmentsVM.removeEquipment(e)
                }
            }
            checkboxContainer.addView(checkbox)
        }

        equipmentsVM.getPersonalPrice().observe(this) {
            priceText.text = "You will pay €${String.format("%.02f", it)} locally."
        }
    }

    private fun setSupportActionBar() {
        supportActionBar?.elevation = 0f
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.example_1_bg)))
        supportActionBar?.setCustomView(R.layout.toolbar)
        val titleTextView =
            supportActionBar?.customView?.findViewById<TextView>(R.id.custom_toolbar_title)
        titleTextView?.text = "Edit Reservations"

        backButton = supportActionBar?.customView?.findViewById<ImageView>(R.id.custom_back_icon)!!
    }

    override fun onResume() {
        super.onResume()
        chipGroup.removeAllViews()
    }

}