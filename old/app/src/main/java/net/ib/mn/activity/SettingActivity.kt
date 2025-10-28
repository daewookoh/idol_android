package net.ib.mn.activity

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.databinding.ActivitySettingBinding
import net.ib.mn.databinding.ItemOfficialLinkBinding
import net.ib.mn.model.OfficialLink
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.SvgLoaderForJava
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.UtilK.Companion.setCustomActionBar
import net.ib.mn.utils.ext.applySystemBarInsets
import java.text.DateFormat

class SettingActivity : BaseActivity(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {
    private lateinit var binding: ActivitySettingBinding
    private val languageDialog: Dialog? = null

    private var secureId: String? = null

    // 데일리팩 구독 여부
    private var isPurchasedDailyPack = false
    private var skuCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 언어 설정 변경이 잘 안먹혀 클래식한 방법으로 변경
        binding = ActivitySettingBinding.inflate(layoutInflater)
        binding.clContainer.applySystemBarInsets()

        setContentView(binding.root)

        val account = getAccount(this)

        // OS는 라이트모드 / 앱은 다크모드일때 회원탈퇴하면 앱이 죽는 현상 발생
        // account 정보가 없으면 이 화면에 있을 이유가 없으므로 화면 종료
        if (account == null) {
            finish()
            return
        }

        // 현재는 user model이 null일리 없지만 나중에 로그인하지 않고 사용하는 경우를 대비
        val domain = account?.userModel?.domain ?: ""
        if (domain.equals(Const.DOMAIN_LETTER_KAKAO, ignoreCase = true)) {
            binding.ivMyIdIcon.setImageResource(R.drawable.kakaolink_btn_small)
        } else if (domain.equals(Const.DOMAIN_LETTER_LINE, ignoreCase = true)) {
            binding.ivMyIdIcon.setImageResource(R.drawable.line_icon)
        } else if (domain.equals(Const.DOMAIN_LETTER_FACEBOOK, ignoreCase = true)) {
            binding.ivMyIdIcon.setImageResource(R.drawable.fb_logo)
            binding.tvMyId.text = account?.email
        } else if (domain.equals(Const.DOMAIN_LETTER_WECHAT, ignoreCase = true)) {
            binding.ivMyIdIcon.setImageResource(R.drawable.icon48_appwx_logo)
            binding.tvMyId.text = "WeChat"
        } else if (domain.equals(Const.DOMAIN_LETTER_QQ, ignoreCase = true)) {
            binding.ivMyIdIcon.setImageResource(R.drawable.qqlink_btn_small)
            binding.tvMyId.text = "QQ"
        } else if (domain.equals(Const.DOMAIN_LETTER_GOOGLE, ignoreCase = true)) {
            binding.ivMyIdIcon.setImageResource(R.drawable.google_btn_small)
            binding.tvMyId.text = account?.email
        } else {
            binding.ivMyIdIcon.visibility = View.GONE
            binding.tvMyId.text = account?.email
        }

        // 가입일
        val f = DateFormat.getDateInstance(
            DateFormat.MEDIUM, getAppLocale(
                this
            )
        )
        val createdAt = account?.createdAt
        if (createdAt != null) {
            val dateString = account.createdAt?.let { f.format(it) }

            val textRegDate = findViewById<AppCompatTextView>(R.id.tv_my_reg_date)
            textRegDate.text = dateString
        }

        //툴바 커스텀  적용
        binding.toolbarSetting.setCustomActionBar(this, R.string.title_setting)

        // 문의
        binding.rlSettingMenu02.setOnClickListener(this)
        binding.rlServiceInfo.setOnClickListener(this)
        binding.rlLaboratory.setOnClickListener(this)
        if (getAccount(this) != null) {
            if (getAccount(this)!!.heart == 10) binding.rlLaboratory.visibility =
                View.VISIBLE

            // 비밀번호 변경
            try {
                if (getAccount(this)!!.email!!.endsWith(Const.POSTFIX_KAKAO)) {
                    binding.rlSettingMenu05.visibility = View.GONE
                } else {
                    binding.rlSettingMenu05.setOnClickListener(this)
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
        // 회원탈퇴
        binding.rlSettingMenu06.setOnClickListener(this)
        binding.tvVersion.text =
            getString(R.string.app_version) + "(" + BuildConfig.VERSION_CODE + ")"

        binding.rlMenuStatus.setOnClickListener(this)

        binding.rlBlockList.setOnClickListener(this)

        //		SharedPreferences pref = PreferenceManager
//				.getDefaultSharedPreferences(getApplicationContext());
//		boolean pushHeart = pref.getBoolean(
//				getString(R.string.pref_key_push_heart), true);
//        boolean pushComment = pref.getBoolean("push_comment", true);
//		boolean pushNotice = pref.getBoolean(
//				getString(R.string.pref_key_push_notice), true);
        if (getAccount(this) == null) {
            // 비번 변경 버튼
            binding.rlSettingMenu05.visibility = View.GONE
            // 탈퇴 버튼
            binding.rlSettingMenu06.visibility = View.GONE
        } else if (!TextUtils.isEmpty(domain) && "GKLFWQ".contains(domain)) {
            // 소셜 로그인이면 비번변경 못하게
            binding.rlSettingMenu05.visibility = View.GONE
        }
        binding.rlSettingMenuLang.setOnClickListener(this)
        binding.tvLanguage.text = Util.getCurrentLanguage(this)

        binding.rlSettingMenuPush.setOnClickListener(this)

        // 실험실
        binding.cbUseInternalEditor.setOnCheckedChangeListener(this)
        binding.cbUseHaptic.setOnCheckedChangeListener(this)
        val useInternalEditor =
            Util.getPreferenceBool(this, Const.PREF_USE_INTERNAL_PHOTO_EDITOR, true)
        val useHaptic = Util.getPreferenceBool(this, Const.PREF_USE_HAPTIC, true)

        if (BuildConfig.ONESTORE) {
            // onestore는 언어변경 숨김
            binding.rlSettingMenuLang.visibility = View.GONE
        }
        binding.cbUseInternalEditor.isChecked = useInternalEditor
        binding.cbUseHaptic.isChecked = useHaptic

        //sid encrypt
        encryptUserId()
        binding.tvSecureUserId.text = secureId

        // 애돌만 있음
        if (!BuildConfig.CELEB) {
            setSnsLayout()
        } else {
            // 셀럽은 햅틱 없음
            binding!!.rlSettingMenuHaptic.visibility = View.GONE
        }

        binding.ivCopyBtn.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()

        FLAG_CLOSE_DIALOG = true // 잠금화면 설정에서 false 해놓은거 복구
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.rl_setting_menu_push -> {
                if (Util.mayShowLoginPopup(this)) {
                    return
                }
                startActivity(SettingPushActivity.createIntent(this))
            }

            R.id.rl_setting_menu02 -> {
                if (Util.mayShowLoginPopup(this)) {
                    return
                }
                startActivity(InquiryActivity.createIntent(this))
            }

            R.id.rl_setting_menu05 -> startActivity(ChangePasswdActivity.createIntent(this))
            R.id.rl_setting_menu06 -> {
                val account = getAccount(this)
                setPurchasedDailyPackFlag(account!!)
                if (isPurchasedDailyPack) {
                    dailyCancel()
                } else {
                    Util.showDefaultIdolDialogWithBtn2(
                        this,
                        getString(R.string.setting_menu06), getString(R.string.msg_drop_out),
                        { v13: View? ->
                            Util.closeIdolDialog()
                            startActivity(
                                Intent(
                                    this@SettingActivity,
                                    DeleteAccountActivity::class.java
                                )
                            )
                        },
                        { v14: View? -> Util.closeIdolDialog() })
                }
            }

            R.id.rl_menu_status -> startActivity(StatusSettingActivity.createIntent(this))
            R.id.rl_setting_menu_lang -> startActivity(
                LanguageSettingActivity.createIntent(
                    this
                )
            )

            R.id.rl_service_info -> startActivity(ServiceInfoActivity.createIntent(this))
            R.id.rl_laboratory -> startActivity(LaboratoryActivity.createIntent(this))
            R.id.iv_copy_btn -> {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("secureId", secureId)
                clipboardManager.setPrimaryClip(clipData)
                makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }

            R.id.rl_block_list -> startActivity(UserBlockListActivity.createIntent(this))
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        // 실험실
        if (buttonView.id == R.id.cb_use_internal_editor) {
//            setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,"use_internal_editor : "+isChecked);
            Util.setPreference(this, Const.PREF_USE_INTERNAL_PHOTO_EDITOR, isChecked)
        } else if (buttonView.id == R.id.cb_use_haptic) {
            Util.setPreference(this, Const.PREF_USE_HAPTIC, isChecked)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ_SMS) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.
            Util.log("Received response for read phone state permission request.")

            // Check if the only required permission has been granted
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // read phone state permission has been granted, preview can be displayed
                makeText(
                    this,
                    getString(R.string.msg_heart_box_event_ok),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
//                Log.i(TAG, "read phone state permission was NOT granted.");
                makeText(
                    this,
                    getString(R.string.msg_heart_box_event_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // END_INCLUDE(permission_result)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun encryptUserId() {
        secureId = Util.getSecureId(this)
        Util.log("secureId is $secureId")
    }

    //데일리팩 구독 체크
    private fun setPurchasedDailyPackFlag(account: IdolAccount) {
        if (account.userModel != null && account.userModel!!.subscriptions != null && !account.userModel!!.subscriptions.isEmpty()) {
            for ((familyappId, _, _, _, _, skuCode1) in account.userModel!!.subscriptions) {
                if (familyappId == 1 || familyappId == 2) {
                    if (skuCode1 == Const.STORE_ITEM_DAILY_PACK) {
                        skuCode = skuCode1
                        isPurchasedDailyPack = true
                        break
                    }
                }
            }
        } else {
            isPurchasedDailyPack = false
        }
    }

    //플레이스토어 정기결제 화면
    private fun openPlayStoreAccount() {
        try {
            v("skuCode::" + skuCode + "packageName" + packageName)
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/account/subscriptions?sku=" + skuCode + "package=" + packageName)
                )
            )
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    //회원 탈퇴 눌렀을 때 데일리팩 구독상태일 경우.
    private fun dailyCancel() {
        val msg = String.format(
            getString(R.string.msg_daily_pack_cancel),
            getString(R.string.cancel_subscription)
        )
        Util.showDialogCancelRedBtn2(
            this,
            msg,
            getString(R.string.cancel_subscription),
            "",
            R.string.withdraw,
            R.string.cancel_subscription,
            { v: View? ->
                Util.closeIdolDialog()
                startActivity(DeleteAccountActivity.createIntent(this))
            },
            { v: View? ->
                Util.closeIdolDialog()
                openPlayStoreAccount()
            }
        )
    }

    private fun setSnsLayout() {
        val channels = Util.getPreference(this, Const.PREF_OFFICIAL_CHANNELS)
        val gson = instance
        val listType = object : TypeToken<List<OfficialLink?>?>() {}.type
        val links = gson.fromJson<List<OfficialLink>>(channels, listType)

        binding.layoutLinks.gravity = Gravity.CENTER

        if (links.isEmpty()) {
            binding.layoutLinks.visibility = View.GONE
            return
        }

        for (officialLink in links) {
            // Inflate the binding for each item
            val inflater = LayoutInflater.from(this)
            val officialLinkBinding = ItemOfficialLinkBinding.inflate(inflater, binding.layoutLinks, false)

            if (officialLink.imageUrl!!.contains(".svg")) {
                var imageUrl: String? = ""
                imageUrl = if (Util.isUsingNightModeResources(this)) {
                    officialLink.imageUrlDarkmode
                } else {
                    officialLink.imageUrl
                }
                SvgLoaderForJava.loadSvgImage(officialLinkBinding.btnOfficialLink, imageUrl)
            } else {
                if (Util.isUsingNightModeResources(this)) {
                    Glide.with(this).load(officialLink.imageUrlDarkmode)
                        .into(officialLinkBinding.btnOfficialLink)
                } else {
                    Glide.with(this).load(officialLink.imageUrl).error(R.drawable.img_login_id)
                        .into(officialLinkBinding.btnOfficialLink)
                }
            }

            // Set click listener
            officialLinkBinding.btnOfficialLink.setOnClickListener { v: View? ->
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(officialLink.linkUrl)
                    )
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Util.showIdolDialogWithBtn1(
                        baseContext, null, getString(R.string.msg_error_ok)
                    ) { Util.closeIdolDialog() }
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }

            if (officialLink === links[links.size - 1]) {
                officialLinkBinding.vEmpty.visibility = View.GONE
            }

            binding.layoutLinks.addView(officialLinkBinding.root)
        }
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, SettingActivity::class.java)
        }

        private const val REQUEST_READ_SMS = 0
    }
}
