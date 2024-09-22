package com.example.lab2.reservation.book_reservation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.lab2.match.create_match.CreateMatchActivity
import com.example.lab2.reservation.confirm_reservation.ConfirmReservationActivity
import com.example.lab2.reservation.utils.AdapterRVSportFilter
import com.example.lab2.view_models.CalendarVM
import com.example.lab2.view_models.MainVM
import com.example.lab2.view_models.NewMatchesVM
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject


@AndroidEntryPoint
class BookReservationFragment : Fragment(R.layout.fragment_book_reservation) {


    private lateinit var navController: NavController

    lateinit var calendarVM: CalendarVM

    @Inject
    lateinit var userVM: MainVM
    lateinit var vm: NewMatchesVM


    private lateinit var noResults: ConstraintLayout
    private lateinit var addMatchButton: FloatingActionButton
    private lateinit var loading: ProgressBar

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { processResponse(it) }

    private fun processResponse(response: androidx.activity.result.ActivityResult) {
        if (response.resultCode == AppCompatActivity.RESULT_OK) {
            val data: Intent? = response.data
            vm.loadNewMatches(
                playerId = userVM.userId,
                date = calendarVM.getSelectedDate().value!!,
                time = calendarVM.getSelectedTime().value!!,
                interests = userVM.user.value!!.interests.toList()
            )
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        }
    }

    private fun showOrHideNoResultImage() {
        if (vm.getMapNewMatches().value.isNullOrEmpty()) {
            noResults.visibility = View.VISIBLE
        } else {
            noResults.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = findNavController()

        vm = ViewModelProvider(requireActivity())[NewMatchesVM::class.java]
        calendarVM = ViewModelProvider(requireActivity())[CalendarVM::class.java]

        loading = view.findViewById(R.id.loading_find_new_game)
        addMatchButton = view.findViewById(R.id.add_match)
        addMatchButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateMatchActivity::class.java)
            launcher.launch(intent)
        }

        val adapterCard = AdapterRVListMatches(emptyMap())
        adapterCard.setOnClickReservationListener(object :
            AdapterRVListMatches.OnClickReservation {
            override fun onClickReservation(bundle: Bundle) {
                val intent = Intent(requireContext(), ConfirmReservationActivity::class.java)
                intent.putExtras(bundle)
                launcher.launch(intent)
            }
        })
        val listReservationsRecyclerView = view.findViewById<RecyclerView>(R.id.available_bookings)
        listReservationsRecyclerView.adapter = adapterCard
        listReservationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapterCardFilters =
            AdapterRVSportFilter(listOf(null, "Padel", "Football", "Something"), vm::setSportFilter)
        val listOfSportRecyclerView = view.findViewById<RecyclerView>(R.id.filters_find_game)
        listOfSportRecyclerView.adapter = adapterCardFilters
        listOfSportRecyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

        noResults = view.findViewById(R.id.no_results)

        userVM.user.observe(viewLifecycleOwner) {
            adapterCardFilters.setFilters(listOf(null).plus(userVM.user.value!!.interests.map { sport ->
                sport.name.lowercase().replaceFirstChar { it.uppercase() }
            }))
        }

        vm.getNewMatches().observe(requireActivity()) {
            loading.visibility = View.GONE
            //calendarVM.selectedTime.value = if(LocalDate.now() == calendarVM.getSelectedDate().value) LocalTime.now() else LocalTime.of(8, 0)
            showOrHideNoResultImage()
            adapterCard.setListCourts(it)
        }

        calendarVM.getSelectedDate().observe(viewLifecycleOwner) {
            Log.d("DATETIME", "Date changed to $it")
            calendarVM.selectedTime.value = if(LocalDate.now() == calendarVM.getSelectedDate().value) LocalTime.now() else LocalTime.of(8, 0)
        }

        vm.getSportFilter().observe(viewLifecycleOwner) {
            loading.visibility = View.VISIBLE
            vm.loadNewMatches(
                playerId = userVM.userId,
                date = calendarVM.getSelectedDate().value!!,
                time = calendarVM.getSelectedTime().value!!,
                interests = userVM.user.value!!.interests.toList()
            )
        }

        calendarVM.getSelectedTime().observe(viewLifecycleOwner) {
            Log.d("DATETIME", "Time changed to $it")
            loading.visibility = View.VISIBLE
            vm.loadNewMatches(
                playerId = userVM.userId,
                date = calendarVM.getSelectedDate().value!!,
                time = it,
                interests = userVM.user.value!!.interests.toList()
            )
        }

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener {
            vm.loadNewMatches(
                playerId = userVM.userId,
                date = calendarVM.getSelectedDate().value!!,
                time = calendarVM.getSelectedTime().value!!,
                interests = userVM.user.value!!.interests.toList()
            )
            swipeRefreshLayout.isRefreshing = false
        }

    }
}
