package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivitySettingPushBinding
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
class SettingPushActivity : BaseActivity(), CompoundButton.OnCheckedChangeListener {
    private lateinit var binding: ActivitySettingPushBinding
    @Inject
    lateinit var usersRepository: UsersRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingPushBinding.inflate(layoutInflater)
        binding.svSettingPush.applySystemBarInsets()
        setContentView(binding.root)

        supportActionBar?.setTitle(R.string.setting_push)

        var pushNotice = false
        var pushHeart = false
        var pushComment = false
        var pushSchedule = false
        var pushSupport = false
        var pushChat = false

        val account = getAccount(this@SettingPushActivity)
        val user = account!!.userModel
        if (user != null) {
            val pushFilter = user.pushFilter

            pushNotice = (pushFilter and 0x01) != 0x01
            pushHeart = (pushFilter and 0x02) != 0x02
            pushComment = (pushFilter and 0x04) != 0x04
            pushSchedule = (pushFilter and 0x10) != 0x10
            pushSupport = (pushFilter and 0x20) != 0x20
            pushChat = (pushFilter and 0x40) != 0x40
        }

        binding.cbPushHeart.setChecked(pushHeart)
        binding.cbPushComment.setChecked(pushComment)
        binding.cbPushNotice.setChecked(pushNotice)
        binding.cbPushSchedule.setChecked(pushSchedule)
        binding.cbPushSupports.setChecked(pushSupport)
        binding.cbPushChat.setChecked(pushChat)

        binding.cbPushHeart.setOnCheckedChangeListener(this)
        binding.cbPushComment.setOnCheckedChangeListener(this)
        binding.cbPushNotice.setOnCheckedChangeListener(this)
        binding.cbPushSchedule.setOnCheckedChangeListener(this)
        binding.cbPushSupports.setOnCheckedChangeListener(this)
        binding.cbPushChat.setOnCheckedChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView.getId() == R.id.cb_push_heart) {
//            setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,"push_heart : "+isChecked);
            setPushFilter()
        } else if (buttonView.getId() == R.id.cb_push_notice) {
//            setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,"push_notice : "+isChecked);
            setPushFilter()
        } else if (buttonView.getId() == R.id.cb_push_comment) {
//            setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,"push_comment : "+isChecked);
            setPushFilter()
        } else if (buttonView.getId() == R.id.cb_push_schedule) {
//            setUiActionFirebaseGoogleAnalyticsActivity(Const.ANALYTICS_BUTTON_PRESS_ACTION,"push_schedule : "+isChecked);
            setPushFilter()
        } else if (buttonView.getId() == R.id.cb_push_supports) {
            setPushFilter()
        } else if (buttonView.getId() == R.id.cb_push_chat) {
            setPushFilter()
        }
    }

    // 아이폰<->안드 섞어쓰는 경우 문제가 되서 ...
    fun setPushFilter() {
        // 안받는 부분의 bit를 1로 켜면 된다.
        // 모두 수신 : 0
        // 공지:1, 하트:2, 코멘트:4, 스케줄:16  서포트:32 , 채팅: 64
        var flag = 0
        if (!binding.cbPushNotice.isChecked) {
            flag += 1
        }
        if (!binding.cbPushHeart.isChecked) {
            flag += 2
        }
        if (!binding.cbPushComment.isChecked) {
            flag += 4
        }
        if (!binding.cbPushSchedule.isChecked) {
            flag += 16
        }
        if (!binding.cbPushSupports.isChecked) {
            flag += 32
        }
        if (!binding.cbPushChat.isChecked) {
            flag += 64
        }

        Util.showProgress(this)
        val filter = flag
        lifecycleScope.launch {
            usersRepository.updatePushFilter(
                filter = flag,
                listener = { response ->
                    val account = getAccount(this@SettingPushActivity)
                    val user = account!!.userModel
                    user!!.pushFilter = filter
                    account.saveAccount(this@SettingPushActivity)
                    Util.closeProgress()
                },
                errorListener = {
                    Toast.makeText(
                        this@SettingPushActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, SettingPushActivity::class.java)
        }
    }
}
