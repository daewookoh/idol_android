package net.ib.mn.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.core.data.repository.ScheduleRepositoryImpl
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.ActivityScheduleWriteBinding
import net.ib.mn.dialog.BaseDialogFragment.DialogResultHandler
import net.ib.mn.dialog.ScheduleDateTimePickerDialogFragment
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.model.toPresentation
import net.ib.mn.schedule.IdolSchedule.Companion.getInstance
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleWriteActivity : BaseActivity(), View.OnClickListener, DialogResultHandler {
    private var mIdol: IdolModel? = null
    private var category: String? = null
    private var mIdols: ArrayList<IdolModel> = ArrayList()
    private var allday = 0
    private var date: Date? = null
    private var address: String? = null
    private var latitude: String? = null
    private var longitude: String? = null
    private var edit = false
    private var scheduleId = 0
    private var isSelectedIdol = false

    private var nowYear = 0
    private var nowMonth = 0

    @Inject
    lateinit var scheduleRepository: ScheduleRepositoryImpl

    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase

    private lateinit var binding: ActivityScheduleWriteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScheduleWriteBinding.inflate(layoutInflater)
        binding.svContainer.applySystemBarInsets()
        setContentView(binding.root)

        edit = intent.getBooleanExtra(EXTRA_EDIT, false)

        binding.category.setOnClickListener(this)
        binding.allday.setOnClickListener(this)
        binding.startday.setOnClickListener(this)
        binding.locationWrapper.setOnClickListener(this)
        binding.btnCancelLocation.setOnClickListener(this)
        binding.btnWrite.setOnClickListener(this)

        if (BuildConfig.CHINA) {
            binding.locationWrapper.setVisibility(View.GONE)
        }

        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.setTitle(getString(R.string.schedule_write))
            actionbar.setDisplayHomeAsUpEnabled(true)
            actionbar.setHomeButtonEnabled(false)
        }

        if (edit) {
            val schedule = intent.getSerializableExtra(EXTRA_SCHEDULE) as ScheduleModel
            binding.title.setText(schedule.title)
            category = schedule.category
            binding.categoryIv.setImageResource(Util.getScheduleIcon(category))
            binding.categoryTv.setText(getString(R.string.schedule_category))
            binding.idolLabel.setText(getString(R.string.stats_idol))

            if (BuildConfig.CELEB) {
                binding.idol.setVisibility(View.GONE)
            }
            if (BuildConfig.CELEB || schedule.idol?.type == "S") {
                binding.idolTv.setText(schedule.idol?.getName(this))
                binding.idolTv.setPadding(0, 0, Util.convertDpToPixel(this, 20f).toInt(), 0)
                binding.idolArrow.setVisibility(View.GONE)
                if (!BuildConfig.CELEB) {
                    mIdols.add(schedule.idol ?: return)
                }
            } else {
                binding.idol.setOnClickListener(this)
                val idolIds = schedule.idol_ids
                val idolNames = ArrayList<String>()
                lifecycleScope.launch(Dispatchers.IO) {
                    val idols = getIdolsByIdsUseCase(idolIds)
                        .mapListDataResource { it.toPresentation() }
                        .awaitOrThrow()

                    idols?.let {
                        for (idol in idols) {
                            mIdols.add(idol)
                            idolNames.add(Util.nameSplit(this@ScheduleWriteActivity, idol)[0])
                        }
                        withContext(Dispatchers.Main) {
                            binding.idolTv.text = idolNames.sorted().joinToString(", ")
                        }
                    }
                }
            }

            nowYear = schedule.dtstart.year
            nowMonth = schedule.dtstart.month
            date = schedule.dtstart
            binding.startdayLabel.setText(getString(R.string.schedule_time))
            allday = schedule.allday
            binding.startdayTv.setText(getDateTimeString(date, allday == 1))
            binding.alldayCheck.setChecked(allday == 1)
            if (binding.alldayCheck.isChecked() == true) binding.alldayTv.setText(getString(R.string.schedule_allday))
            address = schedule.location
            longitude = schedule.lng
            latitude = schedule.lat
            binding.location.setText(address)
            binding.locationLabel.setText(getString(R.string.schedule_location))
            if (binding.location.getText()
                    .toString() != ""
            ) binding.btnCancelLocation.setVisibility(
                View.VISIBLE
            )
            binding.url.setText(schedule.url)
            binding.info.setText(schedule.extra)
            scheduleId = schedule.id
            if (BuildConfig.CELEB) {
                mIdol = intent.getSerializableExtra("idol") as IdolModel?
                mIdol?.let {
                    mIdols.add(it)
                }

            } else {
                val idol = schedule.idol ?: return
                mIdol = IdolModel(idol.getId(), idol.groupId)
            }
        } else {
            mIdol = intent.getSerializableExtra(CommunityActivity.PARAM_IDOL) as IdolModel? ?: return

            //            mIdol.setLocalizedName(this);
            date = intent.getSerializableExtra("date") as Date?
            nowYear = date!!.year
            nowMonth = date!!.month
            date!!.minutes = 0
            date!!.seconds = 0
            binding.startdayTv.setText(getDateTimeString(date, allday == 1))

            // 셀럽에서는 셀럽 선택이 없어서 scheduleIdolTv에 설정되는 값은 별 의미 없음
            if (mIdol!!.type == "S" && mIdol!!.groupId == mIdol!!.getId() || BuildConfig.CELEB) {
                binding.idolTv.setText(mIdol!!.getName(this))
                binding.idolTv.setPadding(0, 0, Util.convertDpToPixel(this, 20f).toInt(), 0)
                binding.idolArrow.setVisibility(View.GONE)
                mIdols.add(mIdol!!)
            } else {
                binding.idol.setOnClickListener(this)
                mIdol!!.resourceUri = "/" + mIdol!!.groupId
                mIdol!!.type = "G"
                mIdols.add(mIdol!!)
            }
            address = ""
            latitude = ""
            longitude = ""
        }
    }

    override fun onResume() {
        super.onResume()
        if (binding.idolTv.text.toString() != "") if (BuildConfig.CELEB) {
            binding.idolLabel.text = getString(R.string.actor)
        } else {
            binding.idolLabel.text = getString(R.string.stats_idol)
        }
        if (binding.alldayCheck.isChecked) binding.alldayTv.text =
            getString(R.string.schedule_allday)
        if (binding.startdayTv.text.toString() != "") binding.startdayLabel.text =
            getString(R.string.schedule_time)
        if (binding.location.text.toString() != "") binding.locationLabel.text =
            getString(R.string.schedule_location)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.category -> startActivityForResult(
                ScheduleWriteCategoryActivity.createIntent(
                    this
                ), CATEGORY_REQUEST_CODE
            )

            R.id.idol -> {
                // 셀럽은 셀럽 선택이 없어서 아래 코드는 실행안됨
                //아이돌이 아무도 선택이 안되어있는 경우 true , 아이돌 선택이 하나라도 되어있는 경우 false
                val isNoIdolSelected = binding.idolTv.text.toString().isEmpty()
                val isGroupSelected =
                    !isNoIdolSelected && mIdols[0]!!.type.equals("G", ignoreCase = true)
                startActivityForResult(
                    ScheduleWriteIdolActivity.createIntent(
                        this,
                        mIdols,
                        isGroupSelected,
                        isNoIdolSelected
                    ), IDOL_REQUEST_CODE
                )
            }

            R.id.allday -> {
                if (binding.alldayCheck.isChecked) {
                    binding.alldayTv.setText(null)
                    allday = 0
                } else {
                    binding.alldayTv.text = getString(R.string.schedule_allday)
                    allday = 1
                    date!!.hours = 0
                    date!!.minutes = 0
                    date!!.seconds = 0
                    binding.startdayLabel.text = getString(R.string.schedule_time)
                }
                binding.startdayTv.text = getDateTimeString(date, allday == 1)

                binding.alldayCheck.isChecked = !binding.alldayCheck.isChecked
            }

            R.id.startday -> {
                val dateDlg = ScheduleDateTimePickerDialogFragment.getInstance(date, allday)
                dateDlg.setActivityRequestCode(TIME_REQUEST_CODE)
                dateDlg.show(supportFragmentManager, "date")
            }

            R.id.location_wrapper, R.id.btn_cancel_location -> if (binding.location.text.toString() == "") {
                if (latitude == null
                    || (latitude!!.isEmpty() || longitude!!.isEmpty())
                ) {
                    startActivityForResult(
                        ScheduleWriteLocationActivity.createIntent(this),
                        LOCATION_REQUEST_CODE
                    )
                } else {
                    startActivityForResult(
                        ScheduleWriteLocationActivity.createIntent(
                            this,
                            latitude!!,
                            longitude!!
                        ), LOCATION_REQUEST_CODE
                    )
                }
            } else {
                address = ""
                latitude = ""
                longitude = ""
                binding.locationLabel.text = ""
                binding.location.text = address
                binding.btnCancelLocation.visibility = View.GONE
            }

            R.id.btn_write -> {
                binding.btnWrite.isEnabled = false
                val view = this.currentFocus
                if (view != null) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
                try {
                    Thread.sleep(100) // 잠깐 딜레이... post로 처리하기보단 그냥 딜레이...
                } catch (ignore: InterruptedException) {
                }
                if (binding.title.text.toString().isEmpty()
                    || binding.categoryTv.text.toString().isEmpty()
                    || binding.startdayTv.text.toString().isEmpty()
                    || (!BuildConfig.CELEB && binding.idolTv.text.toString().isEmpty())
                ) {
                    Util.showDefaultIdolDialogWithBtn1(
                        this,
                        null,
                        Util.getColorText(
                            getString(R.string.schedule_require),
                            "*",
                            ContextCompat.getColor(this@ScheduleWriteActivity, R.color.main)
                        ),
                        { v1: View? ->
                            Util.closeIdolDialog()
                            binding.btnWrite.isEnabled = true
                        }, false
                    )
                } else {
                    var dateString: String? = ""
                    if (allday == 0) {
                        //date는 바뀌면 안되므로 새변수 써서 등록.
                        val transDate = Util.dateToUTC(date)
                        dateString =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:00", Locale.ENGLISH).format(
                                transDate
                            )
                    } else {
                        dateString = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date)
                    }

                    var idsString = ""
                    var i = 0
                    while (i < mIdols.size) {
                        if (idsString.isEmpty()) idsString = mIdols[i].getId().toString() + ""
                        else idsString += "," + mIdols[i].getId()
                        i++
                    }
                    val content = if (binding.info.text != null
                    ) Util.BadWordsFilterToHeart(this, binding.info.text.toString())
                    else ""
                    write(
                        if (BuildConfig.CELEB) mIdol!!.getId() else mIdol!!.groupId,
                        idsString,
                        binding.title.text.toString(),
                        category,
                        address,
                        latitude,
                        longitude,
                        binding.url.text.toString(),
                        dateString,
                        60,
                        allday,
                        content
                    )
                }
            }
        }
    }

    fun write(
        idol_id: Int,
        idol_ids: String?,
        title: String?,
        category: String?,
        location: String?,
        lat: String?,
        lng: String?,
        url: String?,
        dtstart: String?,
        duration: Int,
        allday: Int,
        extra: String?
    ) {
        val listener: (JSONObject) -> Unit = { response ->
            Util.closeProgress()
            binding.btnWrite.isEnabled = true

            if (response.optBoolean("success")) {
                val gson = getInstance(false)
                val listType = object : TypeToken<List<ScheduleModel?>?>() {
                }.type
                val idols = gson.fromJson<List<ScheduleModel>>(
                    response.optJSONArray("objects").toString(), listType
                )
                if (edit) {
                    if (nowYear == idols[0].dtstart.year
                        && nowMonth == idols[0].dtstart.month
                    ) {
                        val originSchedule =
                            intent.getSerializableExtra(EXTRA_SCHEDULE) as ScheduleModel?
                        val editedSchedule = idols[0]
                        // 서버에서 vote, num_yes, num_no 필드를 업데이트 안 된 값으로 보내줘서 앱에서 업데이트 함
                        editedSchedule.vote = originSchedule!!.vote
                        if (originSchedule.vote == "Y") {
                            editedSchedule.num_yes = originSchedule.num_yes
                        } else if (originSchedule.vote == "N") {
                            editedSchedule.num_no = originSchedule.num_no
                        }
                        getInstance().setScheduleItem(editedSchedule)
                    } else {
                        getInstance().deleteScheduleItem(idols[0].id)
                    }
                } else {
                    if (nowYear == idols[0].dtstart.year && nowMonth == idols[0].dtstart.month) getInstance().addScheduleItem(
                        idols[0]
                    )
                }
                getInstance().schedules = getInstance().sort(getInstance().schedules)
                Util.showDefaultIdolDialogWithBtn1(
                    this@ScheduleWriteActivity, null, getString(R.string.schedule_save)
                ) { v: View? ->
                    Util.closeIdolDialog()
                    setResult(RESULT_OK, null)
                    finish()
                }
            } else {
                UtilK.handleCommonError(this@ScheduleWriteActivity, response)
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            Util.closeProgress()
            binding.btnWrite.isEnabled = true
            if (!TextUtils.isEmpty(msg)) {
                makeText(this@ScheduleWriteActivity, msg, Toast.LENGTH_SHORT).show()
            } else {
                makeText(this@ScheduleWriteActivity, R.string.msg_error_ok, Toast.LENGTH_SHORT).show()
            }
        }

        val content = if (title != null
        ) Util.BadWordsFilterToHeart(this, title)
        else ""

        MainScope().launch {
            if(edit) {
                scheduleRepository.edit(
                    scheduleId,
                    idol_id,
                    idol_ids,
                    content,
                    category,
                    location,
                    lat,
                    lng,
                    url,
                    dtstart,
                    duration,
                    allday,
                    extra,
                    listener,
                    errorListener
                )
            } else {
                scheduleRepository.write(
                    idol_id,
                    idol_ids,
                    content,
                    category,
                    location,
                    lat,
                    lng,
                    url,
                    dtstart,
                    duration,
                    allday,
                    extra,
                    getInstance().scheduleLocaleString,
                    listener,
                    errorListener
                )
            }
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_CANCELED) return

        if (resultCode == 1) {
            when (requestCode) {
                TIME_REQUEST_CODE -> {
                    binding.startdayLabel.text = getString(R.string.schedule_time)
                    date = data?.getSerializableExtra("date") as Date?
                    if (allday == 1) {
                        date!!.hours = 0
                        date!!.minutes = 0
                        date!!.seconds = 0
                    }
                    binding.startdayTv.text = getDateTimeString(date, allday == 1)

                    Util.log(">>>" + date.toString())
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CATEGORY_REQUEST_CODE -> {
                    category = data!!.getStringExtra("category")
                    if (category.equals("anniversary", ignoreCase = true)) {
                        allday = 1
                        date!!.hours = 0
                        date!!.minutes = 0
                        date!!.seconds = 0
                        binding.startdayLabel.text = getString(R.string.schedule_time)
                        binding.startdayTv.text = getDateTimeString(date, allday == 1)
                        binding.alldayTv.text = getString(R.string.schedule_allday)
                        binding.alldayCheck.isChecked = true
                        binding.allday.setOnClickListener(null)
                    } else binding.allday.setOnClickListener(this)
                    binding.categoryIv.setImageResource(Util.getScheduleIcon(category))
                    binding.categoryTv.text = getString(R.string.schedule_category)
                }

                IDOL_REQUEST_CODE -> {
                    mIdols.clear()
                    mIdols.addAll(data!!.getSerializableExtra("ids") as ArrayList<IdolModel>)
                    isSelectedIdol = data.getBooleanExtra("isSelected", false)
                    var name = ""
                    var i = 0
                    while (i < mIdols.size) {
                        //                        mIds.get(i).setLocalizedName(this);
                        var tmp = ""
                        tmp = if (mIdols[i].type.equals("G", ignoreCase = true)) {
                            mIdols[i].getName(this)
                        } else {
                            Util.nameSplit(this, mIdols[i])[0]
                        }
                        if (name.isEmpty()) {
                            name = tmp
                        } else {
                            name += ",$tmp"
                        }
                        i++
                    }

                    if (isSelectedIdol) binding.idolTv.text = Util.orderByString(name)
                }

                LOCATION_REQUEST_CODE -> {
                    address = data!!.getSerializableExtra("address") as String?
                    longitude = data.getSerializableExtra("longitude").toString()
                    latitude = data.getSerializableExtra("latitude").toString()
                    binding.location.text = address
                    binding.locationLabel.text = getString(R.string.schedule_location)
                    binding.btnCancelLocation.visibility = View.VISIBLE
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // 하루종일이면 년월일만, 아니면 년월일 시분 까지만 문자열로 변환
    private fun getDateTimeString(date: Date?, isAllDay: Boolean): String {
        val f = if (isAllDay) {
            DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                getAppLocale(this)
            )
        } else {
            DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                getAppLocale(this)
            )
        }

        return f.format(date)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        try {
            Thread.sleep(100) // 잠깐 딜레이... post로 처리하기보단 그냥 딜레이...
        } catch (ignore: InterruptedException) {
        }
        Util.showDefaultIdolDialogWithBtn2(
            this, null, getString(R.string.schedule_write_stop),
            {
                Util.closeIdolDialog()
                finish()
            }, { Util.closeIdolDialog() })
    }

    companion object {
        private const val CATEGORY_REQUEST_CODE = 3000
        private const val IDOL_REQUEST_CODE = 4000
        private const val TIME_REQUEST_CODE = 5000
        private const val LOCATION_REQUEST_CODE = 6000

        const val EXTRA_SCHEDULE: String = "schedule"
        const val EXTRA_EDIT: String = "edit"

        fun createIntent(context: Context?, model: IdolModel?, date: Date?): Intent {
            val intent = Intent(context, ScheduleWriteActivity::class.java)
            intent.putExtra(CommunityActivity.PARAM_IDOL, model as Parcelable?)
            intent.putExtra("date", date)
            return intent
        }

        fun createIntent(
            context: Context?,
            idol: IdolModel?,
            model: ScheduleModel?,
            edit: Boolean?
        ): Intent {
            val intent = Intent(context, ScheduleWriteActivity::class.java)
            intent.putExtra(EXTRA_SCHEDULE, model)
            intent.putExtra(EXTRA_EDIT, edit)
            if (idol != null) {
                // 셀럽은 schedule model의 idol에 group_id가 없어서 직접 전달
                intent.putExtra("idol", idol as Parcelable?)
            }
            return intent
        }
    }
}
