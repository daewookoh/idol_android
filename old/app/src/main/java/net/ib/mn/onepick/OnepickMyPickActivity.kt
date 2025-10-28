package net.ib.mn.onepick

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.OnepickMyPickAdapter
import net.ib.mn.databinding.ActivityOnepickMyPickBinding
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.model.OnepickTopicModel
import com.bumptech.glide.Glide
import net.ib.mn.BuildConfig
import net.ib.mn.onepick.OnePickMainFragment.Companion.IMAGE_PICK_LIST_UPDATE_RESULT_CODE
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets


class OnepickMyPickActivity : BaseActivity() {

    private lateinit var mTopic: OnepickTopicModel
    private lateinit var mMyPickRecyclreView: RecyclerView
    private lateinit var mMyPickAdapter: OnepickMyPickAdapter
    private lateinit var mGlideRequestManager: RequestManager
    private var myPickIdols = ArrayList<OnepickIdolModel>()
    var date: String? = ""

    private var startActivityForResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                IMAGE_PICK_LIST_UPDATE_RESULT_CODE -> {
                    if (result.resultCode == IMAGE_PICK_LIST_UPDATE_RESULT_CODE) {
                        setResult(IMAGE_PICK_LIST_UPDATE_RESULT_CODE)
                        finish()
                    }
                }
            }
        }

    private lateinit var binding: ActivityOnepickMyPickBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_onepick_my_pick)
        binding.nsvOnepickMyPick.applySystemBarInsets()

        supportActionBar?.title = getString(R.string.my_pick)
        mGlideRequestManager = Glide.with(this)

        mTopic = intent.getSerializableExtra(PARAM_TOPIC) as OnepickTopicModel
        mMyPickRecyclreView = binding.rvMypick
        mMyPickAdapter = OnepickMyPickAdapter(this,
                mGlideRequestManager,
                myPickIdols)
        mMyPickRecyclreView.setHasFixedSize(true)
        mMyPickRecyclreView.isNestedScrollingEnabled = false
        mMyPickRecyclreView.isFocusable = false
        mMyPickRecyclreView.adapter = mMyPickAdapter
        binding.clOnepickMyPick.requestFocus()

        if (Util.getPreferenceBool(this, Const.PREF_SHOW_ONEPICK_RESULT_CPATURE, true)) {
            showHelpCaptureEnable()
        }
        binding.btnMypickCapture.setOnClickListener {
            Util.takeScreenShot(this@OnepickMyPickActivity, binding.nsvOnepickMyPick)
        }
        binding.btnResult.setOnClickListener {
            startActivityForResultLauncher.launch(
                OnepickResultActivity.createIntent(this, mTopic)
            )
        }

        if(BuildConfig.CELEB) {
            binding.tvMypick.text = getString(R.string.actor_onepick_mypick)
        }

        showResult()
        setBackPressed(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("OnepickMyPickActivity", "onBackPressed")
                setResult(IMAGE_PICK_LIST_UPDATE_RESULT_CODE)
                finish()
            }
        })
    }

    private fun showHelpCaptureEnable() {
        binding.tvHelpCaptureEnable.visibility = View.VISIBLE
        binding.tvHelpCaptureEnable.setOnClickListener {
            binding.tvHelpCaptureEnable.visibility = View.GONE

            Util.setPreference(applicationContext,
                    Const.PREF_SHOW_ONEPICK_RESULT_CPATURE,
                    false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showResult() {

        binding.tvTopicTitle.text = mTopic.title

        val myPick = intent.getSerializableExtra(PARAM_MY_PICK) as OnepickIdolModel
        val finalRoundIdols =
                intent.getSerializableExtra(PARAM_FINAL_ROUND_IDOLS) as ArrayList<OnepickIdolModel>
        date = intent.getStringExtra(PARAM_DATE)

        val reqImageSize = Util.getOnDemandImageSize(this)
        val imageUrl = date?.let { UtilK.onePickImageUrl(this, myPick.id, it, reqImageSize) }

        val idolId : Int = mTopic.id

        mGlideRequestManager.load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .into(binding.ivTopIdolPhoto)

        if (myPick.idol == null) {
            showError()
        } else {
            binding.tvNickname.text = IdolAccount.getAccount(this)?.userName

            UtilK.setName(this, myPick.idol, binding.tvName, binding.tvGroupName)

            myPickIdols.addAll(finalRoundIdols
                    .filter { it.idol?.getId() != myPick.idol.getId() } as ArrayList<OnepickIdolModel>)
            mMyPickAdapter.notifyDataSetChanged()
        }
    }

    private fun showError(msg: String = getString(R.string.error_abnormal_exception)) {
        Util.showIdolDialogWithBtn1(this@OnepickMyPickActivity,
                null,
                msg
        ) {
            Util.closeIdolDialog()
            finish()
        }
    }

    companion object {
        private const val PARAM_TOPIC = "paramTopic"
        private const val PARAM_FINAL_ROUND_IDOLS = "paramFinalRoundIdols"
        private const val PARAM_DATE = "paramDate"
        private const val PARAM_MY_PICK = "paramMyPick"

        @JvmStatic
        fun createIntent(context: Context,
                         topic: OnepickTopicModel,
                         finalRoundIdols: ArrayList<OnepickIdolModel>,
                         date: String,
                         myPick: OnepickIdolModel): Intent {
            val intent = Intent(context, OnepickMyPickActivity::class.java)
            val args = Bundle()

            args.putSerializable(PARAM_TOPIC, topic)
            args.putSerializable(PARAM_FINAL_ROUND_IDOLS, finalRoundIdols)
            args.putString(PARAM_DATE, date)
            args.putSerializable(PARAM_MY_PICK, myPick)
            intent.putExtras(args)

            return intent
        }
    }
}