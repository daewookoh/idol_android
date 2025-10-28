package net.ib.mn.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.NewCommentActivity.Companion.createIntent
import net.ib.mn.activity.ScheduleDetailActivity
import net.ib.mn.activity.ScheduleWriteActivity
import net.ib.mn.adapter.DayAdapter
import net.ib.mn.adapter.ScheduleAdapter
import net.ib.mn.adapter.ScheduleAdapter.ScheduleDetailClickListener
import net.ib.mn.adapter.ScheduleAdapter.ScheduleRemoveClickListener
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.ScheduleRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ScheduleViewBinding
import net.ib.mn.fragment.BottomSheetFragment.Companion.newInstance
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.schedule.IdolSchedule.Companion.getInstance
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.livedata.Event
import net.ib.mn.viewmodel.CommunityActivityViewModel
import org.json.JSONObject
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleFragment : BaseFragment(), DayAdapter.OnDayClickListener,
    ScheduleAdapter.ScheduleEditClickListener, ScheduleRemoveClickListener,
    ScheduleAdapter.ScheduleCommentClickListener, ScheduleDetailClickListener,
    ScheduleAdapter.ScheduleVoteClickListener, View.OnClickListener {
    private var dayAdapter: DayAdapter? = null
    private var scheduleAdapter: ScheduleAdapter? = null
    private var dayList: ArrayList<String> = ArrayList()
    private var dayIconList: HashMap<Int, String> = HashMap()
    private var mCal: Calendar = Calendar.getInstance()
    private var mCalTmp: Calendar = Calendar.getInstance()
    private var mIdol: IdolModel? = null
    var dayNum: Int = 0
    private var schedule: ArrayList<ScheduleModel> = ArrayList()
    private var scheduleIcon: ArrayList<ScheduleModel> = ArrayList()
    private var actionbar: ActionBar? = null
    private var flag: Boolean? = null
    var manager: FragmentManager? = null
    var trans: FragmentTransaction? = null
    private var nowDate = 0
    private var nowMonth = 0
    private var nowYear = 0
    private var mAccount: IdolAccount? = null
    private var most = false
    private var isAdmin = false
    private var levelAdd = false
    private var levelVote = false
    private var mIds: HashMap<Int, String>? = HashMap()

    private var communityActivityViewModel: CommunityActivityViewModel? = null
    private var mContext: Context? = null

    @Inject
    lateinit var scheduleRepository: ScheduleRepositoryImpl
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var miscRepository: MiscRepository

    private var _binding: ScheduleViewBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = context
        flag = false
        isAdmin = false
        most = false
        levelAdd = false
        levelVote = false
        loadOnce = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ScheduleViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mIdol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().intent.extras!!
                .getSerializable(CommunityActivity.PARAM_IDOL, IdolModel::class.java)
        } else {
            requireActivity().intent.extras!!.getSerializable(CommunityActivity.PARAM_IDOL) as IdolModel?
        }
        mAccount = getAccount(requireContext())

        binding.tvTextLanguage.text = getDefaultLanguageText(mContext)
        getInstance().scheduleLocaleString = getDefaultLanguage(mContext)

        val appLocale = getAppLocale(mContext!!)
        val namesOfDays = DateFormatSymbols.getInstance(appLocale).shortWeekdays
        binding.sunday.text = namesOfDays[Calendar.SUNDAY]
        binding.monday.text = namesOfDays[Calendar.MONDAY]
        binding.tuesday.text = namesOfDays[Calendar.TUESDAY]
        binding.wednesday.text = namesOfDays[Calendar.WEDNESDAY]
        binding.thursday.text = namesOfDays[Calendar.THURSDAY]
        binding.friday.text = namesOfDays[Calendar.FRIDAY]
        binding.saturday.text = namesOfDays[Calendar.SATURDAY]

        manager = requireActivity().supportFragmentManager
        trans = manager!!.beginTransaction()

        nowDate = mCal.get(Calendar.DATE)
        nowMonth = mCal.get(Calendar.MONTH)
        nowYear = mCal.get(Calendar.YEAR)

        setCalendarDate(mCal.get(Calendar.MONTH))
        //        mCal.set(Calendar.MONTH, mCal.get(Calendar.MONTH)-1);
        if (mAccount != null
            && mAccount!!.heart == Const.LEVEL_MANAGER
        ) isAdmin = true
        if (BuildConfig.CELEB) {
            // CELEB
            if (mAccount!!.most != null
                && mAccount!!.most!!.getId() == mIdol!!.getId()
            ) most = true
        } else {
            if (mAccount!!.most != null
                && mAccount!!.most!!.groupId == mIdol!!.groupId
            ) most = true
        }

        if (mAccount!!.most != null
            && mAccount!!.level >= ConfigModel.getInstance(mContext).scheduleAddLevel
        ) levelAdd = true
        if (mAccount!!.most != null
            && mAccount!!.level >= ConfigModel.getInstance(mContext).scheduleVoteLevel
        ) levelVote = true
        dayAdapter = DayAdapter(
            mContext!!, dayList, dayIconList, mCal.get(Calendar.YEAR), mCal.get(
                Calendar.MONTH
            ), mCal.get(Calendar.DATE), this
        )

        //폰트크기가 크면 어르신 전용뷰로 , 아니면 원래뷰로 설정해준다.
        val resId: Int

        //시스템 font scale  받아옴.
        val scale = mContext!!.resources.configuration.fontScale

        resId = if (scale >= 1.5f) {
            R.layout.schedule_older_item
        } else {
            R.layout.schedule_item
        }

        scheduleAdapter = ScheduleAdapter(
            mContext!!,
            resId,
            mAccount!!.userModel!!.id,
            most,
            levelVote, this, this, this, this, this
        )
        binding.dayView.setAdapter(dayAdapter)
        binding.scheduleView.setAdapter(scheduleAdapter)
        binding.dayView.isExpanded = true
        binding.scheduleView.isExpanded = true

        actionbar = (activity as AppCompatActivity?)!!.supportActionBar

        binding.liLanguageWrapper.setOnClickListener(this)

        binding.ivPrev.setOnClickListener(View.OnClickListener { v: View? ->
            if (flag!!) return@OnClickListener
            Util.showProgress(mContext)
            flag = true
            dayList.clear()
            Util.log("prev")
            setCalendarDate(mCal.get(Calendar.MONTH) - 1)
            dayAdapter!!.setNowDay(mCal.get(Calendar.DATE))
            loadList()
            loadIconList()
        })
        binding.ivNext.setOnClickListener(View.OnClickListener { v: View? ->
            if (flag!!) return@OnClickListener
            Util.showProgress(mContext)
            flag = true
            dayList.clear()
            Util.log("next")
            setCalendarDate(mCal.get(Calendar.MONTH) + 1)
            dayAdapter!!.setNowDay(mCal.get(Calendar.DATE))
            loadList()
            loadIconList()
        })

        dataFromVM
    }

    override fun onResume() {
        super.onResume()
        if (mIds!!.isEmpty()) loadIdols(if (BuildConfig.CELEB) mIdol!!.getGroup_id() else mIdol!!.groupId)
        if (!loadOnce) {
            loadOnce = true
            loadList()
        }
        loadIconList()
        schedule = getInstance().schedules
        dayIconList.clear()
        scheduleAdapter!!.clear()
        for (sch in schedule) {
            val day = sch.dtstart.date
            //            if (dayIconList.get(day) == null) dayIconList.put(day, sch.getCategory());
            if (day == mCal[Calendar.DATE]) {
                scheduleAdapter!!.add(sch)
            }
        }
        if (scheduleAdapter!!.count > 0) {
            binding.scheduleView.visibility = View.VISIBLE
            binding.empty.visibility = View.GONE
        } else {
            binding.scheduleView.visibility = View.GONE
            binding.empty.visibility = View.VISIBLE
        }
        //        dayAdapter.notifyDataSetChanged();
        scheduleAdapter!!.notifyDataSetChanged()
    }

    override fun onClick(position: Int) {
        scheduleAdapter!!.clear()
        //        for (int i = 0; i < schedule.size(); i++) {
//            ScheduleModel item = schedule.get(i);
//            int day = item.getDtstart().getDate();
//            if (day == position) {
//                scheduleAdapter.add(item);
//            }
//        }
        mCal[Calendar.DATE] = position
        dayAdapter!!.setNowDay(mCal[Calendar.DATE])
        //        if (scheduleAdapter.getCount() > 0) {
//            binding.scheduleView.setVisibility(View.VISIBLE);
//            binding.empty.setVisibility(View.GONE);
//        } else {
//            binding.scheduleView.setVisibility(View.GONE);
//            binding.empty.setVisibility(View.VISIBLE);
//        }
//        scheduleAdapter.notifyDataSetChanged();
        loadList()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.li_language_wrapper -> showLanguageDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            WRITE_REQUEST_CODE -> {}
            COMMENT_REQUEST_CODE -> if (getInstance().lastSchedule != null) {
                val id = getInstance().lastSchedule!!.id
                val comment = getInstance().lastSchedule!!.num_comments
                var i = 0
                while (i < scheduleAdapter!!.count) {
                    if (scheduleAdapter!!.getItem(i).id == id) scheduleAdapter!!.getItem(i).num_comments =
                        comment
                    i++
                }
                scheduleAdapter!!.notifyDataSetChanged()
                getInstance().lastSchedule = null
            }
        }
    }

    override fun onClick(v: View, item: ScheduleModel) {
        when (v.id) {
            R.id.schedule_detail -> startActivity(
                ScheduleDetailActivity.createIntent(
                    mContext,
                    mIdol,
                    mIds,
                    mCal.time,
                    item.id,
                    most,
                    levelAdd,
                    levelVote
                )
            )

            R.id.btn_edit -> {
                val i = ScheduleWriteActivity.createIntent(
                    mContext,
                    if (BuildConfig.CELEB) mIdol else null,
                    item,
                    true
                )

                // 수정 후 갱신되게...
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_schedule_edit"
                )
                startActivityForResult(i, ScheduleDetailActivity.EDIT_REQUEST_CODE)
            }
        }
    }

    override fun onClick(v: View, item: ScheduleModel, pos: Int) {
        when (v.id) {
            R.id.comment -> lifecycleScope.launch {
                miscRepository.getResource(
                    "articles/" + item.article_id + "/",
                    { response ->
                        if (activity != null && isAdded) {
                            val article =
                                instance.fromJson(response.toString(), ArticleModel::class.java)
                            val intent = createIntent(mContext, article, -1, item, true, mIds, false)
                            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            startActivityForResult(intent, COMMENT_REQUEST_CODE)
                        }
                    },
                    { _ ->
                        if (activity != null && isAdded) {
                            makeText(
                                mContext, R.string.error_abnormal_default,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    private fun setCalendarDate(month: Int) {
        mCal[Calendar.DATE] = 1
        mCal[Calendar.MONTH] = month
        if (nowMonth == month && nowYear == mCal[Calendar.YEAR]) mCal[Calendar.DATE] = nowDate
        mCalTmp[mCal[Calendar.YEAR], mCal[Calendar.MONTH]] = 1
        dayNum = mCalTmp[Calendar.DAY_OF_WEEK]

        for (i in 1 until dayNum) {
            dayList.add("")
        }
        for (i in 0 until mCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            dayList.add("" + (i + 1))
        }
    }

    //아이콘 리스트 ,스케쥴 리스트 분리.
    private fun loadIconList() {
        val listener: (JSONObject) -> Unit = { response ->
            scheduleIcon.clear()
            dayIconList.clear()

            //                scheduleAdapter.clear();
            val gson = getInstance(false)
            val listType = object : TypeToken<List<ScheduleModel?>?>() {
            }.type
            if (response.optBoolean("success")) {
                val idols = gson.fromJson<List<ScheduleModel>>(
                    response.optJSONArray("objects").toString(), listType
                )
                for (idol in idols) {
                    val month = idol.dtstart.month
                    if (month != mCal[Calendar.MONTH]) continue
                    //                        if (idol.getAllday() == 1) {
//                            idol.setDtstart(Util.dateToUTC(idol.getDtstart()));
//                        }
                    scheduleIcon.add(idol)
                }


                //                    schedule = IdolSchedule.getInstance().sort(schedule);
                for (idol in scheduleIcon) {
                    val day = idol.dtstart.date
                    if (dayIconList[day] == null) dayIconList[day] = idol.category
                    //                        if (day == mCal.get(Calendar.DATE)) {
//                            scheduleAdapter.add(idol);
//                        }
                }


                //                    IdolSchedule.getInstance().setSchedules(schedule);
            }
            //                if (scheduleAdapter.getCount() > 0) {
//                    binding.scheduleView.setVisibility(View.VISIBLE);
//                    binding.empty.setVisibility(View.GONE);
//                } else {
//                    binding.scheduleView.setVisibility(View.GONE);
//                    binding.empty.setVisibility(View.VISIBLE);
//                }
            dayAdapter!!.setYear(mCal[Calendar.YEAR])
            dayAdapter!!.setMonth(mCal[Calendar.MONTH])
            dayAdapter!!.nextMonthInit()
            dayAdapter!!.notifyDataSetChanged()
            //                scheduleAdapter.notifyDataSetChanged();
            binding.scrollView.requestChildFocus(null, binding.dayView)
            Handler().postDelayed({
                flag = false
                Util.closeProgress()
            }, 100)
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            if (activity != null && isAdded) {
                var errorText = getString(R.string.error_abnormal_default)
                if (!TextUtils.isEmpty(msg)) {
                    errorText += msg
                }
                makeText(mContext, errorText, Toast.LENGTH_SHORT).show()
            }

            Handler().postDelayed({
                flag = false
                Util.closeProgress()
            }, 100)
        }
        MainScope().launch {
            scheduleRepository.getSchedules(
                idolId = if (BuildConfig.CELEB) mIdol!!.getId() else mIdol!!.groupId,
                yearmonth = mCal[Calendar.YEAR].toString() + String.format(
                    Locale.US,
                    "%02d",
                    (mCal[Calendar.MONTH] + 1)
                ),
                vote = 1,
                locale = getInstance().scheduleLocaleString,
                onlyIcon = "Y",
                listener = listener,
                errorListener = errorListener
            )
        }
    }

    private fun loadList() {
        val sdf = SimpleDateFormat(
            "MMM", getAppLocale(
                mContext!!
            )
        )
        binding.tvYear.text = mCal[Calendar.YEAR].toString()
        binding.tvMonth.text = sdf.format(mCal.time)
        val listener: (JSONObject) -> Unit = { response ->
            schedule.clear()
            //                dayIconList.clear();
            scheduleAdapter!!.clear()
            val gson = getInstance(false)
            val listType = object : TypeToken<List<ScheduleModel?>?>() {
            }.type
            if (response.optBoolean("success")) {
                val idols = gson.fromJson<List<ScheduleModel>>(
                    response.optJSONArray("objects").toString(), listType
                )
                Util.log("idols size ->" + idols.size)
                for (idol in idols) {
                    val month = idol.dtstart.month
                    if (month != mCal[Calendar.MONTH]) continue
                    //                        if (idol.getAllday() == 1) {
//                            idol.setDtstart(Util.dateToUTC(idol.getDtstart()));
//                        }
                    schedule.add(idol)
                }
                schedule = getInstance().sort(schedule)
                for (idol in schedule) {
                    val day = idol.dtstart.date
                    //                        if (dayIconList.get(day) == null) dayIconList.put(day, idol.getCategory());
                    if (day == mCal[Calendar.DATE]) {
                        scheduleAdapter!!.add(idol)
                    }
                }
                getInstance().schedules = schedule
            }
            if (scheduleAdapter!!.count > 0) {
                binding.scheduleView.visibility = View.VISIBLE
                binding.empty.visibility = View.GONE
            } else {
                binding.scheduleView.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE
            }
            //                dayAdapter.setYear(mCal.get(Calendar.YEAR));
//                dayAdapter.setMonth(mCal.get(Calendar.MONTH));
//                dayAdapter.nextMonthInit();
//                dayAdapter.notifyDataSetChanged();
            scheduleAdapter!!.notifyDataSetChanged()
            binding.scrollView.requestChildFocus(null, binding.dayView)
            Handler().postDelayed({
                flag = false
                Util.closeProgress()
            }, 100)        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            if (activity != null && isAdded) {
                var errorText = getString(R.string.error_abnormal_default)
                if (!TextUtils.isEmpty(msg)) {
                    errorText += msg
                }
                makeText(mContext, errorText, Toast.LENGTH_SHORT).show()
            }

            Handler().postDelayed({
                flag = false
                Util.closeProgress()
            }, 100)
        }

        MainScope().launch {
            scheduleRepository.getSchedules(
                idolId = if (BuildConfig.CELEB) mIdol!!.getId() else mIdol!!.groupId,
                yearmonthday = mCal[Calendar.YEAR].toString() + String.format(
                    Locale.US,
                    "%02d",
                    (mCal[Calendar.MONTH] + 1)
                ) + String.format(Locale.US, "%02d", (mCal[Calendar.DAY_OF_MONTH])),
                vote = 1,
                locale = getInstance().scheduleLocaleString,
                listener = listener,
                errorListener = errorListener
            )
        }
    }

    override fun onClick(view: View, id: Int, position: Int) {
        when (view.id) {
            R.id.btn_remove -> {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_schedule_delete"
                )
                Util.showDefaultIdolDialogWithBtn2(
                    mContext,
                    mContext!!.getString(R.string.title_remove),
                    mContext!!.getString(R.string.confirm_delete),
                    { v: View? ->
                        MainScope().launch {
                            scheduleRepository.delete(
                                id,
                                { response ->
                                    if (response.optBoolean("success")) {
                                        loadIconList()
                                        loadList()
                                    } else {
                                        UtilK.handleCommonError(mContext, response)
                                    }
                                }, { throwable ->
                                    makeText(
                                        mContext,
                                        R.string.error_abnormal_exception,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        Util.closeIdolDialog()
                    },
                    { v: View? -> Util.closeIdolDialog() })
            }
        }
    }

    override fun onClick(id: Int, vote: String, view: View, position: Int) {
        vote(id, vote, view, position)
    }

    fun vote(id: Int, vote: String?, view: View, itemPosition: Int) {
        val scheduleItem = schedule[itemPosition]
        val listener: (JSONObject) -> Unit = { response ->
            if (!response.optBoolean("success")) {
                val responseMsg = ErrorControl.parseError(mContext, response)
                if (responseMsg != null) {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        responseMsg,
                        { v1: View? -> Util.closeIdolDialog() },
                        true
                    )
                }
            } else { //vote 가  success 일때


                v("스케쥴 vote 얍얍얍 ->$response")

                val tvReport1 = view.findViewById<View>(R.id.tv_report1) as TextView
                val ivReport1 = view.findViewById<View>(R.id.iv_report1) as ImageView

                val tvReport2 = view.findViewById<View>(R.id.tv_report2) as TextView
                val ivReport2 = view.findViewById<View>(R.id.iv_report2) as ImageView

                val tvReport3 = view.findViewById<View>(R.id.tv_report3) as TextView
                val ivReport3 = view.findViewById<View>(R.id.iv_report3) as ImageView


                if (schedule[itemPosition].vote.isNullOrEmpty()) {
                    when (vote) {
                        "Y" -> {
                            ivReport1.apply {
                                setImageResource(R.drawable.btn_schedule_yes_on)
                                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                                imageTintMode = PorterDuff.Mode.SRC_IN
                            }
                            tvReport1.setTextColor(
                                ContextCompat.getColor(
                                    mContext!!, R.color.main
                                )
                            )
                            scheduleItem.vote = vote
                            scheduleItem.num_yes = scheduleItem.num_yes + 1
                            tvReport1.text = scheduleItem.num_yes.toString()
                        }

                        "N" -> {
                            ivReport2.apply {
                                setImageResource(R.drawable.btn_schedule_no_on)
                                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                                imageTintMode = PorterDuff.Mode.SRC_IN
                            }
                            tvReport2.setTextColor(
                                ContextCompat.getColor(
                                    mContext!!, R.color.main
                                )
                            )
                            scheduleItem.vote = vote
                            scheduleItem.num_no = scheduleItem.num_no + 1
                            tvReport2.text = scheduleItem.num_no.toString()
                        }

                        "D" -> {
                            ivReport3.setImageResource(R.drawable.btn_schedule_overlap_on)
                            tvReport3.setTextColor(
                                ContextCompat.getColor(
                                    mContext!!, R.color.main
                                )
                            )
                            scheduleItem.vote = "D"
                        }
                    }
                }
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(msg)) {
                errorText += msg
            }
            makeText(
                mContext, errorText,
                Toast.LENGTH_SHORT
            ).show()
        }

        MainScope().launch {
            scheduleRepository.vote(id, vote, listener, errorListener)
        }
    }

    private fun loadIdols(id: Int) {
        val listener: ((JSONObject) -> Unit) = { response ->
            try {
                val gson = getInstance(true)
                val listType = object : TypeToken<List<IdolModel?>?>() {
                }.type
                val idols = gson.fromJson<List<IdolModel>>(
                    response.optJSONArray("objects")?.toString() ?: "",
                    listType
                )
                for (idol in idols) {
//                        idol.setLocalizedName(mContext);
                    if (BuildConfig.CELEB) {
                        mIds!![idol.getId()] = idol.getName(mContext)
                    } else {
                        if (idol.type.equals("S", ignoreCase = true)) mIds!![idol.getId()] =
                            Util.nameSplit(
                                mContext, idol
                            )[0]
                        else mIds!![idol.getId()] = idol.getName(mContext)
                    }
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }

        val errorListener: ((Throwable) -> Unit) = { throwable ->
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(throwable.message)) {
                errorText += throwable.message
            }
            makeText(
                mContext, errorText,
                Toast.LENGTH_SHORT
            ).show()
        }

        lifecycleScope.launch {
            idolsRepository.getGroupMembers(
                id,
                listener,
                errorListener
            )
        }
    }

    private fun showLanguageDialog() {
        val sheet = newInstance(
            BottomSheetFragment.FLAG_LANGUAGE_SETTING,
            BottomSheetFragment.FLAG_SCHEDULE_LANGUAGE_SETTING
        )
        val tag = "filter"
        val oldFrag = parentFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            sheet.show(parentFragmentManager, tag)
        }
    }

    fun setLanguage(language: String?, locale: String?) {
        setUiActionFirebaseGoogleAnalyticsFragment(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "schedule_language_change"
        )

        binding.tvTextLanguage.text = language
        getInstance().scheduleLocaleString = locale!!
        loadIconList()
        loadList()
    }

    private val dataFromVM: Unit
        get() {
            communityActivityViewModel = ViewModelProvider(requireActivity()).get(
                CommunityActivityViewModel::class.java
            )

            communityActivityViewModel!!.scheduleWrite.observe(viewLifecycleOwner) { item: Event<Boolean?>? ->
                try {
                    val ac = getAccount(requireContext())
                    if (BuildConfig.CELEB) {
                        if (ac!!.most != null && ac.most!!.getId() == mIdol!!.getId()) most = true
                    } else {
                        if (ac!!.most != null && ac.most!!.groupId == mIdol!!.groupId) most = true
                    }
                    if (ac.most != null && ac.level >= ConfigModel.getInstance(mContext).scheduleAddLevel) levelAdd =
                        true
                    if (ac.most != null && ac.level >= ConfigModel.getInstance(mContext).scheduleVoteLevel) levelVote =
                        true
                } catch (e: NullPointerException) {
                    e.stackTrace
                }
                if (isAdmin) {
                    startActivityForResult(
                        ScheduleWriteActivity.createIntent(
                            mContext,
                            mIdol,
                            mCal.time
                        ), WRITE_REQUEST_CODE
                    )
                } else if (!most) {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        getString(if (BuildConfig.CELEB) R.string.schedule_write_most_actor else R.string.schedule_write_most),
                        { v1: View? -> Util.closeIdolDialog() }, true
                    )
                } else if (!levelAdd) {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        String.format(
                            getString(R.string.schedule_write_level),
                            ConfigModel.getInstance(mContext).scheduleAddLevel.toString()
                        ),
                        { v12: View? -> Util.closeIdolDialog() }, true
                    )
                } else startActivityForResult(
                    ScheduleWriteActivity.createIntent(
                        mContext,
                        mIdol,
                        mCal.time
                    ), WRITE_REQUEST_CODE
                )
            }
        }

    companion object {
        private const val WRITE_REQUEST_CODE = 1000
        const val COMMENT_REQUEST_CODE: Int = 2000
        const val EDIT_REQUEST_CODE: Int = 4000
        val scheduleLanguages: Array<String> = arrayOf(
            "한국어",
            "ENG",
            "中文",
            "日本語",
            "Indonesia",
            "Português",
            "Español",
            "Tiếng Việt",
            "ไทย"
        )
        val scheduleLocales: Array<String> =
            arrayOf("ko", "en", "zh", "ja", "in", "pt", "es", "vi", "th")

        @JvmField
        var loadOnce: Boolean = false

        fun getDefaultLanguage(context: Context?): String {
            for (locale in scheduleLocales) {
                if (locale.startsWith(
                        Util.getSystemLanguage(context).split("_".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0])
                ) return locale
            }
            return scheduleLocales[1]
        }

        fun getDefaultLanguageText(context: Context?): String {
            var i = 0
            for (locale in scheduleLocales) {
                if (locale.startsWith(
                        Util.getSystemLanguage(context).split("_".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0])
                ) return scheduleLanguages[i]
                i++
            }

            return scheduleLanguages[1]
        }
    }
}
