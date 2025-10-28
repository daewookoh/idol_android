package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.adapter.HeartPickAdapter
import net.ib.mn.core.model.HelpInfosModel
import net.ib.mn.databinding.ActionBarTitleAndImageBinding
import net.ib.mn.databinding.ActivityHeartPickBinding
import net.ib.mn.dialog.DefaultDialog
import net.ib.mn.fragment.HeartPickRewardDialogFragment
import net.ib.mn.utils.vote.VotePercentage
import net.ib.mn.heartpick.VotingStatus
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.model.HeartPickVoteRewardModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.getModelFromPref
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HeartPickViewModel
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class HeartPickActivity : BaseActivity(), HeartPickAdapter.HeartPickListener, HeartPickAdapter.CommentClickListener {

    private lateinit var binding: ActivityHeartPickBinding
    private lateinit var heartPickAdapter: HeartPickAdapter
    private lateinit var mGlideRequestManager: RequestManager
    private var timerJob1st: Job? = null
    private var timerJob2end3rd: Job? = null
    private var isTimerRunning: Boolean = false
    private var isTooltipVisible: Boolean = false   // 나의 최애 툴팁 보여줬는지 여부
    private var id: Int = 0
    private val heartPickViewModel: HeartPickViewModel by viewModels()
    @Inject
    lateinit var accountManager: IdolAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_heart_pick)
        binding.root.applySystemBarInsets()

        init()
    }

    private fun init() {
        id = (intent.getIntExtra(PARAM_ID, 0))
        mGlideRequestManager = Glide.with(this)
        heartPickViewModel.timerJob?.cancel()

        getDataFromVM()
        getHeartPick(id)
        setAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob1st?.cancel()
        timerJob2end3rd?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.btn_share -> {
                heartPickViewModel.getHeartPick(this, id, true)
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.HEARTPICK_SHARE.actionValue,
                    GaAction.HEARTPICK_SHARE.label,
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getDataFromVM() {
        heartPickViewModel.registerActivityResult(this)

        heartPickViewModel.getHeartPick.observe(
            this,
            SingleEventObserver { heartPickModel ->
                heartPickViewModel.setDDay(this, lifecycleScope, heartPickModel)
                setLastPlace(heartPickModel) { lastPlaceVotePercent ->
                    // 마지막 등수의 가로 비율은 프로그래스바 길이의 최소값이 됩니다.
                    heartPickModel.minPercent = lastPlaceVotePercent
                    setMostIdol(heartPickModel)
                    setActionBar(heartPickModel)
                    heartPickAdapter.setHeartPickModel(heartPickModel)
                    setData(heartPickModel)
                }
            })

        heartPickViewModel.voteCallBack.observe(
            this,
            SingleEventObserver {
                val intent = Intent()
                intent.putExtra("heartPickId", id)
                setResult(ResultCode.VOTED.value, intent)

                val heartPickModel = it["heartPickIdol"] as HeartPickIdol?
                val reward = it["reward"] as HeartPickVoteRewardModel?

                val dialogFragment =
                    HeartPickRewardDialogFragment.newInstance(heartPickModel, reward) { isShare ->
                        if (isShare) {
                            setUiActionFirebaseGoogleAnalyticsActivity(
                                GaAction.HEARTPICK_VOTE_SHARE.actionValue,
                                GaAction.HEARTPICK_VOTE_SHARE.label,
                            )
                        }
                        // 공유 링크 넣으면 됨
                        heartPickViewModel.getHeartPick(this, id, isShare)
                }
                dialogFragment.show(supportFragmentManager, HeartPickRewardDialogFragment.TAG)

                UtilK.checkLevelUp(
                    this,
                    accountManager,
                    IdolModel(
                        id = heartPickModel?.idol_id ?: return@SingleEventObserver,
                        groupId = if (BuildConfig.CELEB) 0 else heartPickModel.groupId
                    ),
                    reward?.voted ?: return@SingleEventObserver
                )
            }
        )

        heartPickViewModel.dDayText.observe(
            this,
            SingleEventObserver {
                binding.tvDDay.text = it
            }
        )

        heartPickViewModel.isCommented.observe(
            this,
            SingleEventObserver {
                val intent = Intent()
                intent.putExtra("heartPickId", id)
                setResult(ResultCode.COMMENTED.value, intent)
            }
        )

        heartPickViewModel.idol.observe(this, SingleEventObserver {
            startActivity(
                CommunityActivity.createIntent(
                    this@HeartPickActivity,
                    it
                )
            )
        })

        heartPickViewModel.isPrelaunch.observe(this, SingleEventObserver {
            val intent = Intent(this, HeartPickPrelaunchActivity::class.java)
            intent.putExtra(HeartPickPrelaunchActivity.EXTRA_HEART_PICK_MODEL, it)
            startActivity(intent)
            finish()
        })

        heartPickViewModel.isLoading.observe(this, SingleEventObserver {
            binding.loadingProgressBar.visibility = View.GONE
            binding.clContainer.visibility = View.VISIBLE
        })
    }

    private fun setAdapter() {
        heartPickAdapter = HeartPickAdapter(null, this, this, lifecycleScope)
        binding.rvHeartPick.adapter = heartPickAdapter
    }

    private fun getHeartPick(id: Int) {
        heartPickViewModel.getHeartPick(this, id)
    }

    private fun setData(heartPickModel: HeartPickModel) = with(binding) {
        mGlideRequestManager.load(heartPickModel.bannerUrl)
            .into(ivBanner)

        tvTitle.text = heartPickModel.title
        tvSubTitle.text = heartPickModel.subtitle
        binding.tvDataLoad.visibility = View.GONE
    }

    private fun setMostIdol(heartPickModel: HeartPickModel) = with(binding) {
        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@HeartPickActivity))
        val mostIdol = IdolAccount.getAccount(this@HeartPickActivity)?.userModel?.most
        val heartPickIdol:HeartPickIdol? = heartPickModel.heartPickIdols?.find { it.idol_id == mostIdol?.getId() }
        val position: Int? = heartPickModel.heartPickIdols?.indexOf(heartPickIdol)

        if (heartPickModel.status == VotingStatus.VOTE_FINISHED.status) {
            clShadow.visibility = View.VISIBLE
            clShadow.bringToFront()
            clDDay.bringToFront()
        }

        if(heartPickIdol == null || position == 0) {
            clToolTip.visibility = View.GONE
            clMyIdolRoot.visibility = View.GONE
        } else {
            clMyIdolRoot.visibility = View.VISIBLE
            inHeartPickRank.root.setBackgroundColor(ContextCompat.getColor(this@HeartPickActivity, R.color.background_100))

            if(heartPickModel.status == VotingStatus.VOTE_FINISHED.status) {
                clToolTip.visibility = View.GONE
                inHeartPickRank.btnHeart.visibility = View.GONE
                clMyIdol.setPadding(0,0,Util.convertDpToPixel(this@HeartPickActivity, 20f).toInt(),0)
            } else {
                if(!isTooltipVisible){
                    clToolTip.visibility = View.VISIBLE
                    timerJob1st = lifecycleScope.launch(Dispatchers.Main) {
                        delay(3000)
                        clToolTip.visibility = View.GONE
                        isTooltipVisible = true
                    }
                } else {
                    clToolTip.visibility = View.GONE
                }
                inHeartPickRank.btnHeart.visibility = View.VISIBLE
            }

            tvTooltipDown.text = String.format(getString(R.string.heartpick_tooltip_msg), numberFormat.format(heartPickIdol.rank.minus(1)), numberFormat.format(heartPickIdol.diffVote))

            inHeartPickRank.apply {
                tvRank.text = heartPickIdol.rank.toString()
                inRankingPickNameAndGroup.setRecompose(heartPickModel.type != "I")
                inRankingPickNameAndGroup.setNameAndGroupForNoIdol(
                    idolName = heartPickIdol.title,
                    idolGroup = heartPickIdol.subtitle,
                    nameMaxLine = 1,
                    groupMaxLine = 1
                )


                inGradientProgressBar.tvVote.text = numberFormat.format(heartPickIdol.vote)
                if(heartPickModel.vote == 0) {
                    inGradientProgressBar.tvVotePercent.text = numberFormat.format(0).plus("%")
                } else {
                    val votePercent: Float = 100.0f * heartPickIdol.vote.toFloat() / heartPickModel.vote.toFloat()    // 분모가 0일 경우 앱이 죽기때문에 위에서 예외 처리
                    inGradientProgressBar.tvVotePercent.text = numberFormat.format(votePercent.roundToInt()).plus("%")
                }
                btnHeart.setOnSingleClickListener {
                    heartPickViewModel.confirmUse(this@HeartPickActivity, heartPickIdol, heartPickModel.id)
                }
                mGlideRequestManager.load(heartPickIdol.image_url)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(heartPickIdol.idol_id))
                    .fallback(Util.noProfileImage(heartPickIdol.idol_id))
                    .placeholder(Util.noProfileImage(heartPickIdol.idol_id))
                    .dontAnimate()
                    .into(ivPhoto)

                val progressBarPercent = VotePercentage.getVotePercentage(
                    minPercentage = heartPickModel.minPercent.toInt(),
                    firstPlaceVote = heartPickModel.firstPlaceVote.toLong(),
                    currentPlaceVote = heartPickIdol.vote.toLong(),
                    lastPlaceVote = heartPickModel.lastPlaceVote.toLong()
                )
                inGradientProgressBar.progressBar.setWidthRatio(percent = progressBarPercent, isApply = true)
            }

        }

    }

    private fun setActionBar(heartPickModel: HeartPickModel) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val customActionView: ActionBarTitleAndImageBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.action_bar_title_and_image,
            null,
            false
        )

        customActionView.tvActionBarTitle.text =
            if (heartPickModel.status == VotingStatus.VOTE_FINISHED.status) getString(R.string.result_heartpick) else getString(
                R.string.heartpick
            )

        val helpInfoModel = Util.getPreference(this, Const.PREF_HELP_INFO).getModelFromPref<HelpInfosModel>()
        customActionView.ivActionBarInfo.setOnClickListener {
            DefaultDialog(
                title = getString(R.string.popup_title_heartpick),
                subTitle = helpInfoModel?.heartPick,
                context = this,
                theme = android.R.style.Theme_Translucent_NoTitleBar
            ).show()
        }
        supportActionBar?.customView = customActionView.root
    }

    companion object {

        private const val PARAM_ID = "id"

        fun createIntent(context: Context, id: Int): Intent {
            val intent = Intent(context, HeartPickActivity::class.java)
            intent.putExtra(PARAM_ID, id)
            return intent
        }
    }

    override fun onVote(heartPickIdol: HeartPickIdol?, heartPickId: Int) {
        heartPickViewModel.confirmUse(this, heartPickIdol, heartPickId)
    }

    // viewholder에서 타이머 처리하면 toolbar가 투표했을 때 다시 보이는 문제가 있어서 뺐음
    override fun onTimer(view: ConstraintLayout, position: Int) {
        if(isTimerRunning) {
            view.visibility = View.INVISIBLE
            heartPickViewModel.updateHasGoneToolTip(hasGoneToolTip = true)
            return
        }
        timerJob2end3rd = lifecycleScope.launch(Dispatchers.Main) {
            delay(3000)
            view.visibility = View.INVISIBLE
            heartPickViewModel.updateHasGoneToolTip(hasGoneToolTip = true)
            if(position == 1) {
                isTimerRunning = true
            }
        }
    }

    override fun onComment(id: Int) {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.HEARTPICK_COMMENT.actionValue,
            GaAction.HEARTPICK_COMMENT.label,
        )
        heartPickViewModel.startActivityResultLauncher.launch(
            CommentOnlyActivity.createIntent(this, id)
        )
    }

    override fun goCommunity(heartPickModel: HeartPickIdol) {
        heartPickViewModel.getIdolById(heartPickModel.idol_id)
    }

    private fun setLastPlace(heartPickModel: HeartPickModel, successViewLayout: (Float) -> Unit) =
        with(binding.inHeartPickLastPlaceRank.inGradientProgressBar) {
            progressBar.updateIsLastPlace(true)

            // 마지막 등수의 텍스트를 넣고.
            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@HeartPickActivity))
            tvVote.text = numberFormat.format(heartPickModel.lastPlaceVote)

            // 화면에 그려지면 가로 비율을 가져와줍니다.
            progressBar.setOnLayoutCompleteListener { percentWidth ->
                successViewLayout(percentWidth)
            }
        }
}