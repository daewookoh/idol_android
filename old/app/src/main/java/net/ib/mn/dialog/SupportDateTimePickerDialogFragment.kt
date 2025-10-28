package net.ib.mn.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.CalendarView.OnDateChangeListener
import android.widget.TimePicker
import android.widget.TimePicker.OnTimeChangedListener
import androidx.appcompat.widget.AppCompatButton
import net.ib.mn.R
import net.ib.mn.databinding.DialogDatePickerBinding
import net.ib.mn.databinding.DialogDateTimePickerBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class SupportDateTimePickerDialogFragment(//WriteActivty로부터 넘어온 날짜.
    private val period: String, private var mDate: Date?, //이전에 고른날짜.
    private val limitDate: Date?
) :
    BaseDialogFragment(), OnTimeChangedListener, OnDateChangeListener {
    //시작일.
    private var startDate: Date? = null

    //서포트 기간 마지막 날.
    private var lastDayOfSupport: Calendar? = null

    //고를 수 있는 마지막날(현재 100일)
    private var maxPickDay: Calendar? = null

    private var _binding: DialogDatePickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDatePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSelectedDayChange(view: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
        // month: 0 based
        val c = Calendar.getInstance(Const.TIME_ZONE_KST)
        // 한국시간 0시로 설정
        c[year, month] = dayOfMonth // 0 based
        c[Calendar.HOUR_OF_DAY] = 0
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        mDate = c.time

        //선택된 날짜에서 15일 전
        lastDayOfSupport!!.time = mDate
        lastDayOfSupport!!.add(Calendar.DATE, -30)
    }


    override fun onTimeChanged(view: TimePicker, hourOfDay: Int, minute: Int) {
        Util.log(hourOfDay.toString() + ":" + minute * 10)
        mDate!!.hours = hourOfDay
        mDate!!.minutes = minute * 10
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startDate = Date()
        val c = Calendar.getInstance(Const.TIME_ZONE_KST)
        c.time = startDate
        // 달력에서 날짜 안찍고 바로 확인 누르는경우 대비하여 한국시간 0시로 설정
        c[Calendar.HOUR_OF_DAY] = 0
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        startDate = c.time

        //date null 값 아니면,  받아온 값(이전의 값)으로
        //캘린더 뷰  set 해준다.
        if (mDate != null) {
            binding.date.setDate(mDate!!.time)
        }


        //limitDate가 없다는것은 달력을 처음 켰다는거니까 오늘 날짜에서 +1을해준다.
        if (limitDate == null) {
            lastDayOfSupport = Calendar.getInstance()
            lastDayOfSupport?.add(Calendar.DATE, 1)
        } else { //limitDate가 있으면 이전에 선택한 날짜가 있으므로 다시 넣어준다.
            val calendar = Calendar.getInstance()
            calendar.time = limitDate
            lastDayOfSupport = calendar
        }


        //시작일로부터 15일 -> 31일
        val calDday = Calendar.getInstance()
        calDday.time = startDate
        calDday.add(Calendar.DATE, 31)
        // 로컬 타임존 0시로 설정되므로 KST0시로 변환 ex) GMT 0시 -> GMT 전날 15시
        val localTz = TimeZone.getDefault()
        // KST와 시간차이 구하고
        val diff = (Const.TIME_ZONE_KST.rawOffset - localTz.rawOffset).toLong()
        // 로컬 타임존을 KST에 맞춰서 보여주기 위해 (CalendarView는 기본 타임존을 항상 사용해서 적절히 가감해줌)
        binding.date.setMinDate(calDday.timeInMillis + diff)
        if (mDate == null) {
            mDate = calDday.time
        }
        //시작일로부터 3달
        maxPickDay = Calendar.getInstance()
        maxPickDay?.let {
            it.time = startDate
            it.add(Calendar.DATE, 100)
            binding.date.setMaxDate(it.getTimeInMillis() + diff)
        }

        binding.date.setOnDateChangeListener(this)

        val confirmBtn = view.findViewById<AppCompatButton>(R.id.btn_confirm)
        val cancelBtn = view.findViewById<AppCompatButton>(R.id.btn_cancel)

        confirmBtn.setOnClickListener { v: View? ->
            Util.log("SupportWrite::realTime $mDate")
            val result = Intent()
            result.putExtra("date", mDate)
            result.putExtra("limitedDate", lastDayOfSupport!!.time)
            setResult(result)
            setResultCode(RESULT_OK)
            startDate = null
            dismiss()
        }

        cancelBtn.setOnClickListener { v: View? ->
            setResultCode(RESULT_CANCEL)
            dismiss()
        }
    }

    companion object {
        private const val TIME_PICKER_INTERVAL = 10
    }
}
