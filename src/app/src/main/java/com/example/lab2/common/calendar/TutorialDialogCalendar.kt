package com.example.lab2.common.calendar

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lab2.R
import com.example.lab2.databinding.CalendarTutorialBinding
import com.example.lab2.databinding.MonthCalendarCalendarDayBinding
import com.example.lab2.view_models.CalendarVM
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.WeekDayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters


@AndroidEntryPoint
class TutorialDialogCalendar : Fragment(R.layout.calendar_tutorial) {

    private lateinit var binding: CalendarTutorialBinding
    private val monthCalendarView: CalendarView get() = binding.exOneCalendar
    private val weekCalendarView: WeekCalendarView get() = binding.exOneWeekCalendar

    private lateinit var roundCalendarButton: ImageButton
    private lateinit var timeslotFilterButton: TextView

    private lateinit var selectedDate: LocalDate
    private lateinit var oldDate: LocalDate
    private val today = LocalDate.now()

    var weekToMonth = false

    lateinit var vm: CalendarVM

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        timeslotFilterButton = view.findViewById(R.id.timeslot_filter)
        roundCalendarButton = view.findViewById(R.id.roundCalendarButton)

        binding = CalendarTutorialBinding.bind(view)
        vm = ViewModelProvider(requireActivity())[CalendarVM::class.java]
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
        setupMonthCalendar(
            startMonth,
            endMonth,
            currentMonth,
            daysOfWeek
        )
        setupWeekCalendar(
            startMonth,
            endMonth,
            currentMonth,
            daysOfWeek
        )

        roundCalendarButton.setOnClickListener(calendarModeToggle)
        timeslotFilterButton.setOnClickListener {
            val materialTimePicker: MaterialTimePicker = MaterialTimePicker.Builder()
                .setTitleText("Pick your time")
                .setHour(vm.selectedTime.value!!.hour)
                .setMinute(vm.selectedTime.value!!.minute)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()

            materialTimePicker.show(childFragmentManager, requireActivity().toString())

            materialTimePicker.addOnPositiveButtonClickListener {

                val pickedHour: Int = materialTimePicker.hour
                val pickedMinute: Int = materialTimePicker.minute

                if (vm.getSelectedDate().value == LocalDate.now() && pickedHour < LocalTime.now().hour) {
                    Toast.makeText(activity, "Cannot select past time!", Toast.LENGTH_SHORT).show()
                    return@addOnPositiveButtonClickListener
                }
                vm.selectedTime.value = LocalTime.of(pickedHour, pickedMinute)

            }
        }

        vm.selectedTime.observe(viewLifecycleOwner) {
            timeslotFilterButton.text = it.format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        val animation: Animation =
            AlphaAnimation(1f, 0f) //to change visibility from visible to invisible
        animation.duration = 1000 //1 second duration for each animation cycle
        animation.interpolator = LinearInterpolator()
        animation.repeatCount = Animation.INFINITE //repeating indefinitely
        animation.repeatMode =
            Animation.REVERSE //animation will start from end point once ended.
        binding.swipeHand.startAnimation(animation) //to start animation*/
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
                    if (day.date >= LocalDate.now()) {
                        dateClicked(date = day.date)
                        vm.setSelectedDate(day.date)
                        monthCalendarView.scrollToDate(day.date)
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
        monthCalendarView.setup(
            startMonth,
            endMonth,
            daysOfWeek.first()
        )
        monthCalendarView.scrollToDate(LocalDate.now())
    }


    private fun setupWeekCalendar(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth,
        daysOfWeek: List<DayOfWeek>,
    ) {
        class WeekDayViewContainer(view: View) : ViewContainer(view) {
            // Will be set when this container is bound. See the dayBinder.
            lateinit var day: WeekDay
            val textView = MonthCalendarCalendarDayBinding.bind(view).exOneDayText

            init {
                view.setOnClickListener {
                    if (day.position == WeekDayPosition.RangeDate && day.date >= LocalDate.now()) {
                        dateClicked(date = day.date)
                        vm.setSelectedDate(day.date)
                        updateDate()
                    }
                }
            }
        }
        weekCalendarView.dayBinder = object : WeekDayBinder<WeekDayViewContainer> {
            override fun create(view: View): WeekDayViewContainer = WeekDayViewContainer(view)
            override fun bind(container: WeekDayViewContainer, data: WeekDay) {
                container.day = data
                bindDate(
                    data.date,
                    container.textView,
                    (data.position == WeekDayPosition.RangeDate && data.date >= LocalDate.now())
                )
            }
        }
        weekCalendarView.weekScrollListener = { updateTitle() }
        weekCalendarView.setup(
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            endMonth.atEndOfMonth(),
            daysOfWeek.first(),
        )
        weekCalendarView.scrollToDate(LocalDate.now())
    }

    private fun bindDate(date: LocalDate, textView: TextView, isSelectable: Boolean) {
        textView.text = date.dayOfMonth.toString()
        if (isSelectable) {
            when {
                vm.getSelectedDate().value == date -> {
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
        val currentSelection = vm.getSelectedDate().value
        if (currentSelection == date) {
            // If the user clicks the same date, clear selection.
            //selectedDate = null
            // Reload this date so the dayBinder is called
            // and we can REMOVE the selection background.
            monthCalendarView.notifyDateChanged(currentSelection)
            weekCalendarView.notifyDateChanged(currentSelection)
        } else {
            vm.setSelectedDate(date)
            if (currentSelection != null) {
                // We need to also reload the previously selected
                // date so we can REMOVE the selection background.
                monthCalendarView.notifyDateChanged(currentSelection)
                weekCalendarView.notifyDateChanged(currentSelection)
            }
        }
        // Refresh both calendar views..

        // Reload the newly selected date so the dayBinder is
        // called and we can ADD the selection background.
        monthCalendarView.notifyDateChanged(date)
        weekCalendarView.notifyDateChanged(date)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTitle() {
        if (weekToMonth) {
            val month = monthCalendarView.findFirstVisibleMonth()?.yearMonth ?: return
            binding.exOneYearText.text = month.year.toString()
            binding.exOneMonthText.text = month.month.displayText(short = false)
        } else {
            val week = weekCalendarView.findFirstVisibleWeek() ?: return
            // In week mode, we show the header a bit differently because
            // an index can contain dates from different months/years.
            val firstDate = week.days.first().date
            val lastDate = week.days.last().date
            if (firstDate.yearMonth == lastDate.yearMonth) {
                binding.exOneYearText.text = firstDate.year.toString()
                binding.exOneMonthText.text = firstDate.month.displayText(short = false)
            } else {
                binding.exOneMonthText.text =
                    firstDate.month.displayText(short = false) + " - " +
                            lastDate.month.displayText(short = false)
                if (firstDate.year == lastDate.year) {
                    binding.exOneYearText.text = firstDate.year.toString()
                } else {
                    binding.exOneYearText.text = "${firstDate.year} - ${lastDate.year}"
                }
            }

        }
    }


    private val calendarModeToggle = object : View.OnClickListener {
        override fun onClick(view: View) {
            weekToMonth = !weekToMonth
            // We want the first visible day to remain visible after the
            // change so we scroll to the position on the target calendar.
            if (!weekToMonth) {
                val targetDate = monthCalendarView.findFirstVisibleDay()?.date ?: return
                weekCalendarView.scrollToWeek(vm.getSelectedDate().value!!)
            } else {
                // It is possible to have two months in the visible week (30 | 31 | 1 | 2 | 3 | 4 | 5)
                // We always choose the second one. Please use what works best for your use case.
                val targetMonth = weekCalendarView.findLastVisibleDay()?.date?.yearMonth ?: return
                monthCalendarView.scrollToMonth(targetMonth)
            }

            val weekHeight = weekCalendarView.height
            // If OutDateStyle is EndOfGrid, you could simply multiply weekHeight by 6.
            val visibleMonthHeight = weekHeight *
                    monthCalendarView.findFirstVisibleMonth()?.weekDays.orEmpty().count()

            val oldHeight = if (!weekToMonth) visibleMonthHeight else weekHeight
            val newHeight = if (!weekToMonth) weekHeight else visibleMonthHeight

            // Animate calendar height changes.
            val animator = ValueAnimator.ofInt(oldHeight, newHeight)
            animator.addUpdateListener { anim ->
                monthCalendarView.updateLayoutParams {
                    height = anim.animatedValue as Int
                }
                // A bug is causing the month calendar to not redraw its children
                // with the updated height during animation, this is a workaround.
                monthCalendarView.children.forEach { child ->
                    child.requestLayout()
                }
            }

            animator.doOnStart {
                if (weekToMonth) {
                    weekCalendarView.isInvisible = true
                    monthCalendarView.isVisible = true
                }
            }
            animator.doOnEnd {
                if (!weekToMonth) {
                    weekCalendarView.isVisible = true
                    monthCalendarView.isInvisible = true
                } else {
                    // Allow the month calendar to be able to expand to 6-week months
                    // in case we animated using the height of a visible 5-week month.
                    // Not needed if OutDateStyle is EndOfGrid.
                    monthCalendarView.updateLayoutParams { height = WRAP_CONTENT }
                }
                updateTitle()
            }
            animator.duration = 250
            animator.start()
        }
    }

    fun updateDate() {
        vm.selectedTime.value =
            if (vm.getSelectedDate().value == LocalDate.now()) LocalTime.now() else LocalTime.of(
                8,
                0
            )
        weekCalendarView.notifyDateChanged(vm.getSelectedDate().value!!)
    }

}
