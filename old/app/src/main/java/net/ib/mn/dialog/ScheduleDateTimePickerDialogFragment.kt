package net.ib.mn.dialog

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.CalendarView.OnDateChangeListener
import android.widget.NumberPicker
import android.widget.TimePicker
import android.widget.TimePicker.OnTimeChangedListener
import androidx.appcompat.widget.AppCompatButton
import net.ib.mn.R
import net.ib.mn.databinding.DialogDateTimePickerBinding
import net.ib.mn.utils.Util
import java.util.Calendar
import java.util.Date

class ScheduleDateTimePickerDialogFragment : BaseDialogFragment(), OnTimeChangedListener,
    OnDateChangeListener {
    private var mDate: Date? = null

    private var _binding: DialogDateTimePickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDateTimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
        mDate!!.year = year - 1900
        mDate!!.month = month
        mDate!!.date = dayOfMonth
    }

    override fun onTimeChanged(view: TimePicker, hourOfDay: Int, minute: Int) {
        Util.log(hourOfDay.toString() + ":" + minute * TIME_PICKER_INTERVAL)
        mDate!!.hours = hourOfDay
        mDate!!.minutes =
            minute * TIME_PICKER_INTERVAL
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mDate == null) {
            mDate = Date()
        }

        //date.setMinDate();
        val cal = Calendar.getInstance()
        binding.time.setOnTimeChangedListener(this)
        binding.date.setOnDateChangeListener(this)

        val confirmBtn = view.findViewById<AppCompatButton>(R.id.btn_confirm)
        val cancelBtn = view.findViewById<AppCompatButton>(R.id.btn_cancel)

        confirmBtn.setOnClickListener { v: View? ->
            if (mDate!!.minutes < TIME_PICKER_INTERVAL) mDate!!.minutes =
                mDate!!.minutes / TIME_PICKER_INTERVAL
            val result = Intent()
            result.putExtra("date", mDate)
            setResult(result)
            setResultCode(RESULT_OK)
            startDate = null
            dismiss()
        }

        cancelBtn.setOnClickListener { v: View? ->
            setResultCode(RESULT_CANCEL)
            dismiss()
        }

        try {
            val minutePicker = binding.time.findViewById<NumberPicker>(
                Resources.getSystem().getIdentifier(
                    "minute", "id", "android"
                )
            )
            minutePicker.minValue = 0
            minutePicker.maxValue =
                (60 / TIME_PICKER_INTERVAL) - 1
            val displayedValues: MutableList<String> = ArrayList()
            var i = 0
            while (i < 60) {
                displayedValues.add(String.format("%02d", i))
                i += TIME_PICKER_INTERVAL
            }
            minutePicker.value =
                mDate!!.minutes / TIME_PICKER_INTERVAL
            minutePicker.displayedValues = displayedValues.toTypedArray<String>()
        } catch (e: Exception) {
            e.stackTrace
        }


        binding.date.setDate(startDate!!.time)
        val minutePicker = binding.time.findViewById<NumberPicker>(
            Resources.getSystem().getIdentifier(
                "minute", "id", "android"
            )
        )
        minutePicker.value =
            startDate!!.minutes / TIME_PICKER_INTERVAL
        val hourPicker = binding.time.findViewById<NumberPicker>(
            Resources.getSystem().getIdentifier(
                "hour", "id", "android"
            )
        )
        hourPicker.value = startDate!!.hours
        val amPmPicker = binding.time.findViewById<NumberPicker>(
            Resources.getSystem().getIdentifier("amPm", "id", "android")
        )
        if (startDate!!.hours >= 12) {
            amPmPicker.value = 1
        } else {
            amPmPicker.value = 0
        }

        mDate = startDate


        if (allday == 1) binding.time.setEnabled(false)

        mDate!!.minutes =
            (mDate!!.minutes / TIME_PICKER_INTERVAL) * TIME_PICKER_INTERVAL
        mDate!!.seconds = 0
    }

    companion object {
        private const val TIME_PICKER_INTERVAL = 5
        private var startDate: Date? = null
        private var allday = 0

        fun getInstance(date: Date?, all: Int): ScheduleDateTimePickerDialogFragment {
            val fragment = ScheduleDateTimePickerDialogFragment()
            fragment.setStyle(STYLE_NO_TITLE, 0)
            val args = Bundle()
            fragment.arguments = args
            startDate = date
            allday = all
            return fragment
        }
    }
}
