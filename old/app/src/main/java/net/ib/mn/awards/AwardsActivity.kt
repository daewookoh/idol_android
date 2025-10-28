package net.ib.mn.awards

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.android.volley.toolbox.RequestFuture
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.EventActivity
import net.ib.mn.activity.NoticeActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.awards.AwardsGuideFragment.Companion.GUIDE_EXAMPLE
import net.ib.mn.awards.AwardsGuideFragment.Companion.GUIDE_INSTRUCTION
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.model.AwardModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ActivityAwardsBinding
import net.ib.mn.domain.usecase.DeleteAllAndSaveAwardsIdolUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.FrontBannerModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.toDomain
import net.ib.mn.model.toPresentation
import net.ib.mn.support.SupportDetailActivity.Companion.createIntent
import net.ib.mn.support.SupportInfoActivity
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.utils.Const
import net.ib.mn.utils.ExtendedDataHolder.Companion.getInstance
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.sort
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

/**
 * 셀럽용 어워즈 화면: 이제 안쓰는거 같은데..?
 */

@AndroidEntryPoint
class AwardsActivity : BaseActivity(), AwardsGuideFragment.CheckGuideTabPositionListener, View.OnClickListener {

    companion object {
        var awardBarIndex = 0
        const val ACTOR_BOY = 0
        const val ACTOR_GIRL = 1
        const val TROT = 2
    }

    @Inject
    lateinit var idolsRepository: IdolsRepository

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    @Inject
    lateinit var deleteAllAndSaveAwardsIdolUseCase: DeleteAllAndSaveAwardsIdolUseCase

    // 서포트 status(0:진행중, 1:성공, 2:실패)을 가져오기위한 모델
    private lateinit var supportModel: SupportListModel

    private var checkGuideGTab = -1
    private var mGlideRequestManager: RequestManager? = null
    var awardsFrag: BaseFragment? = null
    var account: IdolAccount? = null
    private lateinit var awardData: AwardModel

    private lateinit var binding: ActivityAwardsBinding

    // 가이드 화면에   탬에 따라서  엑티비티  위  탭 메뉴가 보이고 안보이고를 결정한다.
    override fun checkCurrentTab(currentTab: Int) {
        if (currentTab == 0) {
            checkGuideGTab = GUIDE_INSTRUCTION
            binding.awardTopBar.visibility = View.GONE
        } else {
            checkGuideGTab = GUIDE_EXAMPLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_awards)
        binding.rlContainer.applySystemBarInsets()
        showEmptyView()

//        awardData = IdolGson.getInstance().fromJson(Util.getPreference(this, Const.AWARD_MODEL), AwardModel::class.java)
        awardData = Json{ignoreUnknownKeys = true}.decodeFromString<AwardModel>(Util.getPreference(this, Const.AWARD_MODEL))

        mGlideRequestManager = Glide.with(this)

        val actionbar = supportActionBar
        actionbar!!.title = awardData.awardTitle

        account = IdolAccount.getAccount(this)

        // 어워드 용  전면 배너 가져오기
        getAwardBanner()

        when (ConfigModel.getInstance(this).votable) {
            "B" -> {
                awardsFrag = AwardsGuideFragment(this)

                // 가이드 프래그먼트 맨처음 나올때에는
                // 안내 화면이 먼저 나와야 함으로
                // award_top_bar Gone 처리 해줘야됨
                if (checkGuideGTab != 1) {
                    binding.awardTopBar.visibility = View.GONE
                }
            }
            "Y" -> {
                awardsFrag = AwardsMainFragment()
            }
            "A" -> {
                awardsFrag = AwardsResultFragment()
            }
        }

        binding.btnActorBoy.setOnClickListener(this)
        binding.btnActorGirl.setOnClickListener(this)
        binding.btnTrot.setOnClickListener(this)

        getAllAwardsIdolList()
    }

    // 어워드 용 전면 배너가 있는지  판단해서 띄어줌.
    private fun getAwardBanner() {
        val neverEventList = Util.getPreference(this, Const.PREF_NEVER_SHOW_EVENT)
        val extendedDataHolder = getInstance()
        if (extendedDataHolder.hasExtra("bannerList")) {
            val fronbannerlist = extendedDataHolder.getExtra("bannerList") as ArrayList<FrontBannerModel?>?

            val awardBanner = fronbannerlist?.find { it?.type == "A" }

            if (awardBanner != null && !awardBanner.isClosed) {
                // 메모리 부족으로  엑티비티  다시 시작할때,
                // null exception 나와서 예외 처리 적용 .
                try {
                    awardBanner.isClosed = true
                    val eventNo = awardBanner.eventNum
                    val targetMenu = awardBanner.targetMenu
                    val targetId = awardBanner.targetId
                    val imgUrl = awardBanner.url
                    val goUrl = awardBanner.goUrl
                    val readNoticeArray: Array<String> =
                        neverEventList.split(",".toRegex()).toTypedArray()
                    if (neverEventList == "") {
                        showIdolGuideDialog(eventNo, imgUrl, goUrl, targetMenu, targetId)
                    } else {
                        if (!Util.isFoundString(eventNo, readNoticeArray)) {
                            showIdolGuideDialog(eventNo, imgUrl, goUrl, targetMenu, targetId)
                        }
                    }
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showIdolGuideDialog(
        event_no: String,
        imgUrl: String,
        goUrl: String?,
        target_menu: String,
        target_id: Int,
    ) {
        val eventDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventDialog.window!!.attributes = lpWindow
        eventDialog.window!!.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        val cbCheckGuide: AppCompatCheckBox
        val dialogBtnClose: AppCompatButton
        val imgEvent: AppCompatImageView

        eventDialog.setContentView(R.layout.dialog_event)
        eventDialog.setCanceledOnTouchOutside(false)
        eventDialog.setCancelable(true)
        imgEvent = eventDialog.findViewById(R.id.img_event)
        imgEvent.setOnClickListener { v: View? ->
            if (!TextUtils.isEmpty(target_menu)) {
                if (target_menu.equals("notice", ignoreCase = true)) {
                    startActivity(NoticeActivity.createIntent(this, target_id))
                } else if (target_menu.equals("event", ignoreCase = true)) {
                    startActivity(EventActivity.createIntent(this, target_id))
                } else if (target_menu.equals("idol", ignoreCase = true)) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val idol = getIdolByIdUseCase(target_id)
                            .mapDataResource { it?.toPresentation() }
                            .awaitOrThrow()
                        idol?.let {
                            withContext(Dispatchers.Main) {
                                startActivity(CommunityActivity.createIntent(this@AwardsActivity, idol))
                            }
                        }
                    }
                } else if (target_menu.equals("support", ignoreCase = true)) {
                    if (target_id == 0) {
                        startActivity(SupportInfoActivity.createIntent(this))
                    } else {
                        getSupportList(target_id)
                    }
                }
            } else if (goUrl != null) {
                if (goUrl != "") {
                    try {
                        val intent = Intent(this, AppLinkActivity::class.java).apply {
                            data = Uri.parse(goUrl)
                        }

                        startActivity(
                            intent
                        )
                    } catch (e: ActivityNotFoundException) {
                        Util.showIdolDialogWithBtn1(
                            this,
                            null,
                            getString(R.string.msg_error_ok),
                        ) { v1: View? -> Util.closeIdolDialog() }
                        e.printStackTrace()
                    }
                }
            }
        }
        mGlideRequestManager
            ?.load(imgUrl)
            ?.apply(
                RequestOptions()
                    .override(600, 800)
                    .fitCenter(),
            )
            ?.into(imgEvent)
        cbCheckGuide = eventDialog.findViewById(R.id.check_guide)
        cbCheckGuide.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> }
        dialogBtnClose = eventDialog.findViewById(R.id.btn_close)
        dialogBtnClose.setOnClickListener { v: View? ->
            if (cbCheckGuide.isChecked) {
                saveNeverShowEvent(event_no)
            }
            eventDialog.cancel()
        }
        eventDialog.setOnKeyListener { arg0: DialogInterface?, keyCode: Int, event: KeyEvent? ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                eventDialog.cancel()
                return@setOnKeyListener true
            }
            false
        }
        eventDialog.window!!.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT),
        )
        eventDialog.show()
    }

    private fun saveNeverShowEvent(event_no: String) {
        val never_event_list = Util.getPreference(this, Const.PREF_NEVER_SHOW_EVENT)
        val never_event_total: String
        val read_notice_array = never_event_list.split(",".toRegex()).toTypedArray()
        if (never_event_list == "") {
            never_event_total = never_event_list + event_no
            Util.setPreference(
                this,
                Const.PREF_NEVER_SHOW_EVENT,
                never_event_total,
            )
        } else if (!Util.isFoundString(event_no, read_notice_array)) {
            never_event_total = "$never_event_list,$event_no"
            Util.setPreference(
                this,
                Const.PREF_NEVER_SHOW_EVENT,
                never_event_total,
            )
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
        }
    }

    // 해당 게시물의 status값(성공여부)를 가져오기위해 호출.
    private fun getSupportList(targetId: Int) {
        MainScope().launch {
            supportRepository.getSupports(
                limit = 100,
                offset = 0,
                listener = { response ->
                    try {
                        if (response.optBoolean("success")) {
                            val array = response.getJSONArray("objects")
                            val gson = IdolGson.getInstance(true)
                            val items = ArrayList<SupportListModel>()
                            if (array.length() != 0) {
                                for (i in 0 until array.length()) {
                                    items.add(
                                        gson.fromJson(
                                            array.getJSONObject(i).toString(),
                                            SupportListModel::class.java,
                                        ),
                                    )

                                    // targetId와 서버에서 불러온 아이디값만 비교해서 같으면 넣어준다.
                                    if (targetId == items[i].id) {
                                        supportModel = items[i]
                                    }
                                }
                            }

                            // 비동기처리 늦게 될수도 있으니까 null체크 추가 그리고 0이면 기본상세페이지, 1이면 인증샷화면으로...
                            if (supportModel == null || supportModel.status == 0) {
                                startActivity(
                                    createIntent(this@AwardsActivity, targetId),
                                )
                            } else if (supportModel != null && supportModel.status == 1) {
                                // 성공은 인증샷 페이지로 가기로한다.
                                startActivity(
                                    SupportPhotoCertifyActivity.createIntent(
                                        this@AwardsActivity,
                                        getSupportInfo(supportModel),
                                    ),
                                )
                            }
                        } else {
                            UtilK.handleCommonError(this@AwardsActivity, response)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                errorListener = { throwable ->
                    if (Util.is_log()) this@AwardsActivity.showMessage(throwable.message)
                }
            )
        }
    }

    private fun getSupportInfo(supportListModel: SupportListModel): String {
        // 서포트  관련 필요 정보를  서포트 인증샷 화면에 json 화 시켜서 넘겨준다.
        val supportInfo = JSONObject()
        try {
            if (supportListModel.idol.getName(this@AwardsActivity).contains("_")) {
                supportInfo.put(
                    "name",
                    supportListModel.idol.getName(this@AwardsActivity).split("_".toRegex())
                        .toTypedArray()[0],
                )
                supportInfo.put(
                    "group",
                    supportListModel.idol.getName(this@AwardsActivity).split("_".toRegex())
                        .toTypedArray()[1],
                )
            } else {
                supportInfo.put("name", supportListModel.idol.getName(this@AwardsActivity))
            }
            supportInfo.put("support_id", supportListModel.id)
            supportInfo.put("title", supportListModel.title)
            supportInfo.put("profile_img_url", supportListModel.image_url)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return supportInfo.toString()
    }

    // 어워즈 관련 아이돌,배우 리스트를 전부 가져옵니다.
    private fun getAllAwardsIdolList() {
        lifecycleScope.launch {
            try {
                val future = RequestFuture.newFuture<JSONObject>()

                val awardModel = Json{ignoreUnknownKeys = true}.decodeFromString<AwardModel>(Util.getPreference(this@AwardsActivity, Const.AWARD_MODEL))

                val chartModels = awardModel.charts

                val charCodes = arrayListOf<String>()
                for (i in 0 until (chartModels?.size ?: 0)) {
                    charCodes.add(chartModels?.get(i)?.code ?: break)
                }

                val chartCodeAll = charCodes.joinToString(",")

                val response = idolsRepository.getAwardIdols(chartCodeAll, null)

                if (!response.optBoolean("success")) {
                    return@launch
                }

                val objects = response.getJSONArray("objects")

                val gson = IdolGson.getInstance()
                val listType = object : TypeToken<ArrayList<IdolModel>>() {}.type
                val idolList = gson.fromJson<ArrayList<IdolModel>>(objects.toString(), listType)

                val idols = sort(this@AwardsActivity, idolList)

                lifecycleScope.launch(Dispatchers.IO) {
                    deleteAllAndSaveAwardsIdolUseCase(idols.map { it.toDomain() }).collectLatest {
                        hideEmptyView()

                        if (supportFragmentManager.findFragmentByTag("AwardsMain")?.isAdded != true && !supportFragmentManager.isDestroyed) {
                            supportFragmentManager
                                .beginTransaction()
                                .add(R.id.container, awardsFrag!!, "AwardsMain")
                                .commit()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showEmptyView() {
        binding.tvEmpty.visibility = View.VISIBLE
        binding.container.visibility = View.GONE
    }

    private fun hideEmptyView() {
        binding.tvEmpty.visibility = View.GONE
        binding.container.visibility = View.VISIBLE
    }
}