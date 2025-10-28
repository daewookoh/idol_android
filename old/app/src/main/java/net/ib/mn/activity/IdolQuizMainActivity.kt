package net.ib.mn.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.databinding.ActivityQuizMainBinding
import net.ib.mn.dialog.QuizReviewDenyDialogFragment
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.model.QuizCategoryModel
import net.ib.mn.model.QuizReviewModel
import net.ib.mn.core.data.model.QuizTodayModel
import net.ib.mn.quiz.IdolQuizViewModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver

@AndroidEntryPoint
class IdolQuizMainActivity :
    BaseActivity(),
    View.OnClickListener {

    private lateinit var binding: ActivityQuizMainBinding

    private var mIdolId: Int = 0
    private var mType: String? = null
    private var modelList: ArrayList<IdolModel> = ArrayList()
    private var quizTypeList: ArrayList<QuizCategoryModel> = ArrayList()
    private var quizTodayModel: QuizTodayModel? = null
    private var mostId: Int = 0

    private val idolQuizViewModel: IdolQuizViewModel by viewModels()

    private val startActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                QUIZ_REVIEW_RESULT_CODE -> {    //퀴즈 심사하기에서 퀴즈 심사했을 경우
                    FLAG_CLOSE_DIALOG = false
                    Util.showDefaultIdolDialogWithBtn1(
                        this,
                        null,
                        if (BuildConfig.CELEB) getString(R.string.actor_quiz_review_thankyou) else getString(
                            R.string.quiz_review_thankyou
                        ),
                    ) { v ->
                        Util.closeIdolDialog()
                    }
                }

                QUIZ_SOLVE_RESULT_CODE -> {
                    val quizRewardHeart = result.data?.getIntExtra(QUIZ_REWARD_HEART, 0)
                    if (quizRewardHeart != null && quizRewardHeart > 0) {
                        setRewardBottomSheetFragment(quizRewardHeart, true)
                    }
                    initQuiz()
                }
            }
        }

    private fun initQuiz() {
        idolQuizViewModel.getQuizMyOwn(this)
        idolQuizViewModel.getQuizToday(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz_main)
        binding.clContainer.applySystemBarInsets()
        binding.tvQuizStart.bringToFront()

        // 앵커 뷰에 하단 inset 반영 (시스템바/IME 모두)
        ViewCompat.setOnApplyWindowInsetsListener(binding.snackbarAnchor) { v, insets ->
            val bottom = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            ).bottom
            v.layoutParams = v.layoutParams.apply { height = bottom }
            insets
        }

        val actionbar = supportActionBar
        if (BuildConfig.CELEB) {
            actionbar?.setTitle(R.string.actor_menu_quiz)
            binding.tvIdol.setTextColor(ContextCompat.getColor(this, R.color.celeb_quiz_category))
            idolQuizViewModel.getQuizTypeList(this)
            binding.tvTop.text = getString(R.string.quiz_main_title2_celeb)
            binding.tvDown.text = getString(R.string.actor_menu_quiz)
        } else {
            actionbar?.setTitle(R.string.menu_quiz)
            binding.tvIdol.setTextColor(ContextCompat.getColor(this, R.color.idol_quiz_category))
            idolQuizViewModel.getIdolGroupList(this, IdolAccount.getAccount(this))
        }

        mIdolId = 0
        mostId = IdolAccount.getAccount(this)?.most?.getId() ?: 0

        getDataFromVM()
        initQuiz()
    }

    private fun getDataFromVM() = with(idolQuizViewModel) {

        showAdoptedMyNewQuiz.observe(
            this@IdolQuizMainActivity,
            SingleEventObserver { showAdoptedMyNewQuiz ->
                binding.newQuizReward.visibility = if (showAdoptedMyNewQuiz) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        )

        errorToast.observe(this@IdolQuizMainActivity, SingleEventObserver { errorMessage ->
            IdolSnackBar.make(findViewById(android.R.id.content), errorMessage).show()
        })

        getQuizTodayModel.observe(this@IdolQuizMainActivity) { quizTodayModel ->

            this@IdolQuizMainActivity.quizTodayModel = quizTodayModel

            binding.quizNotice.text = if (quizTodayModel?.motd.isNullOrEmpty()) {
                ""
            } else {
                quizTodayModel?.motd
            }

            binding.quizWriteText.text = String.format("%s${quizTodayModel?.quizPoint}", "♡")

            if (quizTodayModel?.incompleteSession?.autoReward == true) {
                quizTodayModel.incompleteSession?.heart?.let {
                    setRewardBottomSheetFragment(it, false)
                }
            }

            showQuizToolTip(quizTodayModel?.tooltip)
            setClickListener()
        }

        finishActivity.observe(this@IdolQuizMainActivity, SingleEventObserver { isFinish ->
            if (!isFinish) {
                return@SingleEventObserver
            }

            Util.closeProgress()
            this@IdolQuizMainActivity.finish()
        })

        goQuizReviewActivity.observe(
            this@IdolQuizMainActivity,
            SingleEventObserver { mapOfReviewData ->

                Util.closeProgress()

                var quizzes = 0
                var reviewModels: ArrayList<QuizReviewModel> = arrayListOf()

                for (entry in mapOfReviewData.entries) {
                    quizzes = entry.key
                    reviewModels = entry.value
                }

                // 심사할 퀴즈 없으면 심사할 퀴즈 없다는 다이얼로그 나오게
                if (reviewModels.size == 0) {
                    QuizReviewDenyDialogFragment.getInstance(QUIZ_REVIEW_NOT_EXIST)
                        .show(supportFragmentManager, "idol_review_dialog")
                    return@SingleEventObserver
                }

                val intent = IdolQuizReviewActivity.createIntent(
                    this@IdolQuizMainActivity,
                    reviewModels,
                    0,
                    mostId,
                    1,
                    quizzes,
                    0,
                    0
                )

                startActivityResultLauncher.launch(intent)
            })

        getQuizTypeList.observe(this@IdolQuizMainActivity, SingleEventObserver { quizTypeList ->
            this@IdolQuizMainActivity.quizTypeList = quizTypeList
        })

        getIdolModelList.observe(this@IdolQuizMainActivity, SingleEventObserver { idolModelList ->
            this@IdolQuizMainActivity.modelList = idolModelList
        })

        successOfQuizPlus.observe(
            this@IdolQuizMainActivity,
            SingleEventObserver { isSuccessOfQuizPlus ->
                if (!isSuccessOfQuizPlus) {
                    return@SingleEventObserver
                }

                val intent = IdolQuizSolveActivity.createIntent(
                    this@IdolQuizMainActivity,
                    mIdolId,
                    quizTodayModel,
                    mType,
                    binding.tvIdol.text.toString(),
                    true,
                )
                startActivityResultLauncher.launch(
                    intent,
                )
            })
    }

    override fun onResume() {
        super.onResume()
        FLAG_CLOSE_DIALOG = false
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Util.removePreference(this, Const.PREF_QUIZ_IDOL_LIST)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.quiz_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btn_quiz_ranking -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "menu_quiz_ranking",
                )
                startActivity(IdolQuizRankingActivity.createIntent(this))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnQuizStart.id -> with(quizTodayModel ?: return) {

                when(quizTodayModel?.status) {
                    STATUS_RETRY -> {
                        showRewardAdDialog()
                    }
                    STATUS_CLOSE -> {
                        Util.showDefaultIdolDialogWithBtn1(
                            this@IdolQuizMainActivity,
                            null,
                            getString(R.string.quiz_unavailable_done),
                        ) { Util.closeIdolDialog() }
                    }
                    STATUS_OPEN -> {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            Const.ANALYTICS_BUTTON_PRESS_ACTION,
                            "idol_quiz_start",
                        )
                        val intent = IdolQuizSolveActivity.createIntent(
                            this@IdolQuizMainActivity,
                            mIdolId,
                            quizTodayModel,
                            mType,
                            binding.tvIdol.text.toString()
                        )
                        startActivityResultLauncher.launch(
                            intent,
                        )
                    }
                    else -> {
                        Util.showDefaultIdolDialogWithBtn1(
                            this@IdolQuizMainActivity,
                            null,
                            getString(R.string.error_abnormal_default),
                        ) {
                            Util.closeIdolDialog()
                            finish()
                        }
                    }
                }
            }

            binding.selectIdol.id -> showBottomSheet()
            binding.llQuizWrite.id -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.QUIZ_WRITE.actionValue,
                    GaAction.QUIZ_WRITE.label,
                )
                if (quizTodayModel!=null && quizTodayModel!!.dailyQuizPostLimit > quizTodayModel!!.dailyPostedQuizzes) {
                    showDialogWriteWarning(this)
                } else {
                    Util.showDefaultIdolDialogWithBtn1(
                        this,
                        null,
                        getString(R.string.quiz_daily_posting_limit),
                    ) { Util.closeIdolDialog() }
                }
            }

            binding.llQuizInfo.id -> startActivity(IdolQuizInfoActivity.createIntent(this))
            binding.llQuizExamine.id -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.QUIZ_REVIEW.actionValue,
                    GaAction.QUIZ_REVIEW.label,
                )
                if (quizTodayModel != null && quizTodayModel!!.reviewCount >= quizTodayModel!!.reviewLimit) {
                    QuizReviewDenyDialogFragment.getInstance(QUIZ_REVIEW_MAX_OVER)
                        .show(supportFragmentManager, "idol_review_dialog")
                } else {
                    Util.showProgress(this)
                    idolQuizViewModel.getQuizReview(mostId, this)
                }
            }
        }
    }

    private fun showDialogWriteWarning(context: Context) {
        val idolDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        idolDialog.window?.attributes = lpWindow
        idolDialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )

        idolDialog.setContentView(R.layout.dialog_write)
        idolDialog.setCanceledOnTouchOutside(true)
        idolDialog.setCancelable(true)
        val btnConfirm = idolDialog.findViewById<TextView>(R.id.btn_confirm)

        btnConfirm.setOnClickListener {
            idolDialog.cancel()
            startActivity(IdolQuizWriteActivity.createIntent(context, quizTypeList))
        }

        try {
            idolDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            idolDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onCategorySelected(position: Int) {
        if (position == 0) {
            selectAll()
        } else {
            selectItem(position)
        }
    }

    private fun selectAll() {
        binding.tvIdol.text = getString(R.string.quiz_idol_all)
        mIdolId = 0
        mType = null
    }

    private fun selectItem(index: Int) {
        if (BuildConfig.CELEB) {
            if (index < 0 || index >= quizTypeList.size) return
            val selected = quizTypeList[index]
            binding.tvIdol.text = selected.name
            mType = selected.type
            mIdolId = 0
        } else {
            if (index - 1 < 0 || index - 1 >= modelList.size) return
            val selected = modelList[index - 1]
            binding.tvIdol.text = selected.getName(this)
            mIdolId = selected.getId()
            mType = null
        }
    }

    private fun showBottomSheet() {
        val items = if (BuildConfig.CELEB) {
            arrayListOf<String>().apply { addAll(quizTypeList.map { it.name }) }
        } else {
            arrayListOf(getString(R.string.quiz_idol_all)).apply { addAll(modelList.map { it.getName(this@IdolQuizMainActivity) }) }
        }

        val bottomSheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_QUIZ_MAIN_FILTER)
        val args = Bundle()
        args.putStringArrayList("items", items)
        bottomSheet.arguments = args
        bottomSheet.show(supportFragmentManager, "quiz_main_filter")
    }

    private fun setRewardBottomSheetFragment(bonusHeart: Int, didReceive: Boolean){
        val mBottomSheetDialogFragment = RewardBottomSheetDialogFragment.newInstance(RewardBottomSheetDialogFragment.FLAG_QUIZ_REWARD, bonusHeart, didReceive)

        val tag = "reward_quiz_main"
        val oldTag: Fragment? = supportFragmentManager.findFragmentByTag(tag)
        if(oldTag == null){
            mBottomSheetDialogFragment.show(supportFragmentManager, tag)
        }
    }

    private fun setClickListener() = with(binding) {
        btnQuizStart.setOnClickListener(this@IdolQuizMainActivity)
        selectIdol.setOnClickListener(this@IdolQuizMainActivity)
        llQuizExamine.setOnClickListener(this@IdolQuizMainActivity)
        llQuizWrite.setOnClickListener(this@IdolQuizMainActivity)
        llQuizInfo.setOnClickListener(this@IdolQuizMainActivity)
    }

    private fun showQuizToolTip(tooltip: String?) = with(binding.inTooltipDown) {
        if (tooltip.isNullOrEmpty()) {
            root.visibility = View.GONE
            return@with
        }

        if (BuildConfig.CELEB) {
            ivTooltipDown.setImageDrawable(
                ContextCompat.getDrawable(
                    this@IdolQuizMainActivity,
                    R.drawable.bg_guide_celeb_down
                )
            )
            tvTooltipDown.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@IdolQuizMainActivity,
                    R.drawable.bg_radius_quiz_main_celeb_tooltip
                )
            )
        } else {
            ivTooltipDown.setImageDrawable(
                ContextCompat.getDrawable(
                    this@IdolQuizMainActivity,
                    R.drawable.bg_guide_down
                )
            )
            tvTooltipDown.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this@IdolQuizMainActivity,
                    R.drawable.bg_radius_quiz_main_tooltip
                )
            )
        }
        tvTooltipDown.text = tooltip
        root.visibility = View.VISIBLE
    }

    private fun showRewardAdDialog() {
        val rewardAdDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        rewardAdDialog.window!!.attributes = lpWindow
        rewardAdDialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )

        rewardAdDialog.setContentView(R.layout.dialog_reward_ad)
        rewardAdDialog.setCanceledOnTouchOutside(false)
        rewardAdDialog.setCancelable(false)

        // view binding 적용시 버튼 레이아웃이 깨지는 현상이 있어서 보류
        val tvTitle: AppCompatTextView = rewardAdDialog.findViewById(R.id.tv_title)
        val tvMsg: AppCompatTextView = rewardAdDialog.findViewById(R.id.tv_msg)
        val btnCancel: AppCompatButton = rewardAdDialog.findViewById(R.id.btn_cancel)
        val btnOK = rewardAdDialog.findViewById<AppCompatButton>(R.id.btn_ok)

        tvTitle.text = getString(R.string.retry_quiz)
        tvMsg.text = getString(R.string.video_ad_chance_solve_quiz)

        btnCancel.setOnClickListener {
            rewardAdDialog.dismiss()
        }
        btnOK.setOnClickListener {
            rewardAdDialog.dismiss()
            startActivityForResult(
                MezzoPlayerActivity.createIntent(this, Const.ADMOB_REWARDED_VIDEO_QUIZ_UNIT_ID),
                MEZZO_PLAYER_REQ_CODE
            )
        }

        rewardAdDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        rewardAdDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MEZZO_PLAYER_REQ_CODE -> {
                if (resultCode == RESULT_CANCELED) {
                    Util.handleVideoAdResult(
                        this, true, true, requestCode, resultCode, data, ""
                    ) {}
                    return
                }
                idolQuizViewModel.requestQuizPlus(this)
            }
        }
    }

    companion object {
        const val QUIZ_REVIEW_RESULT_CODE = 888
        const val QUIZ_SOLVE_RESULT_CODE = 999
        const val QUIZ_REVIEW_MAX_OVER = "quiz_review_max_over"
        const val QUIZ_REVIEW_NOT_EXIST = "quiz_review_not_exist"
        const val QUIZ_REWARD_HEART = "quizRewardHeart"

        const val STATUS_CLOSE = "F"
        const val STATUS_RETRY = "R"
        const val STATUS_OPEN = ""

        fun createIntent(context: Context): Intent {
            return Intent(context, IdolQuizMainActivity::class.java)
        }
    }
}
