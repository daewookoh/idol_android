package net.ib.mn.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.databinding.BottomSheetVoteBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.LocaleUtil

@AndroidEntryPoint
class VoteBottomSheetFragment(
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetVoteBinding

    private lateinit var idol: IdolModel
    private var voteCount: Long = 0
    private lateinit var bonusHeart: String
    private var onClickConfirm: (idol: IdolModel) -> Unit = {}
    private var onClickDismiss: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            idol = it.getSerializable(ARG_IDOL) as IdolModel
            voteCount = it.getLong(ARG_VOTE_COUNT)
            bonusHeart = it.getString(ARG_BONUS_HEART) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetVoteBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@VoteBottomSheetFragment
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val voteString = voteCount.toString()
        binding.tvHeart.text = bonusHeart
        binding.tvSubtitle.text = String.format(
            LocaleUtil.getAppLocale(requireContext()),
            getString(R.string.response_v1_articles_give_heart),
            voteString,
            idol.getName(requireContext()),
            idol.heart.toString(),
            voteString
        )

        binding.btnClose.setOnClickListener {
            onClickDismiss()
            dismiss()
        }

        binding.tvConfirm.setOnClickListener {
            onClickConfirm(idol)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClickDismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(
        requireContext(),
        R.style.BottomSheetDialogRewardTheme,
    ).apply {
        // TODO 차후 드래그 막을거면 사용
//        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//        behavior.isDraggable = false
    }

    companion object {
        private const val ARG_IDOL = "idol"
        private const val ARG_VOTE_COUNT = "vote_count"
        private const val ARG_BONUS_HEART = "bonus_heart"

        fun newInstance(
            idol: IdolModel,
            voteCount: Long,
            bonusHeart: String,
            onClickConfirm: (idol: IdolModel) -> Unit = {},
            onClickDismiss: () -> Unit = {}
        ): VoteBottomSheetFragment {
            val fragment = VoteBottomSheetFragment()
            fragment.onClickConfirm = onClickConfirm
            fragment.onClickDismiss = onClickDismiss
            val args = Bundle().apply {
                putSerializable(ARG_IDOL, idol)
                putLong(ARG_VOTE_COUNT, voteCount)
                putString(ARG_BONUS_HEART, bonusHeart)
            }
            fragment.arguments = args
            return fragment
        }
    }
}