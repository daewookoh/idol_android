package net.ib.mn.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseAndroidViewModel
import net.ib.mn.core.data.repository.HeartpickRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.model.NewPicksModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.dialog.HeartPickVoteDialogFragment
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.model.HeartPickVoteRewardModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.Event
import net.ib.mn.utils.trimNewlineWhiteSpace
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HeartPickViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val heartpickRepository: HeartpickRepositoryImpl,
    private val getIdolByIdUseCase: GetIdolByIdUseCase
) : BaseAndroidViewModel(application) {
    @Inject
    lateinit var usersRepository: UsersRepository

    private val _getHeartPickList = MutableLiveData<Event<MutableList<HeartPickModel>>>()
    val getHeartPickList: LiveData<Event<MutableList<HeartPickModel>>> = _getHeartPickList
    private val presentHeartPickList = mutableListOf<HeartPickModel>()

    private val _getHeartPick = MutableLiveData<Event<HeartPickModel>>()
    val getHeartPick: LiveData<Event<HeartPickModel>> = _getHeartPick

    private val _voteCallBack = MutableLiveData<Event<Map<String, Any?>>>()
    val voteCallBack: LiveData<Event<Map<String, Any?>>> = _voteCallBack

    private val _isPrelaunch = MutableLiveData<Event<HeartPickModel>>()
    val isPrelaunch: LiveData<Event<HeartPickModel>> get() = _isPrelaunch

    private val _idol = MutableLiveData<Event<IdolModel>>()
    val idol: LiveData<Event<IdolModel>> get() = _idol

    private val _isLoading = MutableLiveData<Event<Boolean>>()
    val isLoading: LiveData<Event<Boolean>> get() = _isLoading

    var timerJob: Job? = null

    private val _dDayText = MutableLiveData<Event<String>>()
    val dDayText: LiveData<Event<String>> = _dDayText

    private val _isCommented = MutableLiveData<Event<Boolean>>()
    val isCommented: LiveData<Event<Boolean>> = _isCommented

    lateinit var startActivityResultLauncher: ActivityResultLauncher<Intent>

    //툴팁 시간초 지나고 없어졌나 확인.
    private var hasGoneToolTip = false

    private var offset = 0
    private var limit = 10
    // 다음 페이지가 없으면 null로 오게됨.
    private var nextResourceUrl: String? = null
    // 리사이클러뷰 스크롤 위치 바뀜 방지용
    var recyclerviewScrollState: Parcelable? = null

    // HeartPickFragment에서 사용하는 것
    fun registerActivityResult(componentActivity: ComponentActivity, fragment: Fragment) {
        startActivityResultLauncher = fragment.registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult(),
        ) {
            try {
                if(it.resultCode == ResultCode.VOTED.value) {
                    getHeartPick(componentActivity)
                }
                else if(it.resultCode == ResultCode.COMMENTED.value) {
                    val heartPickId = it.data?.getIntExtra("heartPickId", 0)

                    if (heartPickId != null) {
                        getHeartPick(componentActivity, heartPickId)
                    }
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun registerActivityResult(componentActivity: ComponentActivity) {
        startActivityResultLauncher = componentActivity.registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult(),
        ) {
            try {
                if(it.resultCode == ResultCode.COMMENTED.value || it.resultCode == ResultCode.COMMENT_REMOVED.value) {
                    val heartPickId = it.data?.getIntExtra("heartPickId", 0)

                    if(heartPickId != null) {
                        getHeartPick(
                            componentActivity,
                            heartPickId = heartPickId,
                            isRefresh = true
                        )
                        _isCommented.value = Event(true)
                    }
                }

                } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getHeartPick(context: Context?, heartPickId: Int = 0, isShare: Boolean = false, isRefresh : Boolean = false) {
        if (isRefresh) {
            offset = 0
            presentHeartPickList.clear()
        }

        viewModelScope.launch {
            heartpickRepository.get(
                heartPickId,
                offset,
                limit,
                { response ->
                    val gson = IdolGson.getInstance(true)

                    if (heartPickId != 0) {
                        val heartPickModel = gson.fromJson(response?.optJSONObject("object").toString(), HeartPickModel::class.java).apply {
                            hasGoneToolTip = this@HeartPickViewModel.hasGoneToolTip
                        }

                        if (heartPickModel.status == 0) {
                            _isPrelaunch.value = Event(heartPickModel)
                            return@get
                        }

                        _isLoading.value = Event(false)

                        if(heartPickModel.vote != 0) {
                            val firstPlaceVote =
                                (heartPickModel?.heartPickIdols?.get(0)?.vote ?: 0).toFloat()
                            val totalVote = heartPickModel.vote.toFloat()
                            heartPickModel.heartPick1stPercent =
                                (100.0f * (firstPlaceVote) / totalVote).toInt()
                        }

                        if(heartPickModel.heartPickIdols?.size!! > 1) {
                            var currentRank = 1
                            var previousVote = heartPickModel.heartPickIdols.firstOrNull()?.vote ?: 0
                            var difference = 0

                            for (index in heartPickModel.heartPickIdols.indices) {
                                if (heartPickModel.heartPickIdols[index].vote == previousVote) {
                                    heartPickModel.heartPickIdols[index].rank =currentRank
                                    heartPickModel.heartPickIdols[index].diffVote = difference
                                } else {
                                    currentRank = index + 1
                                    heartPickModel.heartPickIdols[index].rank = currentRank
                                    difference = previousVote - heartPickModel.heartPickIdols[index].vote
                                    heartPickModel.heartPickIdols[index].diffVote = difference
                                    previousVote = heartPickModel.heartPickIdols[index].vote
                                }
                            }
                        }
                        if(isShare) {
                            if (context != null) {
                                shareHeartPick(context, heartPickModel)
                            }
                            return@get
                        }

                        heartPickModel.setFirstPlaceVote()
                        heartPickModel.setLastPlaceVote()
                        viewModelScope.launch(Dispatchers.Main) {
                            _getHeartPick.value = Event(heartPickModel)
                        }
                    } else {
                        nextResourceUrl = response?.optJSONObject("meta")
                            ?.optString("next")

                        if (nextResourceUrl != null &&
                            nextResourceUrl.equals("null")
                        ) {
                            nextResourceUrl = null
                        }

                        val listType = object : TypeToken<List<HeartPickModel?>?>() {}.type
                        val heartPickList = gson.fromJson<ArrayList<HeartPickModel>>(response?.optJSONArray("objects").toString(), listType)

                        if(heartPickList.isNullOrEmpty()) {
                            return@get
                        }

                        presentHeartPickList.addAll(heartPickList)
                        viewModelScope.launch(Dispatchers.Main) {
                            _getHeartPickList.value =
                                Event(presentHeartPickList.distinctBy { it.id }.toMutableList())
                        }
                    }
                }, { throwable ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val msg = throwable.message
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
            if (heartPickId == 0) {
                offset += limit
            }
        }
    }

    fun confirmUse(context: Context?, idol: HeartPickIdol?, heartPickId: Int) {
        if (Util.mayShowLoginPopup(context as BaseActivity)) {
            return
        }

        viewModelScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    if (response.optBoolean("success")) {
                        val gcode = response.optInt("gcode")
                        if (response.optString("active") == Const.RESPONSE_Y) {
                            if (response.optInt("total_heart") == 0) {
                                Util.showChargeHeartDialog(context)
                            } else {
                                if (response.optString("vote_able").equals(Const.RESPONSE_Y, ignoreCase = true)) {
                                    voteHeart(context, idol, response.optLong("total_heart"), response.optLong("free_heart"), heartPickId)
                                } else {

                                    if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                        Toast.makeText(context, context.getString(R.string.response_users_is_active_time_over), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.msg_not_able_vote), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            val start = Util.convertTimeAsTimezone(response.optString("begin"))
                            val end = Util.convertTimeAsTimezone(response.optString("end"))
                            val unableUseTime = String.format(
                                context.getString(R.string.msg_unable_use_vote), start, end)
                            Util.showIdolDialogWithBtn1(context,
                                null,
                                unableUseTime) { Util.closeIdolDialog() }
                        }
                    } else { // success is false!
                        val responseMsg = ErrorControl.parseError(context, response)
                        Toast.makeText(context, responseMsg, Toast.LENGTH_SHORT).show()
                    }
                }, {
                }
            )
        }
    }

    private fun voteHeart(context: Context?, idol: HeartPickIdol?, totalHeart: Long, freeHeart: Long, heartPickId: Int) {
        val dialogFragment = HeartPickVoteDialogFragment.getInstance(idol, totalHeart, freeHeart, heartPickId)
        dialogFragment.setOnDialogDismissListener {
            val reward = it?.get("reward") as HeartPickVoteRewardModel?
            if ((reward?.voted ?: 0) > 0) {
                it?.let { _voteCallBack.value = Event(it) }
            }
        }
        dialogFragment.show((context as BaseActivity).supportFragmentManager, "heart_pick_vote")
    }


    fun setDDay(context: Context, lifecycleScope: LifecycleCoroutineScope, heartPickModel: HeartPickModel)  {
        // 날짜 포맷 정의
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(context))

        // 두 날짜 파싱
        val now: Date = dateTimeFormat.parse(dateTimeFormat.format(Calendar.getInstance().time)) ?: throw IllegalArgumentException("Invalid start time format")
        val endDate: Date = dateTimeFormat.parse(heartPickModel.endAt) ?: throw IllegalArgumentException("Invalid end time format")

        // 두 날짜의 차이 계산 (밀리초)
        val diff = endDate.time - now.time

        when {
            diff < 0-> {
                _dDayText.value = Event(context.getString(R.string.vote_finish))
            }
            diff < 86400000 -> {
                countdownTimer(lifecycleScope, diff, context)
            }
            else -> {
                val dDay = diff / 86400000
                _dDayText.value = Event("D-$dDay")
            }
        }
    }

    private fun countdownTimer(
        lifecycleScope: LifecycleCoroutineScope,
        diff: Long,
        context: Context
    ) {
        timerJob = lifecycleScope.launch(Dispatchers.IO) {
            var remainingTime = diff
            while (remainingTime > 0) {
                val hours = remainingTime / 3600000
                val minutes = (remainingTime % 3600000) / 60000
                val seconds = (remainingTime % 60000) / 1000
                val timeFormat = SimpleDateFormat("HH:mm:ss", LocaleUtil.getAppLocale(context))
                val diffDate = Date(0, 0, 0, hours.toInt(), minutes.toInt(), seconds.toInt())

                withContext(Dispatchers.Main){
                    _dDayText.value = Event(timeFormat.format(diffDate))
                }

                delay(1000)
                remainingTime -= 1000

                if(remainingTime <= 0) {
                    return@launch
                }
            }
        }
    }


    @SuppressLint("StringFormatMatches")
    private fun shareHeartPick(context:Context, heartPickModel: HeartPickModel){
        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(context))
        var top1IdolRank:String? = ""
        var top2IdolRank:String? = ""
        var top3IdolRank:String? = ""

        var top1IdolName:String? = ""
        var top2IdolName:String? = ""
        var top3IdolName:String? = ""

        if(heartPickModel.heartPickIdols.isNullOrEmpty()) return

        when {
            heartPickModel.heartPickIdols.size > 2 -> {
                top1IdolName = UtilK.removeWhiteSpace(shareHeartPickRankName(heartPickModel.heartPickIdols[0].title?:"", heartPickModel.heartPickIdols[0].subtitle?:""))
                top2IdolName = UtilK.removeWhiteSpace(shareHeartPickRankName(heartPickModel.heartPickIdols[1].title?:"", heartPickModel.heartPickIdols[1].subtitle?:""))
                top3IdolName = UtilK.removeWhiteSpace(shareHeartPickRankName(heartPickModel.heartPickIdols[2].title?:"", heartPickModel.heartPickIdols[2].subtitle?:""))

                top1IdolRank = String.format(context.getString(R.string.rank_format), numberFormat.format(1))
                top2IdolRank = String.format(context.getString(R.string.rank_format), numberFormat.format(2))
                top3IdolRank = String.format(context.getString(R.string.rank_format), numberFormat.format(3))
            }
            heartPickModel.heartPickIdols.size == 2 -> {
                top1IdolName = UtilK.removeWhiteSpace(shareHeartPickRankName(heartPickModel.heartPickIdols[0].title?:"", heartPickModel.heartPickIdols[0].subtitle?:""))
                top2IdolName = UtilK.removeWhiteSpace(shareHeartPickRankName(heartPickModel.heartPickIdols[1].title?:"", heartPickModel.heartPickIdols[1].subtitle?:""))
                top1IdolRank = String.format(context.getString(R.string.rank_format), numberFormat.format(1))
                top2IdolRank = String.format(context.getString(R.string.rank_format), numberFormat.format(2))
            }

            heartPickModel.heartPickIdols.size == 1 -> {
                top1IdolName = UtilK.removeWhiteSpace(shareHeartPickRankName(heartPickModel.heartPickIdols[0].title?:"", heartPickModel.heartPickIdols[0].subtitle?:""))
                top1IdolRank = String.format(context.getString(R.string.rank_format), numberFormat.format(1))
            }
        }


        val params = listOf(LinkStatus.HEARTPICK.status, heartPickModel.id.toString())
        val url = LinkUtil.getAppLinkUrl(context = context, params = params)

        val msg = String.format(
            context.getString(R.string.heartpick_share_msg),
            heartPickModel.title,
            if (BuildConfig.CELEB) context.getString(R.string.choeaedol_celeb) else context.getString(
                R.string.app_name
            ),
            top1IdolRank,
            top1IdolName,
            top2IdolRank,
            top2IdolName,
            top3IdolRank,
            top3IdolName
        ).plus(url)
        UtilK.linkStart(context, url = "", msg = msg.trimNewlineWhiteSpace())
    }

    private fun shareHeartPickRankName(title: String, subtitle: String): String{

        val name = if (subtitle.isEmpty()) {
            title
        } else {
            "${title}_${subtitle}"
        }
        return name
    }

    fun updateHasGoneToolTip(hasGoneToolTip: Boolean) {
        this.hasGoneToolTip = hasGoneToolTip
    }

    fun getNewPick(): Boolean {
        val listType = object : TypeToken<NewPicksModel>() {}.type
        val newPicks: NewPicksModel? =
            IdolGson
                .getInstance()
                .fromJson(Util.getPreference(getApplication(), Const.PREF_NEW_PICKS), listType)

        return newPicks?.let {
            newPicks.heartpick
        } ?: false
    }

    fun getNextResourceUrl(): String? {
        return nextResourceUrl
    }

    fun getIdolById(idolId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val idol = getIdolByIdUseCase(idolId)
            .mapDataResource { it?.toPresentation() }
            .awaitOrThrow()
        idol?.let {
            withContext(Dispatchers.Main) {
                _idol.postValue(Event(it))
            }
        }
    }
}