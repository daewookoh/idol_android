/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.FragmentMyinfoBinding
import net.ib.mn.feature.common.InAppBanner
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.MessageManager
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.openAppOrStore
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MyInfoViewModel
import org.json.JSONException
import java.text.NumberFormat
import javax.inject.Inject

/**
 * @see
 * */

@AndroidEntryPoint
open class MyInfoBaseFragment : BaseFragment() {

    private var userId: Int = 0

    protected var myInfoBinding: FragmentMyinfoBinding? = null
    private val viewModel: MyInfoViewModel by viewModels()
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        myInfoBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_myinfo, container, false)
        myInfoBinding?.lifecycleOwner = viewLifecycleOwner
        return myInfoBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observedVM(view)
        setEventListener()
    }

    private fun initUI() {
        if (BuildConfig.CELEB) {
            setLevelProgress()
            setAccountView()
        }
    }

    private fun setLevelProgress() {
        val levelProgress = myInfoBinding?.levelProgress

        levelProgress?.apply {
            minProgress = 0
            maxProgress = 100
            textPaint?.color = ContextCompat.getColor(requireContext(), R.color.main)
            textInvertedPaint?.color =
                ContextCompat.getColor(requireContext(), R.color.text_white_black)
            setTextSize(Util.convertDpToPixel(requireContext(), 8f).toInt())
            setProgress(0)
        }
    }

    private fun setAccountView() {
        val name = myInfoBinding?.name
        val photo = myInfoBinding?.photo
        val accountInfo = myInfoBinding?.accountInfo

        val account = IdolAccount.getAccount(baseActivity)
        userId = account?.userModel?.id ?: 0

        if (account != null) {
            setSubscriptionBadge(account)

            name?.apply {
                text = account.userName
                // 꾹 누르면 복사
                setOnLongClickListener {
                    val clipboard =
                        activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("name", name.text.toString())
                    clipboard.setPrimaryClip(clip)

                    if (activity != null && isAdded) {
                        Toast.makeText(
                            context,
                            R.string.copied_to_clipboard,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    false
                }
            }

            if (account.profileUrl.isNotEmpty()) {
                // 프로필 이미지가 안나온다는 경우가 있어서 false로 변경
                mGlideRequestManager
                    .load(account.profileUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(userId))
                    .fallback(Util.noProfileImage(userId))
                    .placeholder(Util.noProfileImage(userId))
                    .into(photo ?: return)
            }
            setFavoritesName(account)

            // 이미지 등록된게 있는지 체크
            if (account.userModel?.imageUrl == null) {
                photo?.setImageResource(Util.noProfileImage(userId))
            }

            accountInfo?.visibility = View.VISIBLE
        } else {
            accountInfo?.visibility = View.GONE
        }
    }

    private fun setEventListener() {
        myInfoBinding?.clSubscriptionBadge?.setOnClickListener {
            startActivity(
                NewHeartPlusActivity.createIntent(
                    context,
                    NewHeartPlusActivity.FRAGMENT_PACKAGE_SHOP,
                ),
            )
        }
        myInfoBinding?.photo?.setOnClickListener {
            openFeed()
        }

        myInfoBinding?.clAccountInfo?.setOnClickListener {
            openFeed()
        }
    }

    // TODO 차후 데이터 바인딩 적용하면서 view 제거
    private fun observedVM(view: View) = with(viewModel) {
        bannerList.observe(viewLifecycleOwner) { bannerList ->
            if (bannerList.isEmpty()) {
                return@observe
            }

            myInfoBinding?.cvInAppBanner?.apply {
                setContent {
                    MaterialTheme {
                        InAppBanner(
                            bannerList = bannerList,
                            clickBanner = { inAppBanner ->
                                val bannerLink = inAppBanner.link ?: return@InAppBanner
                                if (bannerLink.startsWith("idol")) {
                                    val link = bannerLink.split(":")
                                    val idolId = link[1].toInt()
                                    lifecycleScope.launch {
                                        idolsRepository.getIdolsForSearch(
                                            id = idolId,
                                            listener = { response ->
                                                try {
                                                    val idol = IdolGson.getInstance().fromJson(
                                                        response.getJSONArray("objects")
                                                            .getJSONObject(0)
                                                            .toString(),
                                                        IdolModel::class.java,
                                                    )
                                                    setUiActionFirebaseGoogleAnalyticsFragmentWithKey(
                                                        Const.ANALYTICS_BUTTON_PRESS_ACTION,
                                                        "menu_banner",
                                                        "id",
                                                        inAppBanner.id,
                                                    )
                                                    val intent = CommunityActivity.createIntent(
                                                        context ?: return@getIdolsForSearch, idol
                                                    )
                                                    startActivity(intent)
                                                } catch (e: JSONException) {
                                                    e.printStackTrace()
                                                    Toast.makeText(
                                                        context,
                                                        R.string.error_abnormal_default,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            },
                                            errorListener = {
                                                Toast.makeText(
                                                    context,
                                                    R.string.error_abnormal_default,
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            },
                                        )
                                    }
                                } else if (bannerLink.startsWith("http")) {
                                    if (bannerLink.contains("playfillit.com")) {
                                        requireContext().openAppOrStore()
                                    } else {
                                        val browserIntent = Intent(context, AppLinkActivity::class.java)
                                        browserIntent.data = Uri.parse(bannerLink)
                                        startActivity(browserIntent)
                                    }
                                }

                                // analytics impression event
                                val model = inAppBanner
                                UtilK.sendAnalyticsAdEvent(
                                    context ?: return@InAppBanner,
                                    "ad_click_exodus",
                                    model.id
                                )
                            }
                        )
                    }
                }
                visibility = View.VISIBLE
            }
        }
    }

    private fun setSubscriptionBadge(account: IdolAccount) {
        try {
            val clSubscriptionBadge = myInfoBinding?.clSubscriptionBadge
            val tvSubscriptionName = myInfoBinding?.tvSubscriptionName

            val userModel = account.userModel
            val subscriptions = userModel?.subscriptions
            
            if (userModel != null && !subscriptions.isNullOrEmpty()) {
                for (mySubscription in subscriptions) {
                    if (mySubscription.familyappId == 1 || mySubscription.familyappId == 2) {
                        clSubscriptionBadge?.visibility = View.VISIBLE
                        tvSubscriptionName?.text = mySubscription.name
                        break
                    } else {
                        clSubscriptionBadge?.visibility = View.GONE
                    }
                }
            } else {
                clSubscriptionBadge?.visibility = View.GONE
            }
        } catch (e: NullPointerException) {
            myInfoBinding?.clSubscriptionBadge?.visibility = View.GONE
        }
    }

    private fun openFeed() {
        setUiActionFirebaseGoogleAnalyticsFragment(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "menu_feed",
        )
        if (!MessageManager.shared()
                .hasCoupon()
        ) { // 쿠폰 없고, 피드 들어간적 없을 때만 들어갈 때 느낌표 바로 GONE 처리
            myInfoBinding?.ivFeedNew?.visibility = View.GONE
        }
        startActivity(
            FeedActivity.createIntent(
                context ?: return,
                IdolAccount.getAccount(requireActivity())?.userModel,
            ),
        )
    }

    override fun onResume() {
        super.onResume()
        val account = IdolAccount.getAccount(baseActivity)
        if (account != null) {
            val name = myInfoBinding?.name
            val level = myInfoBinding?.level
            val allLevel = myInfoBinding?.allLevel
            val levelUpText = myInfoBinding?.levelUpText
            val photo = myInfoBinding?.photo

            if (account.userName.isEmpty()) {
                accountManager.fetchUserInfo(context)
            }
            name?.text = account.userName
            account.userModel?.let {
                level?.setImageDrawable(Util.getLevelImageDrawable(baseActivity, it.level))
                allLevel?.setImageBitmap(Util.getBadgeImage(baseActivity, it.itemNo))
            }

            levelUpText?.text = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(requireContext()))
                .format(getNextLevelUpHeart(account.levelHeart))
            showLevelBar(account)
            setFavoritesName(account)
            if (account.profileUrl.isNotEmpty()) {
                // 프로필 이미지가 안나온다는 경우가 있어서 false로 변경
                mGlideRequestManager
                    .load(account.profileUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(userId))
                    .fallback(Util.noProfileImage(userId))
                    .placeholder(Util.noProfileImage(userId))
                    .into(photo ?: return)
            }
            setSubscriptionBadge(account)

            // 이미지 등록된게 있는지 체크
            if (account.userModel?.imageUrl == null) {
                photo?.setImageResource(Util.noProfileImage(userId))
            }
        }

        // 피드 들어간 적 없거나, 쿠폰 있을 땐 VISIBLE
        myInfoBinding?.ivFeedNew?.visibility = if (MessageManager.shared().hasCoupon()) {
            View.VISIBLE
        } else { // 피드 들어간 적 있고, 쿠폰 없을 경우 GONE
            View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    protected fun showLevelBar(account: IdolAccount) {
        val levelUpText = myInfoBinding?.levelUpText
        val levelText = myInfoBinding?.levelText
        val levelProgress = myInfoBinding?.levelProgress

        try {
            val level = account.level
            val levelHeart = account.levelHeart
            val currentLevelHeart = Const.LEVEL_HEARTS[level]
            val nextLevelHeart =
                if (level < Const.LEVEL_HEARTS.size - 1) Const.LEVEL_HEARTS[level + 1] else currentLevelHeart

            val total = nextLevelHeart - currentLevelHeart
            val curr = levelHeart - currentLevelHeart

            val textLevelCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(requireContext())).format(level)
            levelText?.text = "LV.$textLevelCount"

            val isMaxLevel = level == Const.MAX_LEVEL
            val progressPercentage = if (isMaxLevel) 100 else (curr * 100 / total).toInt()

            levelProgress?.apply {
                setProgress(progressPercentage)
                text = "$progressPercentage%"
                startAnimation(300)
            }

            levelUpText?.visibility = if (isMaxLevel) View.GONE else View.VISIBLE

            val updateLevelProgress = {
                updateProgress(level, curr, total, isMaxLevel)
                levelProgress?.startAnimation(300)
            }

            if ((levelProgress?.currentProgress ?: 0) > 0) {
                updateLevelProgress()
            } else {
                levelProgress?.apply {
                    setProgress(0)
                    postDelayed({
                        updateLevelProgress()
                    }, 10)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateProgress(level: Int, curr: Long, total: Int, isMaxLevel: Boolean) {
        val levelProgress = myInfoBinding?.levelProgress
        val progress = if (isMaxLevel || level >= Const.LEVEL_HEARTS.size - 1) {
            100
        } else {
            (curr.toFloat() / total.toFloat() * 100.0f).toInt()
        }

        levelProgress?.apply {
            maxProgress = 100
            setProgress(progress)
        }
    }

    private fun getNextLevelUpHeart(heart: Long): Long {
        var next = heart
        for (i in 1 until Const.LEVEL_HEARTS.size) {
            if (next < Const.LEVEL_HEARTS[i]) {
                next = Const.LEVEL_HEARTS[i].toLong()
                break
            }
        }
        return next - heart
    }

    private fun setFavoritesName(account: IdolAccount) {
        val favorite = myInfoBinding?.favorite

        val most = account.most
        if (most != null) {
            var text = SpannableString(most.getName(context))

            if (BuildConfig.CELEB && most.getName(context).contains("_")) {
                // CELEB 이름에 _가 있으면 (도경수_디오)
                val mostName = Util.nameSplit(context, most)[0]
                text = SpannableString("$mostName ${Util.nameSplit(context, most)[1]}")
                text.setSpan(
                    AbsoluteSizeSpan(Util.convertDpToPixel(context, 10f).toInt()),
                    mostName.length + 1,
                    text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                text.setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_dimmed,
                        ),
                    ),
                    mostName.length + 1,
                    text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            } else if (!BuildConfig.CELEB && !most.type.contains("G")) {
                // 애돌 개인인 경우
                if (most.getName(context).contains("_")) {
                    val mostSoloName = Util.nameSplit(context, most)[0]
                    val mostGroupName = Util.nameSplit(context, most)[1]
                    if (Util.isRTL(context)) {
                        text = SpannableString("$mostGroupName $mostSoloName")
                        text.setSpan(
                            AbsoluteSizeSpan(Util.convertDpToPixel(context, 10f).toInt()),
                            0,
                            mostGroupName.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        text.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.text_dimmed,
                                ),
                            ),
                            0,
                            mostGroupName.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    } else {
                        text = SpannableString("$mostSoloName $mostGroupName")
                        text.setSpan(
                            AbsoluteSizeSpan(Util.convertDpToPixel(context, 10f).toInt()),
                            mostSoloName.length + 1,
                            text.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        text.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.text_dimmed,
                                ),
                            ),
                            mostSoloName.length + 1,
                            text.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
            }
            favorite?.text = text
        } else {
            favorite?.text =
                getString(if (BuildConfig.CELEB) R.string.actor_empty_most else R.string.empty_most)
        }
    }
}