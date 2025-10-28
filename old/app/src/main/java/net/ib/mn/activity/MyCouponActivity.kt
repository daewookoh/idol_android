package net.ib.mn.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.IdolApplication.Companion.getInstance
import net.ib.mn.R
import net.ib.mn.adapter.MyCouponAdapter
import net.ib.mn.adapter.MyCouponAdapter.OnCouponClickListener
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.core.data.repository.MessagesRepositoryImpl
import net.ib.mn.core.model.CouponModel
import net.ib.mn.databinding.ActivityCouponBinding
import net.ib.mn.dialog.BaseDialogFragment.DialogResultHandler
import net.ib.mn.dialog.RenameDialogFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class MyCouponActivity : BaseActivity(), OnCouponClickListener, DialogResultHandler {
    private var mAdapter: MyCouponAdapter? = null
    @Inject
    lateinit var messagesRepository: MessagesRepositoryImpl

    private lateinit var binding: ActivityCouponBinding

    override fun onBackPressed() {
        try {
            // 푸시에서 넘어온 경우 백버튼 누르면 앱종이 아닌 메인화면으로 이동. 또한 나의 정보에서 쿠폰 진입시 나의 정보 화면이 파괴되어 메인으로 가는 현상 방지
            if (getInstance(this).mainActivity == null && intent.getBooleanExtra(EXTRA_IS_FROM_PUSH, false)) {
                startActivity(MainActivity.createIntent(this, false))
                finish()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            super.onBackPressed()
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCouponBinding.inflate(layoutInflater)
        binding.llContainer.applySystemBarInsets()
        setContentView(binding.root)

        val actionbar = supportActionBar
        if (actionbar != null) {
            actionbar.title = getString(R.string.my_coupon)
            actionbar.setDisplayHomeAsUpEnabled(true)
            actionbar.setHomeButtonEnabled(false)
        }

        mAdapter = MyCouponAdapter(this, this)
        binding.loadingView.setVisibility(View.VISIBLE)

        binding.list.setAdapter(mAdapter)
        loadList()
    }

    private fun loadList() {
        val listener: (JSONObject) -> Unit = { response ->
            if (response.optBoolean("success")) {
                val gson = getInstance(true)
                val listType = object : TypeToken<List<CouponModel?>?>() {
                }.type
                val idols = gson.fromJson<List<CouponModel>>(
                    response.optJSONArray("objects").toString(), listType
                )
                var couponNotUse = 0
                mAdapter!!.clear()
                for (idol in idols) {
                    if (idol.type == "C") {
                        mAdapter!!.add(idol)
                        if (idol.usedAt == null) couponNotUse++
                    }
                }
                if (mContext != null) {
                    Util.setPreference(mContext, "message_coupon_count", couponNotUse)
                    Util.setPreference(mContext, "message_new", false)
                }

                //                    Util.setPreference(mContext,"message_coupon", response.optJSONArray
//                    ("objects").toString());
                if (mAdapter!!.count > 0) {
                    binding.list.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                    mAdapter!!.notifyDataSetChanged()
                } else {
                    binding.list.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                }
                binding.loadingView.visibility = View.GONE
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(msg)) {
                errorText += msg
            }
            makeText(
                this@MyCouponActivity, errorText,
                Toast.LENGTH_SHORT
            ).show()
            if (mAdapter!!.count > 0) {
                binding.list.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
                mAdapter!!.notifyDataSetChanged()
            } else {
                binding.list.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            }
            binding.loadingView.visibility = View.GONE
        }
        MainScope().launch {
            messagesRepository.get(null, null, listener, errorListener)
        }
    }

    override fun onCouponClick(v: View, coupon: CouponModel, position: Int) {
        when (v.id) {
            R.id.ll_coupon_use -> {
                Util.showProgress(this)
                useCoupon1(coupon, position)
            }

            R.id.ll_coupon_use2 -> {
                Util.showProgress(this)
                useCoupon2(coupon, position)
            }

            R.id.coupon_number_cp -> {
                val clipboard =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("coupon", coupon.value)
                clipboard.setPrimaryClip(clip)

                makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun useCoupon1(coupon: CouponModel, position: Int) {
        if (coupon.type == "C") {
            val id = coupon.id

            MainScope().launch {
                messagesRepository.claim(id,
                    { response ->
                        Util.closeProgress()
                        if (response.optBoolean("success")) {
                            val url = response.optString("url")
                            if (!url.isEmpty()) {
                                val browserIntent =
                                    Intent(this@MyCouponActivity, AppLinkActivity::class.java)
                                browserIntent.setData(Uri.parse(url))
                                startActivity(browserIntent)
                            } else if (coupon.heart > 0 || coupon.weakHeart > 0) {
                                var str = ""
                                if (coupon.heart > 0) {
                                    str += (coupon.heart.toString() + " "
                                        + getString(R.string.ever_heart) + "\n")
                                }
                                if (coupon.weakHeart > 0) {
                                    str += (coupon.weakHeart.toString() + " "
                                        + getString(R.string.weak_heart) + "\n")
                                }
                                str += getString(R.string.video_ad_success)
                                Util.showDefaultIdolDialogWithBtn1(
                                    this@MyCouponActivity,
                                    null,
                                    str
                                ) { v12: View? ->
                                    mAdapter!!.remove(position)
                                    mAdapter!!.notifyDataSetChanged()
                                    setResult(ResultCode.COUPON_USED.value)
                                    Util.closeIdolDialog()
                                    if (mAdapter!!.count == 0) {
                                        finish()
                                    }
                                }
                            }
                        } else {
                            UtilK.handleCommonError(this@MyCouponActivity, response)
                        }
                    }, { throwable ->
                        Util.closeProgress()
                        makeText(
                            this@MyCouponActivity, R.string.failed_to_load,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
            }
        }
    }

    private fun useCoupon2(coupon: CouponModel, position: Int) {
        // 닉네임 변경 쿠폰
        if (coupon.value == COUPON_CHANGE_NICKNAME) {
            Util.closeProgress()
            val dialog = RenameDialogFragment.getInstance()
            dialog.useCoupon = true
            dialog.setActivityRequestCode(RequestCode.COUPON_USE.value)
            dialog.show(supportFragmentManager, "rename_dialog")
            return
        }

        if (coupon.type == "C") {
            val id = coupon.id

            MainScope().launch {
                messagesRepository.claim(
                    id,
                    { response ->
                        Util.closeProgress()
                        if (response.optBoolean("success")) {
                            val url = response.optString("url")
                            if (!url.isEmpty()) {
                                val clipboard =
                                    getSystemService(
                                        CLIPBOARD_SERVICE
                                    ) as ClipboardManager
                                val clip = ClipData.newPlainText(
                                    "coupon",
                                    coupon.value
                                )
                                clipboard.setPrimaryClip(clip)

                                makeText(
                                    this@MyCouponActivity, R.string.copied_to_clipboard,
                                    Toast.LENGTH_SHORT
                                ).show()
                                val browserIntent =
                                    Intent(this@MyCouponActivity, AppLinkActivity::class.java)
                                browserIntent.setData(Uri.parse(url))
                                startActivity(browserIntent)
                            } else if (coupon.heart > 0 || coupon.weakHeart > 0) {
                                var str = ""
                                if (coupon.heart > 0) {
                                    str += coupon.heart.toString() + " " + getString(
                                        R.string.ever_heart
                                    ) + "\n"
                                }
                                if (coupon.weakHeart > 0) {
                                    str += coupon.weakHeart.toString() + " " + getString(
                                        R.string.weak_heart
                                    ) + "\n"
                                }
                                str += getString(R.string.video_ad_success)
                                Util.showDefaultIdolDialogWithBtn1(
                                    this@MyCouponActivity,
                                    null,
                                    str
                                ) { v1: View? ->
                                    mAdapter!!.remove(position)
                                    mAdapter!!.notifyDataSetChanged()
                                    setResult(ResultCode.COUPON_USED.value)
                                    Util.closeIdolDialog()
                                    if (mAdapter!!.count == 0) {
                                        finish()
                                    }
                                }
                            }
                        } else {
                            UtilK.handleCommonError(this@MyCouponActivity, response)
                        }
                    }, { throwable ->
                        Util.closeProgress()
                        makeText(
                            this@MyCouponActivity, R.string.failed_to_load,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    //닉네임 쿠폰 사용 했을 경우
    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_CANCELED) return

        if (requestCode == RequestCode.COUPON_USE.value) {
            loadList()
            // 피드 닉네임 갱신되게
            setResult(ResultCode.COUPON_USED.value)
        }
    }

    companion object {
        private var mContext: Context? = null
        private var push = 0
        @JvmField
        var COUPON_CHANGE_NICKNAME: String = "CHANGENICKNAME"

        fun createIntent(context: Context?): Intent {
            val intent = Intent(context, MyCouponActivity::class.java)
            mContext = context
            return intent
        }

        fun createIntent(context: Context?, coupon: Int): Intent {
            val intent = Intent(context, MyCouponActivity::class.java)
            mContext = context
            push = coupon
            return intent
        }
    }
}
