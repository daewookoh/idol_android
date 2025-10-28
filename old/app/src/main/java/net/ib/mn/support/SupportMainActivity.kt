package net.ib.mn.support

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.TypedValue
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MyHeartInfoActivity
import net.ib.mn.adapter.SupportMainAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.ActivitySupportMainBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.SupportAdType
import net.ib.mn.model.SupportListModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.setFirebaseUIAction
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SupportMainActivity : BaseActivity(), View.OnClickListener,
    SupportMainAdapter.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var articlesRepository: ArticlesRepository
    @Inject
    lateinit var usersRepository: UsersRepository

    private lateinit var mGlideRequestManager: RequestManager

    private lateinit var displayErrorHandler: Handler
    private lateinit var supportMainAdapter: SupportMainAdapter

    //페이징 처리
    private var totalCount: Int = 0
    private val limit = 100
    private val offset = 0

    //정렬 변수.
    private var mOrderBy = "-created_at"
    private var groupId = ""
    private var status = 0

    // 카테고리
    private val selectTagList = arrayListOf(SupportAdType.KOREA, SupportAdType.MOBILE, SupportAdType.FOREIGN)

    //서포트 리스트.
    private lateinit var items: ArrayList<SupportListModel>

    //타입 리스트.
    private lateinit var typeList: ArrayList<SupportAdTypeListModel>

    //왼쪽상단 성공한 서포트 토글 flag
    private var successFlag = true

    //현재 날짜 가져오기
    private lateinit var mCal: Calendar

    private var deepLinkstatus: String? = null

    private lateinit var binding: ActivitySupportMainBinding

    private var startActivityForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val article = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra(
                    Const.EXTRA_ARTICLE,
                    ArticleModel::class.java,
                )
            } else {
                result.data?.getSerializableExtra(Const.EXTRA_ARTICLE) as ArticleModel?
            }
            when (result.resultCode) {
                ResultCode.SUPPORT_WRITE.value -> {
                    getSupportList()
                }

                ResultCode.SUPPORT_VOTE_UPDATE.value -> {
                    val updatedVoteCountId =
                        result.data?.getIntExtra(SupportDetailActivity.UPDATE_VOTE_COUNT_ID, 0)
                    val updatedVoteCount =
                        result.data?.getIntExtra(SupportDetailActivity.UPDATE_VOTE_COUNT, 0)

                    items.find { it.id == updatedVoteCountId }?.apply {
                        diamond = updatedVoteCount ?: return@apply
                    }

                    applyItems(items = items, month = mCal[Calendar.MONTH] + 1, status = status)
                }
                ResultCode.COMMENTED.value, ResultCode.UPDATE_LIKE_COUNT.value -> {
                    getArticleResource(article)
                }
            }
        }

    override fun onNewIntent(intent: Intent) { //화면 유지 상태이고, 링크타고 들어올때.
        super.onNewIntent(intent)
        if (intent.extras != null) {
            deepLinkstatus = intent.getStringExtra(EXTRA_SUPPORT_STATUS)
            getSupportList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_main)
        binding.supportMainFrag.applySystemBarInsets()

        deepLinkstatus = intent.getStringExtra(EXTRA_SUPPORT_STATUS)
        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.support)

        mGlideRequestManager = Glide.with(this)

        displayErrorHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val responseMsg = msg.obj as String
                Toast.makeText(this@SupportMainActivity, responseMsg, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        mCal = Calendar.getInstance()
        typeList = arrayListOf()
        getTypeList()
        setRefresh()
        if (BuildConfig.CELEB) setCelebTutorials() else setTutorials()
        items = arrayListOf()
        binding.supportMainRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        supportMainAdapter = SupportMainAdapter(
            this,
            mGlideRequestManager,
            this,
            typeList
        )
        binding.supportMainRv.adapter = supportMainAdapter

        binding.tvAllTag.isSelected = true

        setTagEventListener()
        setFilter()
        getSupportList()
        binding.ivNext.setOnClickListener {
            mCal.add(Calendar.MONTH, 1)
            val sdf = SimpleDateFormat("MMM", LocaleUtil.getAppLocale(this))
            binding.tvYear.text = mCal[Calendar.YEAR].toString()
            binding.tvMonth.text = sdf.format(mCal.time)
            getSupportList()
        }

        binding.ivPrev.setOnClickListener {
            mCal.add(Calendar.MONTH, -1)
            val sdf = SimpleDateFormat("MMM", LocaleUtil.getAppLocale(this))
            binding.tvYear.text = mCal[Calendar.YEAR].toString()
            binding.tvMonth.text = sdf.format(mCal.time)
            getSupportList()
        }

        //시스템 font scale  받아옴.
        val scale = this.resources?.configuration?.fontScale

        if (scale!! >= 1.5f) {
            binding.supportTitleTv.layoutParams.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                65F,
                resources.displayMetrics
            ).toInt()
            binding.clMonthChoice.layoutParams.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                65F,
                resources.displayMetrics
            ).toInt()
            binding.supportMainCon.layoutParams.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                65F,
                resources.displayMetrics
            ).toInt()
        }

        //다크모드 전환하고 회원탈퇴할때  여기 로직을 타서  exception 나와서 try catch 해줌.
        try {
            binding.supportMainTvCreate.text =
                String.format(getString(R.string.support_use_diamond), typeList[0].require)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        binding.supportMainLiCreate.setOnClickListener(this)
        binding.supportMainLiMydia.setOnClickListener(this)
        binding.supportInfoLi.setOnClickListener(this)
        binding.supportInfoBtn.setOnClickListener(this)
    }

    override fun onRefresh() {
        binding.supportMainRv.postDelayed({
            getSupportList()
            binding.swipeRefresh.isRefreshing = false
            binding.supportMainRv.smoothScrollToPosition(0)
        }, 500)
    }

    private fun setTutorials() {
        if (TutorialManager.getTutorialIndex() == TutorialBits.SUPPORT_TOGGLE) {
            setupLottieTutorial(binding.lottieTutorialSupport) {
                updateTutorial(TutorialBits.SUPPORT_TOGGLE)
                onClickToggle()
            }
        }
    }

    private fun setCelebTutorials() {
        if (TutorialManager.getTutorialIndex() == CelebTutorialBits.SUPPORT_TOGGLE) {
            setupLottieTutorial(binding.lottieTutorialSupport) {
                updateTutorial(CelebTutorialBits.SUPPORT_TOGGLE)
                onClickToggle()
            }
        }
    }

    private fun onClickToggle() {
        if (successFlag) {
            binding.tvFilter2.text = getString(R.string.order_by_dday)
            binding.supportInfoIv.setImageResource(R.drawable.icon_home)
            binding.supportSuccessTv.text = getString(R.string.support_main)
            status = STATUS_SUCCESS
            val sdf = SimpleDateFormat("MMM", LocaleUtil.getAppLocale(this@SupportMainActivity))
            binding.tvYear.text = mCal[Calendar.YEAR].toString()
            binding.tvMonth.text = sdf.format(mCal.time)
            successFlag = false
            mOrderBy = "expired_at"
            binding.supportInfoBtn.visibility = View.GONE
            binding.clMonthChoice.visibility = View.VISIBLE
            binding.supportTitleTv.visibility = View.GONE
        } else {
            binding.tvFilter2.text = getString(R.string.freeboard_order_newest)
            binding.supportInfoIv.setImageResource(R.drawable.icon_camera)
            binding.supportSuccessTv.text = getString(R.string.support_success_list)
            status = STATUS_IN_PROGRESS
            successFlag = true
            mOrderBy = "-created_at"
            binding.supportInfoBtn.visibility = View.VISIBLE
            binding.clMonthChoice.visibility = View.GONE
            binding.supportTitleTv.visibility = View.VISIBLE
        }
        getSupportList()
    }

    private fun setRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this)
    }

    private fun setFilter() {
        val firstFilter = binding.supportMainLiFilter
        val secondFileter = binding.supportMainLiFilter2

        val firstSheet =
            BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_SUPPORT_MAIN_FILTER_FIRST)
        val secondSeet =
            BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_SUPPORT_MAIN_FILTER_SECOND)
        val secondCertifySheet =
            BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_SUPPORT_MAIN_FILTER_SECOND_CERTIFY)

        firstFilter.setOnClickListener {
            val tag = "filter"
            val oldFrag = supportFragmentManager.findFragmentByTag(tag)
            if (oldFrag == null) {
                firstSheet.show(supportFragmentManager, tag)
            }
        }

        secondFileter.setOnClickListener {
            val tag = "filter"
            val oldFrag = supportFragmentManager.findFragmentByTag(tag)
            if (oldFrag == null) {
                if (successFlag) {
                    secondSeet.show(supportFragmentManager, tag)
                } else {
                    secondCertifySheet.show(supportFragmentManager, tag)
                }
            }
        }
    }

    private fun getSupportList() {

        var yearMonth: String = if (successFlag) {
            ""
        } else {
            //Month 설정할땐 1자리일때도있으니까 앞에 0포맷 넣어주고 아랍어일땐 Locale을 US로 고정해준다(안해주면 format때문에 아랍어숫자로 번역되버림.)
            mCal[Calendar.YEAR].toString() + "" + String.format(
                Locale.US,
                "%02d",
                mCal[Calendar.MONTH] + 1
            )
        }

        supportMainAdapter.setCategory(selectTagList)
        MainScope().launch {
            supportRepository.getSupports(
                limit,
                offset,
                groupId,
                mOrderBy,
                status.toString(),
                yearMonth,
                { response ->
                    runOnUiThread {
                        try {
                            binding.loading.visibility = View.GONE
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }

                    if (!response.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(this@SupportMainActivity, response) ?: return@getSupports
                        val msg = displayErrorHandler.obtainMessage()
                        msg.what = 0
                        msg.arg1 = 0
                        msg.obj = responseMsg
                        displayErrorHandler.sendMessage(msg)
                        return@getSupports
                    }

                    items.clear()
                    totalCount = response.getJSONObject("meta").optInt("total_count", 0)
                    val array = response.getJSONArray("objects")
                    val gson = IdolGson.getInstance(false) // 서버에서 UTC로 줌
                    Util.log("SupportMainFragment::getSupportList -> $array")

                    if (array.length() == 0) {
                        runOnUiThread {
                            showEmptyView()
                        }
                    } else {
                        val listType =
                            object : TypeToken<List<SupportListModel>>() {}.type
                        val supports: List<SupportListModel> =
                            gson.fromJson(array.toString(), listType)

                        items.addAll(supports)

                        val filteredItems = items.filter { item ->
                            selectTagList.any { category ->
                                category.label == item.type.category
                            }
                        }

                        runOnUiThread {
                            // TODO 실섭 테스트용
//                        applyItems(
//                            items = items,
//                            month = mCal[Calendar.MONTH] + 1,
//                            status = status
//                        )
                            if (filteredItems.isEmpty()) {
                                showEmptyView()
                            } else {
                                hideEmptyView()

                                applyItems(
                                    items = filteredItems,
                                    month = mCal[Calendar.MONTH] + 1,
                                    status = status
                                )
                            }
                        }
                    }

                    runOnUiThread {
                        if (deepLinkstatus != null) {
                            if (deepLinkstatus != "inprogress") {
                                successFlag = true
                                binding.supportInfoLi.callOnClick()
                            } else {
                                successFlag = false
                                binding.supportInfoLi.callOnClick()
                            }
                            //다음번에 타고들어올때는 링크가 아닐수도 있으므로 null설정해서 if문 못타게 설정. 만약 타고 들어온다면 onCreate나 onNewIntent에서 초기화됨.
                            deepLinkstatus = null
                        }
                    }
                },
                { throwable ->
                    val msg = displayErrorHandler.obtainMessage()
                    msg.what = 0
                    msg.arg1 = 0
                    msg.obj = throwable.message
                    displayErrorHandler.sendMessage(msg)
                }
            )
        }
    }

    private fun setTagEventListener() {
        val mainTagList = arrayListOf(
            binding.tvAllTag,
            binding.tvDomesticTag,
            binding.tvForeignTag,
            binding.tvMobileTag
        )

        // 태그에 매핑된 SupportAdType 값 설정
        val tagMapping = mapOf(
            binding.tvDomesticTag to SupportAdType.KOREA,
            binding.tvMobileTag to SupportAdType.MOBILE,
            binding.tvForeignTag to SupportAdType.FOREIGN
        )

        // 공통 동작을 처리하는 함수
        fun setupTagListeners(allTag: View, tagList: List<View>, selectedTags: MutableList<SupportAdType>) {
            allTag.setOnClickListener {
                setFirebaseUIAction(GaAction.SUPPORT_SORT_IN_LIST)
                tagList.forEach { tag ->
                    tag.isSelected = false
                }
                allTag.isSelected = true

                // selectTagList를 모든 SupportAdType으로 초기화
                selectedTags.clear()
                selectedTags.addAll(SupportAdType.values())

                val filteredItems = items.filter { item ->
                    selectedTags.any { category -> category.label == item.type.category }
                }

                if (filteredItems.isEmpty()) {
                    showEmptyView()
                } else {
                    hideEmptyView()

                    applyItems(
                        items = filteredItems,
                        month = mCal[Calendar.MONTH] + 1,
                        status = status
                    )
                }
            }

            tagList.forEach { tag ->
                tag.setOnClickListener {
                    setFirebaseUIAction(GaAction.SUPPORT_SORT_IN_LIST)

                    val shouldSelect = !tag.isSelected

                    allTag.isSelected = false
                    tagList.forEach { it.isSelected = false }

                    selectedTags.clear()

                    if (shouldSelect) {
                        tag.isSelected = true
                        tagMapping[tag]?.let { selectedTags.add(it) }
                    }

                    if (selectedTags.isEmpty()) {
                        allTag.isSelected = true
                        selectedTags.addAll(SupportAdType.values())
                    }

                    val filteredItems = items.filter { item ->
                        selectedTags.any { category -> category.label == item.type.category }
                    }

                    if (filteredItems.isEmpty()) {
                        showEmptyView()
                    } else {
                        hideEmptyView()
                        applyItems(
                            items = filteredItems,
                            month = mCal[Calendar.MONTH] + 1,
                            status = status
                        )
                    }
                }
            }
        }

        setupTagListeners(binding.tvAllTag, mainTagList.filter { it != binding.tvAllTag }, selectTagList)
    }

    private fun getArticleResource(articleModel: ArticleModel?) {
        lifecycleScope.launch {
            articlesRepository.getArticle(
                (articleModel ?: return@launch).resourceUri,
                { response ->
                    try {
                        val gson = IdolGson.getInstance(true)

                        val model = gson.fromJson(response.toString(), ArticleModel::class.java)

                        items.find { it.article.id == model.id }?.apply {
                            this.article.apply {
                                heart = model.heart
                                commentCount = model.commentCount
                                content = model.content
                                linkDesc = model.linkDesc
                                linkTitle = model.linkTitle
                                linkUrl = model.linkUrl
                                likeCount = model.likeCount
                                isUserLike = model.isUserLike
                                isUserLikeCache = model.isUserLikeCache
                            }
                        }
                        supportMainAdapter.notifyDataSetChanged()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, {
                    Util.closeProgress()
                }
            )
        }
    }

    private fun updateTutorial(tutorialIndex: Int) = lifecycleScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                }
            },
            errorListener = { throwable ->
                // no-op
            }
        )
    }

    private fun applyItems(items: List<SupportListModel>, month: Int, status: Int) {
        supportMainAdapter.setItems(items, month, status) { isEmpty ->
            if (isEmpty) {
                showEmptyView()
            } else {
                hideEmptyView()
            }
        }
        supportMainAdapter.notifyDataSetChanged()
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.support_main_li_create -> {
                SupportWriteActivity.createIntent(this)?.let {
                    startActivityForResult.launch(
                        it
                    )
                }
            }

            R.id.support_main_li_mydia -> {
                startActivity(MyHeartInfoActivity.createIntent(v.context))
            }
            R.id.support_info_li -> {
                if (binding.lottieTutorialSupport.isVisible) return
//                startActivity(SupportInfoActivity.createIntent(context))
                onClickToggle()
            }
            R.id.support_info_btn -> {
                startActivity(SupportInfoActivity.createIntent(this))
            }

        }
    }

    override fun onItemClicked(
        item: SupportListModel,
        view: View,
        position: Int,
        adPeriod: String
    ) {
        when (view.id) {
            R.id.support_item_main_con -> {
                if (item.status == STATUS_IN_PROGRESS) {//진행중일때
                    startActivityForResult.launch(
                        SupportDetailActivity.createIntent(
                            this,
                            item.id
                        )
                    )
                } else if (item.status == STATUS_SUCCESS) {//성공일때
                    startActivityForResult.launch(
                        SupportPhotoCertifyActivity.createIntent(
                            this,
                            getSupportInfo(item)
                        )
                    )
                }
            }
        }
    }

    private fun getSupportInfo(supportListModel: SupportListModel): String {
        //서포트  관련 필요 정보를  서포트 인증샷 화면에 json 화 시켜서 넘겨준다.

        val supportInfo = JSONObject()

        if (supportListModel.idol.getName(this).contains("_")) {
            supportInfo.put("name", Util.nameSplit(this, supportListModel.idol)[0])
            supportInfo.put("group", Util.nameSplit(this, supportListModel.idol)[1])
        } else {
            supportInfo.put("name", supportListModel.idol.getName(this))
        }
        supportInfo.put("support_id", supportListModel.id)
        supportInfo.put("title", supportListModel.title)
        supportInfo.put("profile_img_url", supportListModel.image_url)

        return supportInfo.toString()
    }

    private fun showEmptyView() {
        binding.emptyView.visibility = View.VISIBLE
        binding.supportMainRv.visibility = View.GONE
    }

    private fun hideEmptyView() {
        binding.emptyView.visibility = View.GONE
        binding.supportMainRv.visibility = View.VISIBLE
    }

    fun filterByAll() {
        if (groupId != "") {
            groupId = ""
            binding.tvFilter.text = getString(R.string.face_all)
            getSupportList()
        }
    }

    fun filterByMyFav() {
        if (groupId == "") {
            val most = IdolAccount.getAccount(this)?.most
            groupId = if(most == null) 0.toString()
            else {
                if(BuildConfig.CELEB) {
                    most.getId().toString()
                } else {
                    most.groupId.toString()
                }
            }
            binding.tvFilter.text = getString(if(BuildConfig.CELEB) R.string.actor_support_filter_favorites else R.string.support_filter_favorites)
            getSupportList()
        }
    }

    fun filterByLatest() {
        mOrderBy = if (status == STATUS_IN_PROGRESS) {
            "-created_at"
        } else {
            "-finished_at"
        }
        binding.tvFilter2.text = getString(R.string.freeboard_order_newest)
        getSupportList()
    }

    fun filterByAchievement() {
        mOrderBy = "-achievement"
        binding.tvFilter2.text = getString(R.string.sort_by_achievement)
        getSupportList()
    }

    fun filterByDeadLine() {
        if (successFlag) {
            binding.tvFilter2.text = getString(R.string.support_filter_due)
        } else {
            binding.tvFilter2.text = getString(R.string.order_by_dday)
        }

        if (mOrderBy != "expired_at") {
            mOrderBy = "expired_at"
            getSupportList()
        }
    }

    fun filterByName() {
        binding.tvFilter2.text = getString(R.string.order_by_name)
        mOrderBy = if (LocaleUtil.getAppLocale(this).language == "ko") {
            "idol__name"
        } else {
            "idol__name" + "_${Locale.ENGLISH}"
        }
        getSupportList()
    }

    private fun getTypeList() {
        try {
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<SupportAdTypeListModel>>() {}.type
            typeList = gson.fromJson(Util.getPreference(this, Const.AD_TYPE_LIST), listType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        const val STATUS_SUCCESS = 1
        const val STATUS_IN_PROGRESS = 0

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, SupportMainActivity::class.java)
        }

        @JvmStatic
        fun createIntent(context: Context, status: String?): Intent {
            val intent = Intent(context, SupportMainActivity::class.java)
            intent.putExtra(EXTRA_SUPPORT_STATUS, status)
            return intent
        }
    }
}