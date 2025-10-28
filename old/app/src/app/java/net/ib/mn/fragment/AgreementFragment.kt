package net.ib.mn.fragment

import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.databinding.FragmentAgreeBinding
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Util
import net.ib.mn.utils.setFirebaseScreenViewEvent
import java.util.Objects

class AgreementFragment : BaseFragment(), View.OnClickListener {
    private var mPrevActionBarTitle: CharSequence? = null
    var displayName: String? = null
    var email: String? = null
    var password: String? = null
    var facebookId: Long? = null
    var domain: String? = null
    var loginType = 0
    private lateinit var binding: FragmentAgreeBinding
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val actionbar = (activity as BaseActivity?)?.getSupportActionBar()
        mPrevActionBarTitle = actionbar?.title
        actionbar?.setTitle(R.string.title_agreement)
        actionbar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDetach() {
        super.onDetach()
        val actionbar = (activity as BaseActivity?)?.getSupportActionBar()
        actionbar?.setTitle(mPrevActionBarTitle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentAgreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context ?: return
        val required = " (${getString(R.string.required)})"
        with(binding) {
            val getColoredRequirement: (Int) -> SpannableString = {
                Util.getColorText(getString(it) + required, required, ContextCompat.getColor(context, R.color.main))
            }
            cbAgree1.setText(getColoredRequirement(R.string.agreement1_agree))
            cbAgree2.setText(getColoredRequirement(R.string.agreement2_agree))
            cbAgree3.setText(getColoredRequirement(R.string.signup_age_limit))

            btnNext.setOnClickListener(this@AgreementFragment)
            cbAgreeAll.setOnClickListener(this@AgreementFragment)
            cbAgree1.setOnClickListener(this@AgreementFragment)
            cbAgree2.setOnClickListener(this@AgreementFragment)
            cbAgree3.setOnClickListener(this@AgreementFragment)
            ivAgree1Right.setOnClickListener(this@AgreementFragment)
            ivAgree2Right.setOnClickListener(this@AgreementFragment)
        }

        // 네트워크 상태 업데이트
        InternetConnectivityManager.updateNetworkState(activity)
        if (!InternetConnectivityManager.getInstance(activity).isConnected) {
            showMessage(getString(R.string.desc_failed_to_connect_internet))
        }
    }

    override fun onResume() {
        super.onResume()
        setFirebaseScreenViewEvent(GaAction.JOIN_AGREE, this::class.simpleName ?: "AgreementFragment")
        validate() //다음 단계 갔다가 백버튼으로 돌아왔을 경우 버튼 기본색인 회색 배경으로 나오는 문제때문에 추가.
        ViewCompat.requestApplyInsets(requireView())
    }

    override fun onClick(v: View) {
        val fm = Objects.requireNonNull(requireActivity()).supportFragmentManager
        when (v.id) {
            R.id.btn_next -> {
                if (binding.cbAgree1.isChecked && binding.cbAgree2.isChecked && binding.cbAgree3.isChecked) {    //체크박스가 다 체크되어있을 때 다음 버튼 누른 경우
                    setUiActionFirebaseGoogleAnalyticsFragment(GaAction.JOIN_AGREE_BUTTON.actionValue, GaAction.JOIN_AGREE_BUTTON.label)
                    // 구글/카카오/라인 로그인이면
                    if (email != null) {
                        if (loginType == LOGIN_GOOGLE) {
                            val mGoogleMoreFragment = GoogleMoreFragment.newInstance(email, displayName, password, "")
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, mGoogleMoreFragment)
                                    .addToBackStack(null).commit()
                        } else {
                            val mKakaoMoreFragment = KakaoMoreFragment.newInstance(email, displayName, password, domain, "", facebookId)
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, mKakaoMoreFragment)
                                    .addToBackStack(null).commit()
                        }
                    } else {
                        fm.beginTransaction().replace(R.id.fragment_container, SignupFragment())
                                .addToBackStack(null).commit()
                    }
                } else {
                    val title: String
                    title = if (!binding.cbAgree1.isChecked) {
                        getString(R.string.need_agree1)
                    } else if (!binding.cbAgree2.isChecked) {
                        getString(R.string.need_agree2)
                    } else {
                        getString(R.string.signup_age_prohibited)
                    }
                    Util.showDefaultIdolDialogWithBtn1(activity,
                            null,
                            title
                    ) { view: View? -> Util.closeIdolDialog() }
                }
                validate()
            }

            R.id.cb_agree1, R.id.cb_agree2, R.id.cb_agree3 -> validate()
            R.id.cb_agree_all -> {
                with(binding) {
                    arrayListOf(cbAgree1, cbAgree2, cbAgree3).forEach {
                        it.isChecked = cbAgreeAll.isChecked
                    }
                    validate()
                }
            }

            R.id.iv_agree1_right -> {
                // 약관 보여주기
                val tosFragment = TermsOfServiceFragment.getInstance()
                fm.beginTransaction()
                    .replace(R.id.fragment_container, tosFragment)
                    .addToBackStack(null).commit()
            }
            R.id.iv_agree2_right -> {
                // 개인정보 취급방침 보여주기
                val privacyFragment = SimplePrivacyPolicyFragment.getInstance()
                fm.beginTransaction()
                    .replace(R.id.fragment_container, privacyFragment)
                    .addToBackStack(null).commit()
            }
        }
    }

    private fun validate() {
        if (binding.cbAgree1.isChecked && binding.cbAgree2.isChecked && binding.cbAgree3.isChecked) {    //체크박스 다 체크 되어있을 경우에만 빨간색으로 버튼 색 변경되도록
            binding.btnNext.setBackgroundResource(R.drawable.bg_round_boarder_main)
            binding.cbAgreeAll.isChecked = true
        } else {
            binding.btnNext.setBackgroundResource(R.drawable.bg_round_boarder_gray200)
            binding.cbAgreeAll.isChecked = false
        }
    }

    companion object {
        // 구글/카카오/라인 로그인에서 넘어오는 경우
        const val LOGIN_GOOGLE = 1
        const val LOGIN_KAKAO = 2
        const val LOGIN_LINE = 3
        const val LOGIN_FACEBOOK = 4
    }
}
