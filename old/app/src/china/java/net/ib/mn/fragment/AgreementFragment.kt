package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.InternetConnectivityManager.Companion.getInstance
import net.ib.mn.addon.InternetConnectivityManager.Companion.updateNetworkState
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.databinding.FragmentAgreeBinding
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.getAppName
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.setFirebaseScreenViewEvent
import java.util.Locale

/**
 * SignupFragment 나오기전에 이용약관 프래그먼트.
 */

class AgreementFragment : BaseFragment(), View.OnClickListener {
    private var mPrevActionBarTitle: CharSequence? = null

    var displayName: String? = null
    var email: String? = null
    var password: String? = null
    var domain: String? = null
    var loginType: Int = 0

    var _binding: FragmentAgreeBinding? = null
    private val binding get() = _binding!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val actionbar = (activity as BaseActivity)
            .supportActionBar
        mPrevActionBarTitle = actionbar!!.title
        actionbar.setTitle(R.string.title_agreement)
        actionbar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDetach() {
        super.onDetach()
        val actionbar = (activity as BaseActivity)
            .supportActionBar
        actionbar!!.title = mPrevActionBarTitle
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvPersonalInformation.setText(
            String.format(
                requireActivity().getString(R.string.msg_personal_information1),
                getAppName(requireContext())
            )
        )
        binding.tvPersonalInformation2.setText(
            String.format(
                requireActivity().getString(R.string.msg_personal_information2),
                getAppName(requireContext())
            )
        )

        val lang = Util.getAgreementLanguage(context)
        if (Util.isUsingNightModeResources(context)) {
            binding.wvAgree1.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray300))
            binding.wvAgree2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray300))
        }
        if (BuildConfig.CELEB) {
            val language = Locale.getDefault().language
            if (language.contains("ko")) {
                binding.wvAgree1.loadUrl(ServerUrl.HOST + "/static/agreement1.html")
                binding.wvAgree2.loadUrl(ServerUrl.HOST + "/static/agreement2.html")
            } else {
                binding.wvAgree1.loadUrl(ServerUrl.HOST + "/static/agreement1_en.html")
                binding.wvAgree2.loadUrl(ServerUrl.HOST + "/static/agreement2_en.html")
            }
        } else {
            binding.wvAgree1.loadUrl(ServerUrl.HOST + "/static/agreement1" + lang + ".html")
            binding.wvAgree2.loadUrl(ServerUrl.HOST + "/static/agreement2" + lang + ".html")
        }

        binding.btnNext.setOnClickListener(this)
        binding.cbAgree1.setOnClickListener(this)
        binding.cbAgree2.setOnClickListener(this)
        binding.cbAgree3.setOnClickListener(this)

        // 네트워크 상태 업데이트
        getInstance(activity)
        updateNetworkState(activity)
        if (!getInstance(activity).isConnected) {
            showMessage(getString(R.string.desc_failed_to_connect_internet))
        }
    }

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.JOIN_AGREE, javaClass.simpleName)
        btnBackgroundColor() //다음 단계 갔다가 백버튼으로 돌아왔을 경우 버튼 기본색인 회색 배경으로 나오는 문제때문에 추가.
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_next -> {
                if (binding.cbAgree1.isChecked && binding.cbAgree2.isChecked && binding.cbAgree3!!.isChecked) {    //체크박스가 다 체크되어있을 때 다음 버튼 누른 경우
                    val fm = requireActivity().supportFragmentManager
                    fm.beginTransaction().replace(R.id.fragment_container, SigninFragment())
                        .addToBackStack(null).commit()
                } else {
                    val title = if (!binding.cbAgree1.isChecked) {
                        getString(R.string.need_agree1)
                    } else if (!binding.cbAgree2.isChecked) {
                        getString(R.string.need_agree2)
                    } else {
                        getString(R.string.signup_age_prohibited)
                    }
                    Util.showDefaultIdolDialogWithBtn1(
                        activity,
                        null,
                        title
                    ) { view: View? -> Util.closeIdolDialog() }
                }
                btnBackgroundColor()
            }

            R.id.cb_agree1, R.id.cb_agree2, R.id.cb_agree3 -> btnBackgroundColor()
        }
    }

    private fun btnBackgroundColor() {
        if (binding.cbAgree1.isChecked && binding.cbAgree2.isChecked && binding.cbAgree3.isChecked) {    //체크박스 다 체크 되어있을 경우에만 빨간색으로 버튼 색 변경되도록
            binding.btnNext.setBackgroundResource(R.drawable.bg_round_boarder_main)
        } else {
            binding.btnNext.setBackgroundResource(R.drawable.bg_round_boarder_gray200)
        }
    }

    companion object {
        // 구글/카카오/라인 로그인에서 넘어오는 경우
        const val LOGIN_GOOGLE: Int = 1
        const val LOGIN_KAKAO: Int = 2
        const val LOGIN_LINE: Int = 3
        const val LOGIN_FACEBOOK: Int = 4
        const val LOGIN_WECHAT: Int = 5
        const val LOGIN_QQ: Int = 6
    }
}
