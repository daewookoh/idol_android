package net.ib.mn.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.ib.mn.R
import net.ib.mn.databinding.BottomSheetHeartPickVoteBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickVoteRewardModel
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.util.Locale

class HeartPickRewardDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetHeartPickVoteBinding

    private var reward: HeartPickVoteRewardModel? = null
    private var heartPIckIdol: HeartPickIdol? = null
    private var dismissCallBack: ((Boolean) -> Unit)? = null
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    override fun getTheme(): Int = R.style.BottomSheetDialogRewardTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        theme,
    ).apply {
        // landscape 모드에서  bottomsheet 메뉴모두  expand되지 않아서  아래 설정값 넣어줌.
        this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        isCancelable = false
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_heart_pick_vote, container, false)

        setHeartPickRewardDialog(heartPIckIdol, reward, dismissCallBack)
        return binding.root
    }


    private fun setHeartPickRewardDialog(heartPickIdol: HeartPickIdol?, reward: HeartPickVoteRewardModel?, dismiss: ((Boolean) -> Unit)?) = with(binding){
        if(heartPickIdol == null || dismiss == null || reward == null) {
            this@HeartPickRewardDialogFragment.dismiss()
            return
        }

        setContentSection(
            heartPickIdol = heartPickIdol,
            reward = reward
        )
        imgReview.bringToFront()
        setRewardSection(reward = reward)

        clConfirm.setOnClickListener {
            dismissCallBack?.invoke(true)
            this@HeartPickRewardDialogFragment.dismiss()
        }

        btnClose.setOnClickListener {
            dismissCallBack?.invoke(false)
            this@HeartPickRewardDialogFragment.dismiss()
        }
    }

    private fun setContentSection(heartPickIdol: HeartPickIdol, reward: HeartPickVoteRewardModel) =
        with(binding) {
            val title = if (heartPickIdol.subtitle.isEmpty()) {
                heartPickIdol.title
            } else {
                heartPickIdol.title.plus("_").plus(heartPickIdol.subtitle)
            }

            val content = String.format(
                getString(R.string.msg_heartpick_vote_result), numberFormat.format(
                    reward.voted
                )
            )

            val combinedString = String.format("%s\n%s", title, content)

            tvContent.text = if (reward.bonusHeart > 0) {
                tvTitle.visibility = View.GONE
                combinedString
            } else {
                tvTitle.text = title
                tvTitle.visibility = View.VISIBLE
                content
            }
        }

    private fun setRewardSection(reward: HeartPickVoteRewardModel) = with(binding.inRewardSection) {
        if (reward.bonusHeart <= 0) {
            root.visibility = View.GONE
            return@with
        }

        root.visibility = View.VISIBLE
        // 바텀 패딩이 너무 많이 들어가있어서 0으로 조정했습니다.
        // xml 수정 불가 X 다른 뷰에서 패팅이 맞춰져있음..
        clReward.setPadding(
            clReward.paddingLeft,
            clReward.paddingTop,
            clReward.paddingRight,
            Util.convertDpToPixel(context, 0f).toInt()
        )
        clReward.visibility = View.VISIBLE
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        tvReward.text = getString(R.string.vote_heart_title)
        tvHeart.text = numberFormat.format(reward.bonusHeart)
    }

    companion object {
        const val TAG = "heart_pick_reward"

        fun newInstance(heartPickIdol: HeartPickIdol?, reward: HeartPickVoteRewardModel?, dismissCallBack: ((Boolean) -> Unit)?): HeartPickRewardDialogFragment {
            return HeartPickRewardDialogFragment().apply {
                this.heartPIckIdol = heartPickIdol
                this.reward = reward
                this.dismissCallBack = dismissCallBack
            }
        }
    }
}