package net.ib.mn.dialog

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.databinding.DialogVoteBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HeartPickVoteDialogViewModel
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class HeartPickVoteDialogFragment : BaseDialogFragment() {
    private lateinit var binding: DialogVoteBinding

    private val heartPickVoteDialogViewModel: HeartPickVoteDialogViewModel by viewModels()

    private var dismissListener: ((Map<String, Any?>?) -> Unit)? = null

    private var heartPickIdol : HeartPickIdol? = null
    private var totalHeart: Long = 0
    private var freeHeart: Long = 0
    private var heartPickId: Int = 0

    fun setOnDialogDismissListener(listener: (Map<String, Any?>?) -> Unit) {
        this.dismissListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogVoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        getDataFromVM()
        onClickListener()
    }

    private fun init() = with(binding) {
        if (arguments != null) {
            heartPickIdol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getSerializable(PARAM_HEART_PICK_IDOL, HeartPickIdol::class.java)
            } else {
                arguments?.getSerializable(PARAM_HEART_PICK_IDOL) as HeartPickIdol
            }
            totalHeart = arguments?.getLong(TOTAL_HEART, 0)?:0
            freeHeart = arguments?.getLong(FREE_HEART, 0)?:0
            heartPickId = arguments?.getInt(HEART_PICK_ID, 0)?:0
        }

        if(context != null) {
            tvSolo.text = heartPickIdol?.title
            tvGroup.text = heartPickIdol?.subtitle

            heartCountWeakHeart.text = getCommaNumber(freeHeart)
            heartCountEverheart.text = getCommaNumber(totalHeart - freeHeart)
            heartCountMyheart.text = getCommaNumber(totalHeart)
        }

        tvSolo.post {

            if (tvSolo.lineCount <= 1) {
                return@post
            }

            val constraintSet = ConstraintSet().apply {
                clone(binding.clTitle)

                connect(
                    binding.tvSolo.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )

                connect(
                    binding.tvGroup.id,
                    ConstraintSet.TOP,
                    binding.tvSolo.id,
                    ConstraintSet.BOTTOM
                )
                connect(
                    binding.tvGroup.id,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )

                connect(
                    binding.tvGroup.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )

                connect(
                    binding.tvGroup.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )

            }

            constraintSet.applyTo(binding.clTitle)
        }
    }

    private fun getDataFromVM() {
        heartPickVoteDialogViewModel.voteHeart.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                Util.closeProgress()
                dismissListener?.invoke(it)
                dismiss()
            })

        heartPickVoteDialogViewModel.errorToast.observe(
            viewLifecycleOwner,
            SingleEventObserver { msg ->
                binding.btnConfirm.isEnabled = true
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun onClickListener() = with(binding) {
        btnConfirm.setOnClickListener {
            if(binding.heartCount.text.toString().isEmpty()) {
                return@setOnClickListener
            }
            if(binding.heartCount.text.toString().toInt() > totalHeart) {
                Util.closeProgress()
                Toast.makeText(activity, getString(R.string.not_enough_heart), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.heartCount.text.toString().toInt() == 0) {
                Util.closeProgress()
                dismiss()
                return@setOnClickListener
            }

            btnConfirm.isEnabled = false
            heartPickVoteDialogViewModel.voteHeartPick(lifecycleScope, context, heartPickId, heartPickIdol, binding.heartCount.text.toString().toInt())
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        clHeart1.setOnClickListener {
            addHeart(1L)
        }

        clHeart10.setOnClickListener {
            addHeart(10L)
        }

        clHeart50.setOnClickListener {
            addHeart(50L)
        }

        clHeart100.setOnClickListener {
            addHeart(100L)
        }

        clHeartAll.setOnClickListener {
            Util.showDefaultIdolDialogWithBtn2(
                activity,
                null,
                resources.getString(R.string.are_you_sure),
                {
                    heartCount.setText(totalHeart.toString())
                    Util.closeIdolDialog()
                }
            ) { Util.closeIdolDialog() }
        }

        clHeartFreeAll.setOnClickListener {
            heartCount.setText(freeHeart.toString())
        }
    }

    private fun addHeart(count: Long) {
        val a: CharSequence = binding.heartCount.text.toString()
        val heart: Int = if (!TextUtils.isEmpty(a)) {
            a.toString().toInt()
        } else {
            0
        }
        if (totalHeart >= count + heart) {
            binding.heartCount.setText(count.plus(heart).toString())
        }
    }

    private fun getCommaNumber(count: Long): String? {
        val format = NumberFormat.getNumberInstance(Locale.US)
        return format.format(count)
    }

    companion object{
        private const val PARAM_HEART_PICK_IDOL = "idol"
        private const val TOTAL_HEART = "total_heart"
        private const val FREE_HEART = "free_heart"
        private const val HEART_PICK_ID = "heart_pick_id"

        fun getInstance(heartPickIdol: HeartPickIdol?, totalHeart: Long, freeHeart: Long, heartPickId: Int) : HeartPickVoteDialogFragment{
            val args = Bundle()
            val fragment = HeartPickVoteDialogFragment()
            args.putSerializable(PARAM_HEART_PICK_IDOL, heartPickIdol)
            args.putLong(TOTAL_HEART, totalHeart)
            args.putLong(FREE_HEART, freeHeart)
            args.putInt(HEART_PICK_ID, heartPickId)
            fragment.arguments = args

            return fragment
        }
    }
}