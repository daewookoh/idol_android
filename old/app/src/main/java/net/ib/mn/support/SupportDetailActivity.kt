package net.ib.mn.support

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.adapter.SupportTop5Adapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.ActivitySupportDetailBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.MapDialog
import net.ib.mn.dialog.SupportVoteDialogFragment
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.SupportAdType
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.SupportTop5Model
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.getAdDatePeriod
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.setFirebaseUIAction
import org.json.JSONException
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class SupportDetailActivity : BaseActivity(),
    View.OnClickListener,
    BaseDialogFragment.DialogResultHandler {

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl
    @Inject
    lateinit var accountManager: IdolAccountManager

    private lateinit var model: SupportListModel

    private lateinit var mGlideRequestManager: RequestManager

    private lateinit var supportTop5Adapter: SupportTop5Adapter

    private lateinit var items: ArrayList<SupportTop5Model>

    private var id: Int? = null

    private var curVoteCount: Int? = null
    private var maxVoteCount: Int? = null

    var myvoteDia: Int = 0

    //타입 리스트
    private lateinit var typeList: ArrayList<SupportAdTypeListModel>

    //내 다이아몬드
    private var myTotalDia: Int? = null

    private var isVoted = false

    private lateinit var binding: ActivitySupportDetailBinding
    override fun onResume() {
        super.onResume()
        val account = IdolAccount.getAccount(this)
        if (account != null) {
            accountManager.fetchUserInfo(this, {
                myTotalDia = account.userModel?.diamond ?: 0
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_detail)
        binding.clContainer.applySystemBarInsets()

        typeList = arrayListOf()
        mGlideRequestManager = Glide.with(this)

        id = intent.getIntExtra("id", 0)

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.line_divider)!!)

        items = arrayListOf()
        binding.rvTop5.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        supportTop5Adapter = SupportTop5Adapter(
            this,
            mGlideRequestManager,
            items
        )
        supportTop5Adapter.setHasStableIds(true)
        binding.rvTop5.adapter = supportTop5Adapter
        binding.rvTop5.addItemDecoration(divider)
        binding.rvTop5.setHasFixedSize(true)

        //불려지는 순서 getSupportDetail -> getType5 -> setUI
        getSupportDetail()
        binding.btnBack.setOnClickListener(this)
        binding.layoutSupportButton.setOnClickListener(this)
        binding.ivBtnShare.setOnClickListener(this)

        binding.tvContent.isSelected = true

        if (BuildConfig.CELEB) {
            binding.ivLocationMarker.setImageResource(R.drawable.icon_marker_celeb)
        }

        binding.tvContent.setOnClickListener {
            binding.tvTop5.isSelected = false
            binding.tvContent.isSelected = true

            binding.detailLi.visibility = View.VISIBLE
            binding.detailLi2.visibility = View.GONE
        }

        binding.tvTop5.setOnClickListener {
            binding.tvTop5.isSelected = true
            binding.tvContent.isSelected = false

            binding.detailLi.visibility = View.GONE
            binding.detailLi2.visibility = View.VISIBLE
        }
    }

    private fun getTop5() {
        MainScope().launch {
            supportRepository.getTop5(
                supportId = model.id,
                { response ->
                    if (response.optBoolean("success")) {
                        items.clear()
                        myvoteDia = response.getInt("my_support")
                        val array = response.getJSONArray("objects")
                        Util.log("SupportDetail::top5 -> ${array}")
                        val gson = IdolGson.getInstance()

                        try {
                            for (i in 0 until array.length()) {
                                items.add(
                                    gson.fromJson(
                                        array.getJSONObject(i).toString(),
                                        SupportTop5Model::class.java
                                    )
                                )
                            }

                            for (i in items.indices) {
                                val item: SupportTop5Model = items.get(i)
                                // 동점자 처리
                                if (i > 0 && items.get(i - 1).diamond == item.diamond) item.rank =
                                    items.get(i - 1).rank else item.rank = i
                            }

                            supportTop5Adapter.setItems(items)
                            supportTop5Adapter.notifyDataSetChanged()
                            setUI()

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }, { throwable ->
                    Toast.makeText(
                        this@SupportDetailActivity, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }
                }
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setUI() {
        binding.loading.visibility = View.GONE

        binding.headerBar.bringToFront()

        UtilK.setName(this, model.idol, binding.name, binding.group)

        mGlideRequestManager
            .load(model.image_url)
            .centerCrop()
            .dontAnimate()
            .into(binding.photo1)

        mGlideRequestManager
            .load(model.type.imageUrl)
            .dontAnimate()
            .into(binding.adExampleIv)

        mGlideRequestManager
            .load(model.type.imageUrl2)
            .dontAnimate()
            .into(binding.ivAdGuide)

        binding.tvAdName.text = model.type.name.replace("&#39;", " \' ")
        binding.adLocation.text = model.type.location.replace("&#39;", " \' ")
        binding.adDescription.text = model.type.description.replace("&#39;", " \' ")
        binding.adTitle.text = model.title

        val createdAt = UtilK.getKSTDateString(model.created_at, this)
        val expiredAt = UtilK.getKSTDateString(model.expired_at, this)
        val dateString = UtilK.getKSTDateString(model.d_day, this)

        binding.tvFundPeriod.text = "$createdAt ~ $expiredAt"
        binding.adPeriod.text =
            "${String.format(getString(R.string.format_include_date), dateString, model.type.period.getAdDatePeriod(this))}"
        //남은 시간 계산.
        val currentDate = System.currentTimeMillis()

        var resultDate = Util.dateFromUTC(model.expired_at).time - currentDate
        var transDate = resultDate / (24 * 60 * 60 * 1000)
        transDate = abs(transDate)

        var dayDate = resultDate % (24 * 60 * 60 * 1000)
        dayDate /= (60 * 60 * 1000)

        // 0시간 남음으로 표시되면 1시간으로 표시
        if (transDate == 0L && dayDate == 0L) {
            dayDate = 1
        }
        val returnTime = if (transDate == 0L) {
            String.format(getString(R.string.date_format_hours), dayDate)
        } else {
            "${String.format(getString(R.string.date_format_days), transDate)} ${
                String.format(
                    getString(R.string.date_format_hours),
                    dayDate
                )
            }"
        }

        binding.adTime.text = when (model.status) {
            0 -> {
                "${getString(R.string.date_deadline)} $returnTime"
            }

            1 -> {
                getString(R.string.support_success)
            }

            2 -> {
                getString(R.string.support_end_2)
            }

            else -> {
                ""
            }
        }

        val stringResId = when (model.type.category) {
            SupportAdType.KOREA.label -> R.string.adtype_korean
            SupportAdType.FOREIGN.label -> R.string.adtype_global
            SupportAdType.MOBILE.label -> R.string.adtype_mobile
            else -> null
        }

        if (stringResId != null) {
            binding.tvCategory.text = getString(stringResId)
            binding.tvCategory.visibility = View.VISIBLE
        } else {
            binding.tvCategory.visibility = View.GONE
        }

        maxVoteCount = model.goal
        curVoteCount = model.diamond

        val curVoteCountComma =
            NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this)).format(curVoteCount)
        val maxVoteCountComma =
            NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this)).format(maxVoteCount)
        binding.tvGoal.text =
            getString(R.string.support_goal) + " $curVoteCountComma / $maxVoteCountComma"

        binding.progress.visibility = View.VISIBLE
        if (curVoteCount == 0) {
            binding.progress.setWidthRatio(15)
        } else {
            binding.count.text =
                "${(((curVoteCount!!.toDouble() / maxVoteCount!!.toDouble()) * 100.0)).toInt()}%"
            //15 이하는 텍스트가 짤릴 수 있으므로 15이하는 15로 설정.
            if ((curVoteCount!!.toDouble() / maxVoteCount!!.toDouble()) * 100.0 < 15.0) {
                binding.progress.setWidthRatio(15)
            } else {
                binding.progress.setWidthRatio(((curVoteCount!!.toDouble() / maxVoteCount!!.toDouble()) * 100.0).toInt())
            }
        }

        val countDiaFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this)).format(myvoteDia)
        binding.myDiamond.text =
            String.format(getString(R.string.heart_count_format), countDiaFormat)


        if (model.status == 1 || model.status == 2) {
            val paddingBottomInDp = 0
            val scale = resources.displayMetrics.density
            val paddingBottomInPx = (paddingBottomInDp * scale + 0.5f).toInt()

            binding.svDetail.setPadding(
                binding.svDetail.paddingLeft,
                binding.svDetail.paddingTop,
                binding.svDetail.paddingRight,
                paddingBottomInPx
            )

            binding.layoutBottom.visibility = View.GONE
        } else {
            val paddingBottomInDp = 75
            val scale = resources.displayMetrics.density
            val paddingBottomInPx = (paddingBottomInDp * scale + 0.5f).toInt()

            binding.svDetail.setPadding(
                binding.svDetail.paddingLeft,
                binding.svDetail.paddingTop,
                binding.svDetail.paddingRight,
                paddingBottomInPx
            )
        }

        //광고 디자인 공모전 안내.
        binding.adGuidance.text = Util.getPreference(this, Const.AD_GUIDANCE)

        try {
            if (binding.adMakingGuideTitle.text.isEmpty()) {
                val returnGuide = model.type.guide.replace("&#39;", " \' ").split("\n")

                returnGuide?.let {
                    try {
                        binding.adMakingGuideTitle.text = returnGuide[0]
                        UtilK.addSupportGuideView(this, returnGuide, binding.llDetailGuide)
                    } catch (e: Exception) {
                        Toast.makeText(this, R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                            .show();
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    private fun getSupportDetail() {
        MainScope().launch {
            supportRepository.getSupportDetail(id!!,
                { response ->
                    Util.log("SupportDetail::${response}")
                    val gson = IdolGson.getInstance(false) // 서포트는 UTC
                    try {
                        model = gson.fromJson(response.toString(), SupportListModel::class.java)

                        if ((model.type.locationImageUrl == null && model.type.locationMapUrl == null) || model.type.category == SupportAdType.MOBILE.label) {
                            binding.layoutCheckLocation.visibility = View.GONE
                        } else {
                            binding.layoutCheckLocation.setOnClickListener {
                                setFirebaseUIAction(GaAction.SUPPORT_ADDRESS)
                                if (!model.type.locationImageUrl.isNullOrEmpty()) {
                                    MapDialog.getInstance(
                                        model.type.locationImageUrl!!
                                    ).show(this@SupportDetailActivity.supportFragmentManager, "map_dialog")
                                } else {
                                    val gmmIntentUri = Uri.parse(model.type.locationMapUrl.toString())
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

                                    if (mapIntent.resolveActivity(packageManager) != null) {
                                        startActivity(mapIntent)
                                    }
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    getTop5()
                },
                { throwable ->
                    Toast.makeText(
                        this@SupportDetailActivity, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (Util.is_log()) {
                        showMessage(throwable.message)
                    }

                })
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_back -> {
                if (isVoted) {
                    setUpdateVoteCountResult()
                }
                finish()
            }

            R.id.layout_support_button -> {
                //onResume에서한번 투표할때 한번 초기화 시켜주기.
                val account = IdolAccount.getAccount(this)
                if (account != null) {
                    myTotalDia = account.userModel?.diamond ?: 0
                }
                Util.log("SupportDetail::myTotalDia is ${myTotalDia}")
                if (myTotalDia!! <= 0) {
//                    val chargeDialogFragment = SupportChargeDiaDialogFragment.getDiaChargeInstance()
//                    chargeDialogFragment.show(supportFragmentManager, "charge_diamond")
                    Util.showChargeDiamondWithBtn1(this@SupportDetailActivity, null, null, {
                        startActivity(
                            NewHeartPlusActivity.createIntent(
                                this@SupportDetailActivity,
                                NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP
                            )
                        )
                        Util.closeIdolDialog()
                    }, {
                        startActivity(
                            NewHeartPlusActivity.createIntent(
                                this@SupportDetailActivity,
                                NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP
                            )
                        )
                        Util.closeIdolDialog()
                    }, {
                        Util.closeIdolDialog()
                    })
                } else {
                    voteDiamond()
                }
            }

            //서포트 공유 버튼 클릭
            R.id.iv_btn_share -> {

                //해당 detail 화면 공유 실행
                shareSupportDetail()
            }
        }
    }

    //서포트 화면 공유
    private fun shareSupportDetail() {
        val idolName = Util.nameSplit(this, model.idol)
        if (idolName[1] != "") idolName[1] = "#${UtilK.removeWhiteSpace(idolName[1])}"


        val params = listOf(LinkStatus.SUPPORTS.status, id.toString())
        val url = LinkUtil.getAppLinkUrl(context = this@SupportDetailActivity, params = params)
        val adName = model.type.name.replace("&#39;", " \' ")
        val msg = String.format(
            getString(if (BuildConfig.CELEB) R.string.share_in_progress_support_celeb else R.string.share_in_progress_support),
            UtilK.removeWhiteSpace(idolName[0]), idolName[1], idolName[0], adName, ""
        )

        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "in_progress_support_share"
        )

        UtilK.linkStart(this, url = url, msg = msg)
    }

    private fun voteDiamond() {
        val adName = model.type.name.replace("&#39;", " \' ")
        val dialogFragment = SupportVoteDialogFragment.getDiaMondVoteInstance(model, id!!, adName)
        dialogFragment.setActivityRequestCode(RequestCode.SUPPORT_VOTE.value)
        dialogFragment.show(supportFragmentManager, "support_vote")

    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.SUPPORT_VOTE.value && resultCode == BaseDialogFragment.RESULT_OK) {
            val myVoteDiamond =
                data?.getIntExtra(SupportVoteDialogFragment.PARAM_MYVOTE_DIAMOND, -1)
            if (myVoteDiamond!! > 0) {
                isVoted = true
                curVoteCount = curVoteCount?.plus(myVoteDiamond)
                getSupportDetail()
            } else {
                Util.closeProgress()
            }

        }

    }

    override fun onBackPressed() {
        if (isVoted) {
            setUpdateVoteCountResult()
        }
        super.onBackPressed()
    }

    //달성률 변화가 생겼을때 서포트 메인화면으로 Result값을 넘겨줍니다.
    private fun setUpdateVoteCountResult() {
        val data = Intent()
        data.putExtra(UPDATE_VOTE_COUNT_ID, model.id)
        data.putExtra(UPDATE_VOTE_COUNT, curVoteCount)
        setResult(ResultCode.SUPPORT_VOTE_UPDATE.value, data)
    }

    companion object {

        const val UPDATE_VOTE_COUNT_ID = "id"
        const val UPDATE_VOTE_COUNT = "update_vote_count"

        @JvmStatic
        fun createIntent(context: Context, id: Int): Intent {
            val intent = Intent(context, SupportDetailActivity::class.java)
            intent.putExtra("id", id)
            return intent
        }
    }
}