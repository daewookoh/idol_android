package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.adapter.MyHeartExpandableRcyAdapter
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.FragmentMyHeartHistoryBinding
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsetsAndRequest
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MyHeartInfoViewModel
import net.ib.mn.viewmodel.MyHeartInfoViewModelFactory
import org.json.JSONArray
import java.text.NumberFormat
import javax.inject.Inject

@AndroidEntryPoint
class MyheartHistoryFragment : BaseFragment() {
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private lateinit var binding: FragmentMyHeartHistoryBinding

    @Inject
    lateinit var getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase

    private val myHeartInfoViewModel: MyHeartInfoViewModel by activityViewModels {
        MyHeartInfoViewModelFactory(requireContext(), SavedStateHandle(), usersRepository, accountManager, getIsEnableVideoAdPrefsUseCase)
    }

    private var heartSpendRcyAdapter = MyHeartExpandableRcyAdapter()
    private var heartEarnRcyAdapter = MyHeartExpandableRcyAdapter()
    private var diaSpendRcyAdapter = MyHeartExpandableRcyAdapter()
    private var diaEarnRcyAdapter = MyHeartExpandableRcyAdapter()

    private var isHeartSpendExpanded = false
    private var isHeartEarnExpanded = false
    private var isDiaSpendExpanded = false
    private var isDiaEarnExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_my_heart_history, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (BuildConfig.CELEB) {
            (activity as? AppCompatActivity)?.supportActionBar?.hide()
        }
        initSet()
        setClickEvent()
        observeVM()
    }

    override fun onResume() {
        super.onResume()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (parentFragmentManager.backStackEntryCount > 0) {
                        if (BuildConfig.CELEB) (activity as? AppCompatActivity)?.supportActionBar?.show()
                        parentFragmentManager.popBackStack()
                    } else {
                        // 액티비티 이벤트 사용할거면 사용
                    }
                }
            })
    }

    //초기 세팅
    private fun initSet() {
        if (!BuildConfig.CELEB) binding.clContainer.applySystemBarInsetsAndRequest()

        val titleColor = if (Util.isDarkTheme(requireActivity())) {
            R.color.color_white
        } else {
            R.color.text_default
        }
        binding.tvTitle.setTextColor(requireContext().getColor(titleColor))
        //각 리사이클러뷰 adapter 연결
        binding.rcyHeartAccLog.adapter = heartEarnRcyAdapter
        binding.rcyHeartUsedLog.adapter = heartSpendRcyAdapter
        binding.rcyDiaAccLog.adapter = diaEarnRcyAdapter
        binding.rcyDiaUsedLog.adapter = diaSpendRcyAdapter

        //나의 내역  관련  데이터를 뷰모델로 부터  observe해서  받아옴.
        //viewLifecycleOwner =>  데이터  생명주기 영향 안받게
        myHeartInfoViewModel.postMyHeartHistory(myHeartInfoViewModel.getMyHeartHistory())

        myHeartInfoViewModel.resultModelForMyHeartHistory.observe(viewLifecycleOwner, SingleEventObserver{
            val arrayHeartSpend=it["spend"] as JSONArray?
            val arrayHeartEarn=it["earn"] as JSONArray?
            val arrayDiaSpend=it["diamond_spend"] as JSONArray?
            val arrayDiaEarn=it["diamond_earn"]as JSONArray?
            heartSpendRcyAdapter.addDataList(arrayHeartSpend, HEART_LOG, true,isHeartSpendExpanded)
            heartEarnRcyAdapter.addDataList(arrayHeartEarn, HEART_LOG, false,isHeartEarnExpanded)
            diaSpendRcyAdapter.addDataList(arrayDiaSpend, DIAMOND_LOG, true,isDiaSpendExpanded)
            diaEarnRcyAdapter.addDataList(arrayDiaEarn, DIAMOND_LOG, false,isDiaEarnExpanded)
        })

        if(BuildConfig.CELEB) {
            binding.btnHeartInfo.visibility = View.GONE
        }
    }


    //클릭 이벤트들 정의
    private fun setClickEvent() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.llHeader.setOnClickListener {
            // no-op
        }

        binding.btnHeartInfo.setOnClickListener {
            if (BuildConfig.CELEB) {
                return@setOnClickListener
            }
            val heart: String =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(requireContext()))
                    .format(myHeartInfoViewModel.getMissionHeart().toLong())
            val help =
                String.format(requireActivity().getString(R.string.myheart_today_earn_help), heart)
            Util.showDefaultIdolDialogWithBtn1(
                requireActivity(),
                requireActivity().getString(R.string.myheart_today_earn),
                help
            ) { Util.closeIdolDialog() }
        }

        //하트 사용내역
        binding.heartUsedHistory.setOnClickListener {
            setUiActionFirebaseGoogleAnalyticsFragment(
                GaAction.MYHEART_USAGE.actionValue,
                GaAction.MYHEART_USAGE.label
            )
            isHeartSpendExpanded = !isHeartSpendExpanded
            setArrowStatus(isHeartSpendExpanded, binding.ivArrowClose, binding.ivArrowOpen)
            collapseHistoryItem(isHeartSpendExpanded, heartSpendRcyAdapter)
        }

        //하트 적립 내역
        binding.heartAccHistory.setOnClickListener {
            setUiActionFirebaseGoogleAnalyticsFragment(
                GaAction.MYHEART_GET.actionValue,
                GaAction.MYHEART_GET.label
            )
            isHeartEarnExpanded = !isHeartEarnExpanded
            setArrowStatus(isHeartEarnExpanded, binding.ivArrowClose1, binding.ivArrowOpen1)
            collapseHistoryItem(isHeartEarnExpanded, heartEarnRcyAdapter)
        }

        //다이아  사용 내역
        binding.diaUsedHistory.setOnClickListener {
            isDiaSpendExpanded = !isDiaSpendExpanded
            setArrowStatus(isDiaSpendExpanded, binding.ivArrowClose2, binding.ivArrowOpen2)
            collapseHistoryItem(isDiaSpendExpanded, diaSpendRcyAdapter)
        }

        //다이아 적립 내역
        binding.diaAccHistory.setOnClickListener {
            isDiaEarnExpanded = !isDiaEarnExpanded
            setArrowStatus(isDiaEarnExpanded, binding.ivArrowClose3, binding.ivArrowOpen3)
            collapseHistoryItem(isDiaEarnExpanded, diaEarnRcyAdapter)
        }
    }

    private fun observeVM() {
        myHeartInfoViewModel.todayEarnHeart.observe(viewLifecycleOwner) {
            val locale = LocaleUtil.getAppLocale(context ?: return@observe)

            binding.tvEverHeartCount.text = NumberFormat.getNumberInstance(locale).format(it.peekContent().first)
            binding.tvDailyHeartCount.text =
                NumberFormat.getNumberInstance(locale).format(it.peekContent().second)
        }
    }

    //아이템 collapse여부  adapter로 보냄.
    private fun collapseHistoryItem(
        isExpanded: Boolean,
        recyclerViewAdapter: MyHeartExpandableRcyAdapter,
    ) {
        recyclerViewAdapter.collapse(isExpanded)
        recyclerViewAdapter.notifyDataSetChanged()
    }


    //각  히스토리  아이템 눌렸을때  화살표 방향  위아래 체인지
    private fun setArrowStatus(
        isExpanded: Boolean,
        ivArrowClose: ImageView,
        ivArrowOpen: ImageView,
    ) {
        if (isExpanded) {
            ivArrowClose.visibility = View.GONE
            ivArrowOpen.visibility = View.VISIBLE
        } else {
            ivArrowClose.visibility = View.VISIBLE
            ivArrowOpen.visibility = View.GONE
        }
    }

    companion object {

        //하트 로그인지 다이아 로그인지 여부 체크
        const val DIAMOND_LOG = 0
        const val HEART_LOG = 1
    }
}