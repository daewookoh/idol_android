package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.OnFocusChangeListener
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.chatting.SocketManager.Companion.getInstance
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ActivityDeleteAccountBinding
import net.ib.mn.model.SubscriptionModel
import net.ib.mn.utils.ApiCacheManager.Companion.getInstance
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.handleCommonError
import net.ib.mn.utils.ext.applySystemBarInsets
import java.util.Random
import javax.inject.Inject

@AndroidEntryPoint
class DeleteAccountActivity : BaseActivity(), View.OnClickListener {
    private var mDrawableInputOk: Drawable? = null
    private var mDrawableInputError: Drawable? = null
    private var isValid = false
    private var captcha: String? = null
    private var mAccount: IdolAccount? = null

    private lateinit var binding: ActivityDeleteAccountBinding
    @Inject
    lateinit var usersRepository: UsersRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        binding.llContainer.applySystemBarInsets()
        setContentView(binding.root)

        mAccount = getAccount(this)

        supportActionBar?.setTitle(R.string.setting_menu06)

        init()
    }

    override fun onResume() {
        super.onResume()
        if (mAccount == null) mAccount = getAccount(this)
    }

    protected fun init() {
        mDrawableInputOk = ResourcesCompat.getDrawable(resources, R.drawable.join_approval, null)
        mDrawableInputOk!!.setBounds(
            0, 0, mDrawableInputOk!!.getIntrinsicWidth(),
            mDrawableInputOk!!.getIntrinsicHeight()
        )
        mDrawableInputError = ResourcesCompat.getDrawable(resources, R.drawable.join_disapproval, null)
        mDrawableInputError!!.setBounds(
            0, 0,
            mDrawableInputError!!.intrinsicWidth,
            mDrawableInputError!!.intrinsicHeight
        )

        binding.btnDeleteAccount.setOnClickListener(this)
        binding.btnDeleteAccount.setEnabled(isValid)

        captcha = getCaptcha()
        binding.accountCaptcha.text = captcha

        if (BuildConfig.CELEB) {
            binding.inputCaptcha.onFocusChangeListener = object : OnFocusChangeListener {
                override fun onFocusChange(v: View?, hasFocus: Boolean) {
                    if (hasFocus) {
                        binding.inputCaptcha.background
                            .setTint(ContextCompat.getColor(this@DeleteAccountActivity, R.color.main))
                        return
                    }
                    binding.inputCaptcha.background
                        .setTint(ContextCompat.getColor(this@DeleteAccountActivity, R.color.text_dimmed))
                }
            }
        }

        binding.inputCaptcha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val captchaInput = binding.inputCaptcha.getText().toString()
                if (captcha != captchaInput) {
                    //Util.log(captcha);
                    //Util.log(captchaInput);
                    isValid = false
                    binding.inputCaptcha.setCompoundDrawables(null, null, mDrawableInputError, null)
                    binding.btnDeleteAccount.setEnabled(isValid)
                } else {
                    isValid = true
                    binding.btnDeleteAccount.setEnabled(isValid)
                    binding.inputCaptcha.setCompoundDrawables(null, null, mDrawableInputOk, null)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }


    fun removeAccount() {
        lifecycleScope.launch {
            usersRepository.dropout(
                { response ->
                    Util.closeProgress()
                    if (response.optBoolean("success")) {
                        Util.showDefaultIdolDialogWithBtn1(
                            this@DeleteAccountActivity, null, getString(
                                R.string.dropout_done
                            ), object : View.OnClickListener {
                                override fun onClick(v: View?) {
                                    Util.closeIdolDialog()
                                    try {
                                        val account = getAccount(this@DeleteAccountActivity)
                                        Util.deleteChatDB(
                                            this@DeleteAccountActivity,
                                            account!!.userId
                                        )

                                        //싱글턴이라 탈퇴하고,  앱을 종료하지 않고  회원가입 후, 로그인 하면  아이돌 즐겨찾기 목록이 같이 나와서
                                        //탈퇴할때  clear 시켜준다.
                                        getInstance().clearCache(Const.KEY_FAVORITE)

                                        val manager =
                                            getInstance(this@DeleteAccountActivity, null, null)
                                        manager.disconnectSocket()
                                        manager.socket = null

                                        Util.removeAllPreference(this@DeleteAccountActivity)

                                        // clear account 이후에 getAccount()를 하게되면 다시 인스턴스가 생성되므로
                                        // 무조건 마지막에 선언해야됩니다.
                                        account.clearAccount(this@DeleteAccountActivity)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    val intent = Intent(
                                        this@DeleteAccountActivity,
                                        StartupActivity::class.java
                                    )
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    finish()
                                }
                            })
                    } else {
                        handleCommonError(this@DeleteAccountActivity, response)
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(
                        this@DeleteAccountActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()

                }
            )
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_delete_account) {
            if (isValid) {
                v.setEnabled(false)
                v.postDelayed(object : Runnable {
                    override fun run() {
                        try {
                            v.setEnabled(true)
                        } catch (_: Exception) {
                        }
                    }
                }, 3000)

                //Util.log("test ok!");
                val subscriptions = getAccount(this)!!.userModel!!.subscriptions
                val androidSubscriptions = ArrayList<SubscriptionModel?>()

                if (subscriptions != null
                    && !subscriptions.isEmpty()
                ) {
                    for (subscription in subscriptions) {
                        if (subscription.familyappId == 1 || subscription.familyappId == 2) androidSubscriptions.add(
                            subscription
                        )
                    }
                }

                //기존에 구독중일 경우에는 탈퇴버튼이 아예 안눌리게 되어있었음. usersSubscriptionCancel API를 이용하여 count == androidSubscriptions.size 같을 경우에만 삭제하게 했는데, 구독중일 경우에는 두개가 달라 삭제가 안되었음.
//서버에서는 이 API를 이용하여 다시 가입했을 때 구독 정보를 체크하지 않기 때문에 쓸모없는 API라 판단. 주석 처리
//                if (androidSubscriptions.isEmpty()) {
                removeAccount()

                //                } else {
//                    try {
//                        for (int i = 0; i < androidSubscriptions.size(); i++) {
//
//                            JSONObject receipt = new JSONObject();
//                            receipt.put("packageName", androidSubscriptions.get(i).getPackageName())
//                                    .put("productId", androidSubscriptions.get(i).getSkuCode())
//                                    .put("purchaseToken", androidSubscriptions.get(i).getPurchaseToken());
//
//                            JSONObject param = new JSONObject().put("receipt", receipt);
//
//                            final int count = i;
//
//                            ApiResources.userSubscriptionCancel(DeleteAccountActivity.this,
//                                    param,
//                                    new RobustListener(DeleteAccountActivity.this) {
//                                        @Override
//                                        public void onSecureResponse(JSONObject response) {
//                                            if (count == androidSubscriptions.size()) removeAccount();
//                                        }},
//                                    new RobustErrorListener(DeleteAccountActivity.this) {
//                                        @Override
//                                        public void onErrorResponse(VolleyError error,
//                                                                    String msg) {
//                                            Util.closeProgress();
//                                            Toast.makeText(DeleteAccountActivity.this, R.string.error_abnormal_exception,Toast.LENGTH_SHORT).show();
//                                            if(Util.is_log()){
//                                                showMessage(msg);
//                                            }
//                                        }});
//
//                        }
//
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
            //else Util.log("test fail!");
        }
    }

    //랜덤 문자열 받기
    fun getCaptcha(): String {
        //랜덤으로 추출될  char들 들어있는 string

        val randomString = "ABCDEFGHJKLMNPQRSTUVWXY3456789"
        val stringBuilder = StringBuilder()
        val rnd = Random()

        //총 6자리 랜덤 문자 생성
        (0..5).forEach {
            val rndCharIndex = rnd.nextInt(randomString.length)
            val rndChar = randomString[rndCharIndex]
            stringBuilder.append(rndChar)
        }

        return stringBuilder.toString()
    }

    companion object {
        fun createIntent(context: Context?): Intent {
            val intent = Intent(context, DeleteAccountActivity::class.java)
            return intent
        }
    }
}

