package com.example.lab2.common.calendar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.databinding.MonthCalendarCalendarDayBinding
import com.example.lab2.databinding.MonthCalendarFragmentBinding
import com.example.lab2.view_models.CalendarVM
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@AndroidEntryPoint
class MonthCalendar : Fragment(R.layout.month_calendar_fragment) {

    private lateinit var binding: MonthCalendarFragmentBinding
    private val monthCalendarView: CalendarView get() = binding.exOneCalendar

    private lateinit var selectedDate: LocalDate
    private val today = LocalDate.now()

    lateinit var vm: CalendarVM

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = MonthCalendarFragmentBinding.bind(view)

        vm = ViewModelProvider(requireActivity())[CalendarVM::class.java]

        vm.getSelectedDate().observe(viewLifecycleOwner) {
            selectedDate = it
            binding.exOneCalendar.scrollToMonth(selectedDate.yearMonth)
        }

        val daysOfWeek = daysOfWeek()
        binding.legendLayout.root.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                textView.text = daysOfWeek[index].displayText()
                textView.setTextColorRes(R.color.darker_blue)
                textView.setTextAppearance(view.context, R.style.MonthLightTextAppearance)
            }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        setupMonthCalendar(startMonth, endMonth, currentMonth, daysOfWeek)

    }

    private fun setupMonthCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>,
    ) {
        class DayViewContainer(view: View) : ViewContainer(view) {
            // Will be set when this container is bound. See the dayBinder.
            lateinit var day: CalendarDay
            val textView = MonthCalendarCalendarDayBinding.bind(view).exOneDayText

            init {
                view.setOnClickListener {
                    // Check the day position as we do not want to select in or out dates.
                    if (day.position == DayPosition.MonthDate && day.date >= LocalDate.now()) {
                        dateClicked(date = day.date)
                    }
                }
            }
        }
        monthCalendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                bindDate(
                    data.date,
                    container.textView,
                    (data.position == DayPosition.MonthDate && data.date >= LocalDate.now())
                )
            }
        }
        monthCalendarView.monthScrollListener = { updateTitle() }
        monthCalendarView.setup(startMonth, endMonth, daysOfWeek.first())
        monthCalendarView.scrollToMonth(currentMonth)
    }

    private fun bindDate(date: LocalDate, textView: TextView, isSelectable: Boolean) {
        textView.text = date.dayOfMonth.toString()
        if (isSelectable) {
            when {
                selectedDate == date -> {
                    textView.setTextColorRes(R.color.darker_blue)
                    textView.setBackgroundResource(R.drawable.month_calendar_selected_bg)
                }

                today == date -> {
                    textView.setTextColorRes(R.color.darker_blue)
                    textView.setBackgroundResource(R.drawable.month_calendar_today_bg)
                }

                else -> {
                    textView.setTextColorRes(R.color.darker_blue)
                    textView.background = null
                }
            }
        } else {
            textView.setTextColorRes(R.color.darker_blue_disabled)
            textView.background = null
        }
    }

    private fun dateClicked(date: LocalDate) {
        // Keep a reference to any previous selection
        // in case we overwrite it and need to reload it.
        val currentSelection = selectedDate
        if (currentSelection == date) {
            // If the user clicks the same date, clear selection.
            //selectedDate = null
            // Reload this date so the dayBinder is called
            // and we can REMOVE the selection background.
            monthCalendarView.notifyDateChanged(currentSelection)
        } else {
            selectedDate = date
            if (currentSelection != null) {
                // We need to also reload the previously selected
                // date so we can REMOVE the selection background.
                monthCalendarView.notifyDateChanged(currentSelection)
            }
        }
        // Refresh both calendar views..

        // Reload the newly selected date so the dayBinder is
        // called and we can ADD the selection background.
        monthCalendarView.notifyDateChanged(date)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        val month = monthCalendarView.findFirstVisibleMonth()?.yearMonth ?: return
        binding.exOneYearText.text = month.year.toString()
        binding.exOneMonthText.text = month.month.displayText(short = false)
    }


}
