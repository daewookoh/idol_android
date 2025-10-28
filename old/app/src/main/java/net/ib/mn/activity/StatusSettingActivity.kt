package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityStatusSettingBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
class StatusSettingActivity : BaseActivity() {

    private var reqToggleHandler = Handler()
    private var feedIsViewable: String? = null
    private var friendAllow: String? = null

    private lateinit var binding: ActivityStatusSettingBinding
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setInit()
    }

    private fun setInit() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_status_setting)
        binding.nsvStatusSetting.applySystemBarInsets()

        supportActionBar?.title = getString(R.string.status_settings)
        getStatus()
        setToggle()
        getDarkmode()
    }

    private fun setToggle() {
        binding.cbPrivacy.setOnClickListener {

            feedIsViewable = if (binding.cbPrivacy.isChecked) {
                "N"
            } else {
                "Y"
            }
            reqToggleHandler.removeCallbacksAndMessages(null)
            reqToggleHandler.postDelayed({
                setStatus()
            }, 300)
        }

        binding.cbBlockFriendAdd.setOnClickListener {

            friendAllow = if (binding.cbBlockFriendAdd.isChecked) {
                "N"
            } else {
                "Y"
            }
            reqToggleHandler.removeCallbacksAndMessages(null)
            reqToggleHandler.postDelayed({
                setStatus()
            }, 300)
        }

        binding.cbDataSaving.setOnClickListener {
            if (binding.cbDataSaving.isChecked) {
                Util.setPreference(this@StatusSettingActivity,
                        Const.PREF_DATA_SAVING,
                        true)
            } else {
                Util.setPreference(this@StatusSettingActivity,
                        Const.PREF_DATA_SAVING,
                        false)
            }
        }

        binding.cbAnimationOnOff.setOnClickListener {
            if (binding.cbAnimationOnOff.isChecked) {
                Util.setPreference(this@StatusSettingActivity,
                        Const.PREF_ANIMATION_MODE,
                        true)
            } else {
                Util.setPreference(this@StatusSettingActivity,
                        Const.PREF_ANIMATION_MODE,
                        false)
            }
            val intent = Intent(this, StartupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

    }

    private fun getDarkmode() {
        // 공식문서는 안드로이드 10부터 지원인데 실제로는 9부터 되는듯
        if( !BuildConfig.DEBUG && Build.VERSION.SDK_INT < Build.VERSION_CODES.P ) {
            binding.clDarkmode.visibility = View.GONE
            return
        }

        with(binding) {
            val darkmode = AppCompatDelegate.getDefaultNightMode()
            when (darkmode) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> tvSystemConfiguration.text = getString(R.string.darkmode_system)
                AppCompatDelegate.MODE_NIGHT_AUTO -> tvSystemConfiguration.text = getString(R.string.darkmode_system)
                AppCompatDelegate.MODE_NIGHT_YES -> tvSystemConfiguration.text = getString(R.string.darkmode_always)
                AppCompatDelegate.MODE_NIGHT_NO -> tvSystemConfiguration.text = getString(R.string.darkmode_never)
            }

            btnDarkmode.setOnClickListener {
                val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_DARKMODE_SETTING)
                val tag = "filter"
                val oldFrag = supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    sheet.show(supportFragmentManager, tag)
                }
            }
        }
    }

    private fun setStatus() {
        lifecycleScope.launch {
            usersRepository.setStatus(
                feedIsViewable = feedIsViewable,
                friendAllow = friendAllow,
                listener = {
                },
                errorListener = {
                    Util.showDefaultIdolDialogWithBtn1(this@StatusSettingActivity, null,
                        getString(R.string.desc_failed_to_connect_internet)
                    ) {
                        Util.closeIdolDialog()
                    }
                }
            )
        }
    }

    private fun getStatus() {
        binding.cbDataSaving.isChecked = Util.getPreferenceBool(this, Const.PREF_DATA_SAVING, false)
        binding.cbAnimationOnOff.isChecked = Util.getPreferenceBool(this, Const.PREF_ANIMATION_MODE, false)

        val account = IdolAccount.getAccount(this) ?: return

        lifecycleScope.launch {
            usersRepository.getStatus(
                userId = account.userModel?.id ?: 0,
                listener = { response ->
                    if (response.optBoolean("success")) {
                        with(binding) {
                            // 피드 비공개
                            if (response.optString("feed_is_viewable", "Y") == "Y") {
                                cbPrivacy.isChecked = false
                                feedIsViewable = "Y"
                            } else {
                                cbPrivacy.isChecked = true
                                feedIsViewable = "N"
                            }
                            // 친구 요청
                            if (response.optString("friend_allow", "Y") == "Y") {
                                cbBlockFriendAdd.isChecked = false
                                friendAllow = "Y"
                            } else {
                                cbBlockFriendAdd.isChecked = true
                                friendAllow = "N"
                            }
                        }
                    }
                },
                errorListener = {
                    Util.showDefaultIdolDialogWithBtn1(this@StatusSettingActivity, null,
                        getString(R.string.desc_failed_to_connect_internet)
                    ) {
                        Util.closeIdolDialog()
                        finish()
                    }
                }
            )
        }
    }

    fun setDarkmode(mode : Int) {
        Util.setPreference(this, Const.KEY_DARKMODE, mode)

        val intent = Intent(this, StartupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()

    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, StatusSettingActivity::class.java)
        }
    }

}