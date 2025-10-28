package net.ib.mn.onepick

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.collection.SparseArrayCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.OnepickMatchAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.dto.OnepickVoteDTO
import net.ib.mn.core.data.repository.OnepickRepositoryImpl
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.databinding.ActionBarTitleAndImageBinding
import net.ib.mn.databinding.ActivityOnepickMatchBinding
import net.ib.mn.dialog.DefaultDialog
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.onepick.OnePickMainFragment.Companion.IMAGE_PICK_LIST_UPDATE_RESULT_CODE
import net.ib.mn.onepick.OnePickMainFragment.Companion.THEME_PICK_LIST_UPDATE_RESULT_CODE
import net.ib.mn.onepick.viewholder.themepick.OnePickVoteStatus
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.PickAnimation
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.getModelFromPref
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.sqrt

@AndroidEntryPoint
class OnepickMatchActivity : BaseActivity() {

    private lateinit var mGlideRequestManager: RequestManager
    private lateinit var mTopic: OnepickTopicModel
    private lateinit var mGridLayoutManager: GridLayoutManager
    private lateinit var mMatchRecyclerView: RecyclerView
    private lateinit var mMatchAdapter: OnepickMatchAdapter
    private lateinit var mCopy: AppCompatImageView
    //private lateinit var mLoadingHeartDialogFragment: LoadingHeartDialogFragment
    private var mImageSize: Int = 0
    var date: String = ""

    private var qualifyingRoundList = SparseArrayCompat<ArrayList<OnepickIdolModel>>()
    private var finalRoundList = ArrayList<OnepickIdolModel>()
    private var roundIdolList = ArrayList<OnepickIdolModel>()
    private var totalRound: Int = 0
    private var round: Int = 0
    private var dimension: Int = DEFAULT_DIMENSION
    var sizeOfMatch: Int = SIZE_OF_A_MATCH
    private var myPicks = ArrayList<Int>()
    var isUnableSelectPhoto: Boolean = false

    private var startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                IMAGE_PICK_LIST_UPDATE_RESULT_CODE -> {
                    if (result.resultCode == IMAGE_PICK_LIST_UPDATE_RESULT_CODE) {
                        setResult(IMAGE_PICK_LIST_UPDATE_RESULT_CODE)
                        finish()
                    }
                }
            }
        }

    private lateinit var binding: ActivityOnepickMatchBinding
    @Inject
    lateinit var onepickRepository: OnepickRepositoryImpl
    @Inject
    lateinit var sharedAppState: SharedAppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_onepick_match)
        binding.clOnePickMatch.applySystemBarInsets()

        mGlideRequestManager = Glide.with(this)
        mImageSize = getImageSize()
        mTopic = intent.getSerializableExtra(PARAM_TOPIC) as OnepickTopicModel
        mMatchRecyclerView = binding.rvMatch
        mMatchAdapter = OnepickMatchAdapter(this,
                mGlideRequestManager,
                mImageSize,
                roundIdolList)
        mMatchRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return isUnableSelectPhoto or (mMatchAdapter.countLoadRequest != SIZE_OF_A_MATCH)
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
        mMatchRecyclerView.setHasFixedSize(true)
        mMatchRecyclerView.adapter = mMatchAdapter

        //mLoadingHeartDialogFragment = LoadingHeartDialogFragment.getInstance()
        setMatchTitle(PARAM_QUALIFYING_ROUND)
        loadIdols(mTopic.id)
        setBackPressed(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showAlert()
            }
        })
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    private fun getImageSize(): Int {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x / dimension
    }

    private fun showAlert() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        dialog.window!!.attributes = lpWindow
        dialog.window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)

        val msg = if(mTopic.vote == OnePickVoteStatus.ABLE.code){
            getString(R.string.onepick_confirm_exit2)
        } else{
            getString(R.string.onepick_confirm_exit)
        }

        dialog.setContentView(R.layout.dialog_new_default_two_btn)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvMsg: AppCompatTextView = dialog.findViewById(R.id.tv_msg)
        val btnConfirm: AppCompatButton = dialog.findViewById(R.id.btn_confirm)
        val btnCancel: AppCompatButton = dialog.findViewById(R.id.btn_cancel)

        tvMsg.text = msg
        btnConfirm.setOnClickListener {
            if (dialog.isShowing) {
                try {
                    setResult(OnePickMainFragment.IMAGE_PICK_LIST_UPDATE_RESULT_CODE)
                    dialog.dismiss()
                    chooseMyPick(null)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        btnCancel.setOnClickListener {
            if (dialog.isShowing) {
                try {
                    dialog.dismiss()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }

        try {
            Util.adjustIdolDialogHeight(this, dialog)
            dialog.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun showError(msg: String = getString(R.string.error_abnormal_exception)) {
        Util.showIdolDialogWithBtn1(this@OnepickMatchActivity,
                null,
                msg
        ) {
            Util.closeIdolDialog()
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setMatchTitle(round: String) {
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val customActionView: ActionBarTitleAndImageBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.action_bar_title_and_image,
            null,
            false
        )

        if (round == PARAM_FINAL_ROUND) {
            customActionView.tvActionBarTitle.text =
                    "${getString(if(BuildConfig.CELEB) R.string.actor_onepick else R.string.imagepick)} ${getString(R.string.final_round)}"
            binding.tvRound.text = getString(R.string.final_round)
            binding.tvTopic.text = mTopic.title
        } else {
            customActionView.tvActionBarTitle.text =
                    "${getString(if(BuildConfig.CELEB) R.string.actor_onepick else R.string.imagepick)} ${getString(R.string.qualifying_round)}"
            binding.tvRound.text = getString(R.string.qualifying_round)
            binding.tvTopic.text = mTopic.title
        }

        val helpInfoModel = Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
        customActionView.ivActionBarInfo.setOnClickListener {
            DefaultDialog(
                title = getString(R.string.popup_title_imagepick),
                subTitle = helpInfoModel?.onePick,
                context = this,
                theme = android.R.style.Theme_Translucent_NoTitleBar
            ).show()
        }

        supportActionBar?.customView = customActionView.root
    }

    private fun loadIdols(topicId: Int) {
        if (topicId == -1) {
            showError(getString(R.string.error_abnormal_exception))
        } else {
            Util.showProgress(this)

            MainScope().launch {
                onepickRepository.getResult(
                    topicId,
                    true,
                    { response ->
                        if (response.optBoolean("success")) {
                            Logger.v("보여줌. $response")
                            try {
                                date = response.optString("date")
                                val rankingJsonArray = response.getJSONArray("objects")

                                if(BuildConfig.CELEB) {
                                    dimension = response.optInt("onepick_dimension", DEFAULT_DIMENSION)

                                    sizeOfMatch = dimension * dimension

                                    mImageSize = getImageSize()
                                    mMatchRecyclerView = binding.rvMatch
                                    mMatchAdapter = OnepickMatchAdapter(this@OnepickMatchActivity,
                                        mGlideRequestManager,
                                        mImageSize,
                                        roundIdolList)
                                    mMatchRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                                        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                                            return isUnableSelectPhoto or (mMatchAdapter.countLoadRequest != sizeOfMatch)
                                        }

                                        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

                                        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                                    })
                                    mMatchRecyclerView.setHasFixedSize(true)
                                    mMatchRecyclerView.adapter = mMatchAdapter
                                    mGridLayoutManager = GridLayoutManager(this@OnepickMatchActivity, dimension)
                                    mMatchRecyclerView.layoutManager = mGridLayoutManager
                                }
                                val rankingSize = rankingJsonArray.length()
                                val remainder = rankingSize % sizeOfMatch

                                totalRound = if (remainder == 0) {
                                    rankingSize / sizeOfMatch
                                } else {
                                    (rankingSize / sizeOfMatch) + 1
                                }

                                if (rankingSize > 0) {
                                    val tempArrayList = ArrayList<OnepickIdolModel>()
                                    for (i in 0 until rankingSize - remainder + sizeOfMatch) {
                                        var idol: OnepickIdolModel
                                        idol = if (i < rankingSize) {
                                            val obj = rankingJsonArray.getJSONObject(i)
                                            IdolGson.getInstance(false)
                                                .fromJson(obj.toString(), OnepickIdolModel::class.java)
                                        } else {
                                            OnepickIdolModel(0, null, null, 0,
                                                if(BuildConfig.CELEB) rankingSize else totalRound * SIZE_OF_A_MATCH)
                                        }
//                                            idol.idol?.setLocalizedName(this@OnepickMatchActivity)

                                        tempArrayList.add(idol)

                                        if (i % sizeOfMatch == sizeOfMatch - 1) {
                                            qualifyingRoundList.put(i / sizeOfMatch,
                                                tempArrayList.clone() as ArrayList<OnepickIdolModel>)
                                            tempArrayList.clear()
                                        }
                                    }

                                    // for preload first and second round photos
                                    val reqImageSize = Util.getOnDemandImageSize(this@OnepickMatchActivity)

                                    qualifyingRoundList.valueAt(round).forEach {
                                        val imageUrl = UtilK.onePickImageUrl(this@OnepickMatchActivity, it.id, date, reqImageSize)

                                        mGlideRequestManager.load(imageUrl)
                                            .preload()
                                    }

                                    if (qualifyingRoundList.size() > 1) {
                                        qualifyingRoundList.valueAt(round + 1).forEach {
                                            val imageUrl = UtilK.onePickImageUrl(this@OnepickMatchActivity, it.id, date, reqImageSize)

                                            mGlideRequestManager.load(imageUrl)
                                                .preload()
                                        }
                                    }

                                    roundIdolList.addAll(qualifyingRoundList.valueAt(round))
                                    mMatchAdapter.notifyDataSetChanged()

                                    Handler().postDelayed({
                                        Util.closeProgress()
                                        addProgressDot()
                                        binding.rlPhotoPickWrapper.visibility = View.VISIBLE
                                    }, 800)
                                } else {
                                    Util.closeProgress()
                                    showError(getString(R.string.error_abnormal_exception))
                                }
                            } catch (e: Exception) {
                                Util.closeProgress()
                                showError(getString(R.string.error_abnormal_exception))
                            }
                        } else {
                            if(response.optInt("gcode") == ErrorControl.ERROR_88888 || response.optInt("gcode") == ErrorControl.ERROR_1117){
                                showError(response.getString("msg"))
                            }else{
                                showError(getString(R.string.error_abnormal_exception))
                            }
                            Util.closeProgress()
                        }
                    }, { throwable ->
                        Util.closeProgress()
                        showError(getString(R.string.error_abnormal_exception))
                    }
                )
            }
        }
    }

    fun addProgressDot() {
        val lp = LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
        if(this.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR){
            lp.setMargins(0, 0, Util.convertDpToPixel(this, 8f).toInt(), 0)
        }else{
            lp.setMargins(Util.convertDpToPixel(this, 8f).toInt(), 0, 0, 0)
        }

        for (i in 0 until totalRound) {
            val progressDot = AppCompatImageView(this)

            if (i == 0) {
                progressDot.setBackgroundResource(R.drawable.dot_active)
            } else {
                progressDot.setBackgroundResource(R.drawable.dot_inactive)
            }

            if (i != totalRound - 1) {
                progressDot.layoutParams = lp
            }

            binding.llProgressDotWrapper.addView(progressDot)
        }
    }

    fun goNextRound(view: AppCompatImageView, idol: OnepickIdolModel) {
        // 다른 사진들 안눌리게 처리
        isUnableSelectPhoto = true

        round += 1
        myPicks.add(idol.idol?.getId()!!)
        roundIdolList.clear()

        expandPhoto(view) {
            when {
                // 예선
                round < totalRound -> {

                    if (round < totalRound - 1) {
                        val nextRoundIdolList = qualifyingRoundList.valueAt(round + 1)
                        val reqImageSize = Util.getOnDemandImageSize(this@OnepickMatchActivity)

                        nextRoundIdolList.forEach {
                            val imageUrl = UtilK.onePickImageUrl(this, it.id, date, reqImageSize)

                            mGlideRequestManager.load(imageUrl)
                                    .preload()
                        }
                    }

                    finalRoundList.add(idol)
                    roundIdolList.addAll(qualifyingRoundList.valueAt(round))
                    mMatchAdapter.notifyDataSetChanged()
                    Handler().postDelayed({
                        binding.rlPhotoPickWrapper.removeView(mCopy)

                        val currentProgressDot = binding.llProgressDotWrapper.getChildAt(round) as AppCompatImageView
                        currentProgressDot.setBackgroundResource(R.drawable.dot_active)
                        isUnableSelectPhoto = false
                    }, 300)
                }
                // 결승
                round == totalRound -> {
                    Util.showLottie(this, true)
                    //mLoadingHeartDialogFragment.show(supportFragmentManager, "loading_heart_dialog")

                    finalRoundList.add(idol)
                    val finalIdolSize = finalRoundList.size
                    val finalDimension = ceil(sqrt(finalIdolSize.toDouble())).toInt()

                    roundIdolList.addAll(finalRoundList)
                    if(BuildConfig.CELEB) {
                        // dummy 넣어주기
                        for (i in finalIdolSize until finalDimension * finalDimension) {
                            roundIdolList.add(OnepickIdolModel(0, null, null, 0, 0))
                        }

                        // 결승전 올라온 수 만큼 dimension을 변경
                        mGridLayoutManager = GridLayoutManager(this@OnepickMatchActivity,
                            finalDimension)
                        mMatchRecyclerView.layoutManager = mGridLayoutManager

                        sizeOfMatch = finalDimension * finalDimension
                    }
                    mMatchAdapter.notifyDataSetChanged()

                    Handler().postDelayed({
                        binding.rlPhotoPickWrapper.removeView(mCopy)

                        Util.closeProgress()
                       // mLoadingHeartDialogFragment.dismissAllowingStateLoss()

                        setMatchTitle(PARAM_FINAL_ROUND)

                        binding.llProgressDotWrapper.removeAllViews()
                        isUnableSelectPhoto = false
                    }, 1500)
                }
                // 우승
                round > totalRound -> {
                    chooseMyPick(idol)
                }
            }
        }
    }

    private fun expandPhoto(view: AppCompatImageView, callback: () -> (Unit)) {
        // 이미지 해당 위치에 복사
        val viewPosition = IntArray(2)
        val layoutPosition = IntArray(2)

        view.getLocationOnScreen(viewPosition)
        binding.rlPhotoPickWrapper.getLocationOnScreen(layoutPosition)

        mCopy = AppCompatImageView(this)
        mCopy.setImageDrawable(view.drawable)
        mCopy.layoutParams = view.layoutParams
        mCopy.x = (viewPosition[0] - layoutPosition[0]).toFloat()
        mCopy.y = (viewPosition[1] - layoutPosition[1]).toFloat()

        binding.rlPhotoPickWrapper.addView(mCopy)
        view.visibility = View.INVISIBLE

        // https://easings.net/
        val easeInOutCubic = PathInterpolatorCompat.create(0.645f, 0.045f, 0.355f, 1f)
        val anim = PickAnimation(mCopy, binding.rlPhotoPickWrapper)

        anim.interpolator = easeInOutCubic
        anim.duration = PARAM_ANIM_TIME

        mCopy.startAnimation(anim)

        Handler().postDelayed({
            callback()
        }, PARAM_ANIM_TIME)
    }

    private fun chooseMyPick(myPick: OnepickIdolModel?) {
        Util.showProgress(this, false)

        val params = OnepickVoteDTO(
            id = mTopic.id,
            voteIds = if (myPick == null) "" else TextUtils.join(",", myPicks),
            voteType = mTopic.voteType
        )
        lifecycleScope.launch {
            onepickRepository.vote(
                id = mTopic.id,
                voteIds = if (myPick == null) "" else TextUtils.join(",", myPicks),
                voteType = mTopic.voteType,
                { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        if (myPick == null) {
                            finish()
                        } else {
                            startActivityForResultLauncher.launch(OnepickMyPickActivity.createIntent(
                                this@OnepickMatchActivity,
                                mTopic,
                                finalRoundList,
                                date,
                                myPick)
                            )
                            // 이미지픽 결과 화면 상태를 업데이트한다 (추가투표 가능/불가능 상태)
                            // 미러에서 가져오기 때문에 1초 후에 가져온다
                            MainScope().launch {
                                delay(1000)
                                sharedAppState.setRefreshImagePickResult(true)
                            }
                        }
                    } else {
                        showError(response.optString("msg"))
                    }
                }, { throwable ->
                    Util.closeProgress()
                    showError(getString(R.string.error_abnormal_exception))
                })
        }
    }

    companion object {
        const val PARAM_TOPIC = "paramTopic"
        const val PARAM_QUALIFYING_ROUND = "paramQualifyingRound"
        const val PARAM_FINAL_ROUND = "paramFinalRound"
        const val PARAM_ANIM_TIME: Long = 1000
        const val DEFAULT_DIMENSION = 3
        const val SIZE_OF_A_MATCH = 9

        @JvmStatic
        fun createIntent(context: Context, topic: OnepickTopicModel): Intent {
            val intent = Intent(context, OnepickMatchActivity::class.java)
            val args = Bundle()

            args.putSerializable(PARAM_TOPIC, topic)
            intent.putExtras(args)

            return intent
        }
    }
}