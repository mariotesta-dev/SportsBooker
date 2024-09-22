package com.example.lab2.reservation.my_reservations

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.lab2.R
import com.example.lab2.reservation.book_reservation.BookReservationActivity
import com.example.lab2.reservation.edit_reservation.EditReservationActivity
import com.example.lab2.view_models.CalendarVM
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.MyReservationsVM
import com.example.lab2.view_models.RatingModalVM
import com.example.lab2.entities.MatchWithCourtAndEquipments
import com.example.lab2.reservation.utils.AdapterRVSportFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class MyReservationsFragment : Fragment(R.layout.fragment_my_reservations),
    AdapterRVMyReservations.OnEditClickListener {

    private lateinit var navController: NavController

    @Inject
    lateinit var userVM: MainVM
    lateinit var vm: MyReservationsVM
    lateinit var calendarVM: CalendarVM
    lateinit var ratingModalVM: RatingModalVM

    private lateinit var adapterCardFilters: AdapterRVSportFilter

    private lateinit var loading: ProgressBar
    private lateinit var noResults: ConstraintLayout
    private lateinit var findBookReservationButton: Button
    private lateinit var leaveRatingLayout: ConstraintLayout
    var showBanner = false

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { processResponse(it) }

    private fun processResponse(response: androidx.activity.result.ActivityResult) {
        if (response.resultCode == AppCompatActivity.RESULT_OK) {
            loading.visibility = View.VISIBLE
        }else{
            loading.visibility = View.GONE
        }
    }

    private fun showOrHideNoResultImage(list: List<MatchWithCourtAndEquipments>) {
        if (list.isEmpty()) {
            noResults.visibility = View.VISIBLE
        } else {
            noResults.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(requireActivity())[MyReservationsVM::class.java]
        calendarVM = ViewModelProvider(requireActivity())[CalendarVM::class.java]
        ratingModalVM = ViewModelProvider(requireActivity())[RatingModalVM::class.java]

        loading = view.findViewById(R.id.loading_my_reservation)
        noResults = view.findViewById(R.id.no_results)
        val listOfSportRecyclerView = view.findViewById<RecyclerView>(R.id.my_reservation_filter)

        leaveRatingLayout = view.findViewById(R.id.leave_rating_banner)


        navController = findNavController()
        requireActivity().actionBar?.elevation = 0f

        val adapterCard = AdapterRVMyReservations(emptyList(), this)
        val listReservationsRecyclerView =
            view.findViewById<RecyclerView>(R.id.your_reservation_recycler_view)
        listReservationsRecyclerView.adapter = adapterCard
        listReservationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        //TODO: Initialize with user interests
        adapterCardFilters = AdapterRVSportFilter(
            listOf(null)
                .plus(userVM.user.value!!.interests
                    .map { sport ->
                        sport.name.lowercase().replaceFirstChar { it.uppercase() }
                    }),
            vm::setSportFilter
        )

        listOfSportRecyclerView.adapter = adapterCardFilters
        listOfSportRecyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)


        userVM.user.observe(viewLifecycleOwner) {
            adapterCardFilters.setFilters(
                listOf(null)
                    .plus(userVM.user.value!!.interests
                        .map { sport ->
                            sport.name.lowercase().replaceFirstChar { it.uppercase() }
                        })
            )
        }

        vm.getMyReservations().observe(viewLifecycleOwner) {
            vm.startMatchListener()
            loading.visibility = View.GONE
            calendarVM.selectedTime.value = if(LocalDate.now() == calendarVM.getSelectedDate().value) LocalTime.now() else LocalTime.of(8, 0)
        }

        calendarVM.getSelectedDate().observe(requireActivity()) {
            if(it == LocalDate.now()) {
                calendarVM.selectedTime.value = LocalTime.now()
            } else {
                calendarVM.selectedTime.value = LocalTime.of(8, 0)
            }
            val list = vm.filterList(
                vm.getSportFilter().value,
                calendarVM.getSelectedDate().value!!,
                calendarVM.getSelectedTime().value
            )
            if (list.isNotEmpty()) listReservationsRecyclerView.scrollToPosition(0)
            adapterCard.setReservations(list)
            showOrHideNoResultImage(list)
        }

        vm.getSportFilter().observe(viewLifecycleOwner) {
            val list = vm.filterList(it, calendarVM.getSelectedDate().value!!, calendarVM.getSelectedTime().value)
            if (list.isNotEmpty()) listReservationsRecyclerView.scrollToPosition(0)
            adapterCard.setReservations(list)
            showOrHideNoResultImage(list)
        }

        calendarVM.getSelectedTime().observe(viewLifecycleOwner) {
            val list = vm.filterList(vm.getSportFilter().value, calendarVM.getSelectedDate().value!!, calendarVM.getSelectedTime().value)
            if (list.isNotEmpty()) listReservationsRecyclerView.scrollToPosition(0)
            adapterCard.setReservations(list)
            showOrHideNoResultImage(list)
        }


        findBookReservationButton = view.findViewById(R.id.find_new_games_button)
        findBookReservationButton.setOnClickListener {
            val intentBookReservation =
                Intent(requireContext(), BookReservationActivity::class.java)
            launcher.launch(intentBookReservation)
        }

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            vm.startListener()
            swipeRefreshLayout.isRefreshing = false
        }

        //TODO this tutorial must show after the user has registered theirselves, so at first login
        /* calendarVM.getShowTutorial().observe(viewLifecycleOwner) {
             val builder = AlertDialog.Builder(requireContext())
             val view = layoutInflater.inflate(R.layout.modal_tutorial, null)
             builder.setView(view)
             builder.setTitle("Swipe horizontally to change the week")
             builder.setPositiveButton("OK") { dialog, which ->
             }
             val dialog = builder.create()
             dialog.show()
         }*/

    }

    override fun onEditClick(reservation: MatchWithCourtAndEquipments) {

        val jsonString = Json.encodeToString(MatchWithCourtAndEquipments.serializer(), reservation)

        val intentEditReservation = Intent(activity, EditReservationActivity::class.java).apply {
            addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE)
            putExtra("reservationJson", jsonString)
        }
        launcher.launch(intentEditReservation)
    }

    override fun onResume() {
        super.onResume()
        vm.startMatchListener()
    }

    override fun onPause() {
        super.onPause()
        vm.stopListener()
        vm.stopMatchListener()
    }
}
