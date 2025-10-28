package net.ib.mn.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.DialogItem3ConfirmBinding
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.BurningDayPurchaseDialogViewModel
import net.ib.mn.viewmodel.CommunityActivityViewModel
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class BurningDayPurchaseDialogFragment : BaseDialogFragment() {
    private lateinit var binding: DialogItem3ConfirmBinding

    private val communityActivityViewModel: CommunityActivityViewModel by activityViewModels()
    private val burningDayPurchaseDialogViewModel: BurningDayPurchaseDialogViewModel by viewModels()
    private lateinit var mGlideRequestManager: RequestManager

    override fun onStart() {
        super.onStart()

        dialog ?: return

        dialog?.window?.apply {
            val lpWindow = attributes.apply {
                flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                dimAmount = 0.7f
                gravity = Gravity.CENTER
            }

            attributes = lpWindow
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog?.apply {
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        binding.btnCancel.setOnClickListener { dialog?.cancel() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_item3_confirm, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mGlideRequestManager = Glide.with(this)

        init()

    }

    private fun init() {
        getDataFromVM()

        if (BuildConfig.CELEB) {
            binding.textGuide1.text = getString(R.string.actor_item_guide_3_1)
        }
        // config/self에서 필요레벨 받아오기
        Util.showProgress(context)
        burningDayPurchaseDialogViewModel.getItemShopList(context)
    }

    private fun getDataFromVM() {
        burningDayPurchaseDialogViewModel.setItemLevel(context)

        burningDayPurchaseDialogViewModel.callConfigSelf.observe(
            viewLifecycleOwner,
            SingleEventObserver{
                burningDayPurchaseDialogViewModel.getConfigSelf(context)
            }
        )

        burningDayPurchaseDialogViewModel.daysList.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                binding.textGuide1.text = String.format(
                    getString(R.string.item_guide_3_1),
                    burningDayPurchaseDialogViewModel.getItemLevel()
                )
                binding.textGuide2.text = String.format(
                    getString(R.string.item_guide_3_2),
                    burningDayPurchaseDialogViewModel.getDiamond()
                )
                binding.btnOk.setText(R.string.register)

                val days = it["days"] as ArrayList<String>
                val daysYmd = it["daysYmd"] as ArrayList<String?>

                burningDayPurchaseDialogViewModel.burningDay(daysYmd[0])

                binding.numberPicker.apply {

                    minValue = 0 // 0~9(총10개)를 set해준다. 아이폰과 다르게 안드로이드는 10개만 보여줌.
                    maxValue = 9
                    displayedValues = days.toTypedArray()

                    setOnValueChangedListener { _, _, newVal ->
                        burningDayPurchaseDialogViewModel.burningDay(daysYmd[newVal])
                    }
                }
                setOnClickListener()
                try {
                    dialog?.show()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        )

        burningDayPurchaseDialogViewModel.purchaseBurningDay.observe(
            viewLifecycleOwner,
            SingleEventObserver {burningDay ->
                dialog?.cancel()
                communityActivityViewModel.apply {
                    idolModel.value?.burningDay = burningDay
                    successBurningDay(burningDay ?: "")
                }

                burningDayPurchaseDialogViewModel.upsertIdol(communityActivityViewModel.idolModel.value!!)
            }
        )
    }

    private fun setOnClickListener() {
        binding.btnOk.setOnClickListener {
            if (burningDayPurchaseDialogViewModel.getBurningDay() == null) {
                Toast.makeText(
                    context,
                    R.string.error_select_day,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            try {
                // 보내줄때도 KST로 변경해서 보내주기.
                val dateFormat = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.ENGLISH
                )
                dateFormat.timeZone =
                    TimeZone.getTimeZone("Asia/Seoul")

                val burningDay = burningDayPurchaseDialogViewModel.getBurningDay()?.let { it1 ->
                    burningDayPurchaseDialogViewModel.getDfYmd().parse(it1)?.let {
                        dateFormat.format(
                            it
                        )
                    }
                }

                burningDayPurchaseDialogViewModel.setBurningDay(burningDay)

                val burningDate = dateFormat.parse(burningDay.toString())
                val targetFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.KOREA)
                targetFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val formattedDate = burningDate?.let { it1 -> targetFormat.format(it1) }

                Util.showDefaultIdolDialogWithBtn2(context,
                    formattedDate,
                    getString(R.string.msg_confirm_burning_day),
                    R.string.register,
                    R.string.btn_cancel,
                    false,
                    false,
                    {
                        Util.closeIdolDialog()
                        Util.showProgress(context)
                        burningDayPurchaseDialogViewModel.purchaseItemBurningDay(context)
                    }) {
                    Util.closeIdolDialog()
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun getInstance(): BurningDayPurchaseDialogFragment {
            val fragment = BurningDayPurchaseDialogFragment()
            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)

            return fragment
        }
    }
}