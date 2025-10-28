package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityStatusMessageModificationBinding
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

@AndroidEntryPoint
class StatusMessageModificationActivity: BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityStatusMessageModificationBinding
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_status_message_modification)
        binding.clStatusMessageModification.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.feed_edit_status)

        val statusMessage = intent.getStringExtra(PARAM_STATUS_MESSAGE)
        if (statusMessage != null) {
            if (statusMessage.isNotEmpty()) {
                // 수정 아이콘을 위한 길이만큼 제거해줌.
                // 더보기 눌르고 엄청빠르게 수정아이콘 있는 공간을 누르게 되면 수정아이콘이 인식이안됨. 그래서 exception추가 (흔한경우는아님)
                try {
                    binding.etStatusMessage.setText(statusMessage.substring(0, statusMessage.length - 2))
                }catch (e:StringIndexOutOfBoundsException){
                    e.printStackTrace()
                }
            }
        }
        binding.tvStatusMessageCount.text = "${binding.etStatusMessage.text.toString().length}/1000"

        binding.etStatusMessage.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tvStatusMessageCount.text = "${binding.etStatusMessage.text.toString().length}/1000"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
        binding.btnConfirm.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val statusMessage = Util.BadWordsFilterToHeart(this, binding.etStatusMessage.text.toString())
        when (v?.id) {
            binding.btnConfirm.id -> {
                Util.hideSoftKeyboard(this,binding.etStatusMessage)
                lifecycleScope.launch {
                    usersRepository.setStatus(
                        statusMessage = statusMessage,
                        newFriends = NewFriendsActivity.didApplyNewFriends,
                        listener = { response ->
                            Util.showDefaultIdolDialogWithBtn1(this@StatusMessageModificationActivity,
                                null,
                                getString(R.string.msg_save_ok)
                            ) {
                                val resultIntent = Intent()
                                resultIntent.putExtra(PARAM_STATUS_MESSAGE,
                                    statusMessage)

                                setResult(RESPONSE_MODIFIED, resultIntent)
                                finish()
                            }
                        },
                        errorListener = {
                            Util.showDefaultIdolDialogWithBtn1(this@StatusMessageModificationActivity,
                                null,
                                getString(R.string.msg_error_ok)
                            ) {
                                setResult(RESPONSE_CANCEL)
                                finish()
                            }
                        }
                    )
                }
            }
            binding.btnCancel.id -> {
                setResult(RESPONSE_CANCEL)
                finish()
            }
        }
    }

    companion object {
        const val PARAM_STATUS_MESSAGE = "paramStatusMessage"
        const val RESPONSE_MODIFIED = 10
        const val RESPONSE_CANCEL = 20

        @JvmStatic
        fun createIntent(context: Context, statusMessage: String): Intent {
            val intent = Intent(context, StatusMessageModificationActivity::class.java)
            intent.putExtra(PARAM_STATUS_MESSAGE, statusMessage)

            return intent
        }
    }

}