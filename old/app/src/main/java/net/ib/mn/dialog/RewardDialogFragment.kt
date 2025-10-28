package net.ib.mn.dialog

import android.os.Bundle
import android.text.SpannableString
import android.view.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import net.ib.mn.R


class RewardDialogFragment : BaseDialogFragment() {

    private lateinit var tvTitle: AppCompatTextView
    private lateinit var tvDescription: AppCompatTextView
    private lateinit var tvRewardHeart: AppCompatTextView
    private lateinit var btnOk: AppCompatButton
    private lateinit var ivReward: AppCompatImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_reward, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tv_title)
        tvDescription = view.findViewById(R.id.tv_description)
        tvRewardHeart = view.findViewById(R.id.tv_reward_heart)
        ivReward = view.findViewById(R.id.iv_reward)
        btnOk = view.findViewById(R.id.btn_ok)

        tvTitle.text = arguments?.getString(PARAM_REWARD_TITLE)
        if (arguments?.getString(PARAM_REWARD_DESCRIPTION).isNullOrEmpty()) {
            tvDescription.visibility = View.GONE
        } else {
            tvDescription.text = arguments?.getString(PARAM_REWARD_DESCRIPTION)
        }
        if (arguments?.getCharSequence(PARAM_REWARD_REWARD_MSG).isNullOrEmpty()) {
            tvRewardHeart.visibility = View.GONE
        } else {
            tvRewardHeart.text = arguments?.getCharSequence(PARAM_REWARD_REWARD_MSG)
        }
        ivReward.setImageResource(arguments?.getInt(PARAM_REWARD_IMG_ID)!!)
        btnOk.setOnClickListener {
            dialog?.dismiss()
            mBtnOkCb.invoke()
        }

        // back button 눌러서 끌 때를 위함
        dialog?.setOnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && event.action == KeyEvent.ACTION_UP
                    && !event.isCanceled) {
                dialog.dismiss()
                mBtnOkCb.invoke()

                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    companion object {
        private const val PARAM_REWARD_TITLE = "rewardTitle"
        private const val PARAM_REWARD_DESCRIPTION = "rewardDescription"
        private const val PARAM_REWARD_REWARD_MSG = "rewardRewardMsg"
        private const val PARAM_REWARD_IMG_ID = "rewardImgId"

        private var mBtnOkCb: () -> Unit = {}

        fun getInstance(title: String,
                        description: String?,
                        rewardMsg: SpannableString,
                        imgId: Int) {
            getInstance(title, description, rewardMsg, imgId) {}
        }

        fun getInstance(title: String,
                        description: String?,
                        rewardMsg: SpannableString?,
                        imgId: Int,
                        callback: () -> Unit): RewardDialogFragment {

            this.mBtnOkCb = callback

            val args = Bundle()
            val fragment = RewardDialogFragment()

            args.putString(PARAM_REWARD_TITLE, title)
            args.putString(PARAM_REWARD_DESCRIPTION, description)
            args.putCharSequence(PARAM_REWARD_REWARD_MSG, rewardMsg)
            args.putInt(PARAM_REWARD_IMG_ID, imgId)

            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            fragment.arguments = args

            return fragment
        }
    }
}