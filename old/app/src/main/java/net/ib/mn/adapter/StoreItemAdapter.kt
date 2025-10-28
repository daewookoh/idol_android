package net.ib.mn.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.RequestManager
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.ItemChargeInappDefaultBinding
import net.ib.mn.databinding.ItemChargeInappDefaultOlderBinding
import net.ib.mn.databinding.ItemChargeInappDefaultWelcomeBinding
import net.ib.mn.databinding.ItemChargeInappSubscribingBinding
import net.ib.mn.databinding.ItemChargeInappSubscriptionBinding
import net.ib.mn.fragment.HeartPlusChinaFragment
import net.ib.mn.fragment.HeartPlusFragment2
import net.ib.mn.model.StoreItemModel
import net.ib.mn.model.SubscriptionModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class StoreItemAdapter(
    private val context: Context,
    private val mGlideRequestManager: RequestManager,
    private val fragment: Fragment,
    private val productList: ArrayList<StoreItemModel>,
    private var bannerUrl: String?
) : RecyclerView.Adapter<StoreItemAdapter.ViewHolder>() {

    interface OnBtnBuyClickListener {
        fun onBtnBuyClick(item: StoreItemModel)
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun getItemViewType(position: Int): Int {
        val mySubscriptions = IdolAccount.getAccount(context)?.userModel?.subscriptions ?: arrayListOf<SubscriptionModel>()

        if (isSubscription(position)) {
            for (mySubscription in mySubscriptions) {
                if (mySubscription.familyappId == 1
                        && mySubscription.skuCode == productList[position].skuCode)
                    return TYPE_SUBSCRIBING
            }

            return TYPE_SUBSCRIPTION
        } else {
            if( productList[position].goods_type == "D" )
                return TYPE_DIAMOND

            if (productList[position].goods_type == "P" && productList[position].skuCode.startsWith(
                    if(BuildConfig.CELEB) "actor_welcome" else "welcome"
                )
            ) {
                return TYPE_WELCOME
            } else if (productList[position].goods_type == "P" && !productList[position].skuCode.startsWith(
                    if(BuildConfig.CELEB) "actor_welcome" else "welcome"
                )) {
                return TYPE_PACKAGE
            }

            return TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productList[position], position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        lateinit var viewHolder: ViewHolder
        val view: View

        when (viewType) {
            TYPE_NORMAL,
            TYPE_PACKAGE -> {
                //시스템 font scale  받아옴.
                val scale: Float = context.resources.configuration.fontScale

                if (scale >= 1.5f) { //특정 font size 이상이 되면 어르신전용 뷰로 아니면 원래 뷰로.
                    val binding = ItemChargeInappDefaultOlderBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    viewHolder = DefaultViewHolder(DefaultBindingProxy(binding))
                } else {
                    val binding = ItemChargeInappDefaultBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false
                    )
                    viewHolder = DefaultViewHolder(DefaultBindingProxy(binding))
                }
            }
            TYPE_WELCOME -> {
                val binding = ItemChargeInappDefaultWelcomeBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                viewHolder = WelComeViewHolder(binding)
            }
            TYPE_SUBSCRIPTION -> {
                val binding = ItemChargeInappSubscriptionBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                viewHolder = SubscriptionViewHolder(binding)
            }
            TYPE_SUBSCRIBING -> {
                val binding = ItemChargeInappSubscribingBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                viewHolder = SubscribingViewHolder(binding)
            }
            TYPE_DIAMOND -> { //다이안은 길지 않으니까 그냥쓴다.
                val binding = ItemChargeInappDefaultBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                viewHolder = DefaultViewHolder(DefaultBindingProxy(binding))
            }
            else -> {
                val binding = ItemChargeInappDefaultBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                viewHolder = DefaultViewHolder(DefaultBindingProxy(binding))
            }
        }

        return viewHolder
    }

    private fun isSubscription(position: Int): Boolean {
        return productList[position].subscription.equals("Y", true)
    }

    inner class SubscribingViewHolder(val binding: ItemChargeInappSubscribingBinding) : ViewHolder(binding.root) {

        private val receivedDateFormat = SimpleDateFormat("yyyy-MM-dd", LocaleUtil.getAppLocale(itemView.context))
        private val sentDateFormat = SimpleDateFormat("yyyy. MM. dd", LocaleUtil.getAppLocale(itemView.context))
        private val mySubscriptions = IdolAccount.getAccount(context)?.userModel?.subscriptions ?: arrayListOf<SubscriptionModel>()

        override fun bind(item: StoreItemModel, position: Int): Unit = with(binding) {

            if (position == 0 ||
                    (position != 0 && productList[position - 1].subscription != "Y")) {
                section.visibility = View.VISIBLE
            } else {
                section.visibility = View.GONE
            }

            if (position == productList.size - 1) {
                border.visibility = View.GONE
            } else {
                border.visibility = View.VISIBLE
            }

            mGlideRequestManager
                    .load(item.imageUrl)
                    .into(itemImg)

            val icon = ContextCompat.getDrawable(context, if(BuildConfig.CELEB) R.drawable.icon_my_question_mark else R.drawable.icon_my_help) as Drawable
            icon.setBounds(
                0,
                0,
                icon.intrinsicWidth,
                icon.intrinsicWidth
            )
            val imageSpan = ImageSpan(icon, ImageSpan.ALIGN_BASELINE)
            subscriptionTitle.text = SpannableString("${item.name}  ").apply {
                setSpan(
                    imageSpan,
                    item.name.length + 1,
                    item.name.length + 2,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }

            for (mySubscription in mySubscriptions) {
                if (mySubscription.skuCode == item.skuCode) {
                    subscriptionCreatedAt.text =
                            "${context.getString(R.string.subscribed)} (${sentDateFormat.format(
                                receivedDateFormat.parse(
                                    mySubscription.subscriptionCreatedAt.substring(
                                        0,
                                        10
                                    )
                                )
                            )}~)"
                    subscriptionExpiredAt.text =
                            "*${context.getString(R.string.next_billing_date)}: ${sentDateFormat.format(
                                receivedDateFormat.parse(
                                    mySubscription.subscriptionExpiredAt.substring(
                                        0,
                                        10
                                    )
                                )
                            )}"
                    break
                }
            }

            itemContainer.setOnClickListener {
                if(BuildConfig.CHINA) {
                    (fragment as HeartPlusChinaFragment).onBtnDetailClick(item)
                } else {
                    (fragment as HeartPlusFragment2).onBtnDetailClick(item)
                }
            }

            val paint = Paint()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT
            paint.textSize = Util.convertDpToPixel(context, 12f)


            price.apply {
                setPadding(
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 2f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 3f).toInt()
                )
            }
        }
    }

    inner class SubscriptionViewHolder(val binding: ItemChargeInappSubscriptionBinding) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(item: StoreItemModel, position: Int): Unit = with(binding) {

            if (position == 0 ||
                    (position != 0 && productList[position - 1].subscription != "Y")) {
                section.visibility = View.VISIBLE
            } else {
                section.visibility = View.GONE
            }

            if(item.goods_type == "D") {
                section.text = context.getString(R.string.shop_diamond)
            }

            if (position == productList.size - 1) {
                border.visibility = View.GONE
            } else {
                border.visibility = View.VISIBLE
            }

            mGlideRequestManager
                    .load(item.imageUrl)
                    .into(itemImg)

            val icon = ContextCompat.getDrawable(context, if(BuildConfig.CELEB) R.drawable.icon_my_question_mark else R.drawable.icon_my_help) as Drawable
            icon.setBounds(
                0,
                0,
                icon.intrinsicWidth,
                icon.intrinsicWidth
            )
            val imageSpan = ImageSpan(icon, ImageSpan.ALIGN_BASELINE)
            subscriptionTitle.text = SpannableString("${item.name}  ").apply {
                setSpan(
                    imageSpan,
                    item.name.length + 1,
                    item.name.length + 2,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
            subscriptionTitle.setOnClickListener {
                if(BuildConfig.CHINA) {
                    (fragment as HeartPlusChinaFragment).onBtnDetailClick(item)
                } else {
                    (fragment as HeartPlusFragment2).onBtnDetailClick(item)
                }
            }

            itemContainer.setOnClickListener {
                if(BuildConfig.CHINA) {
                    (fragment as HeartPlusChinaFragment).onBtnDetailClick(item)
                } else {
                    (fragment as HeartPlusFragment2).onBtnDetailClick(item)
                }
            }

            subscriptionDesc.text = item.description
            price.setOnClickListener {
                if(BuildConfig.CHINA) {
                    (fragment as HeartPlusChinaFragment).onBtnBuyClick(item)
                } else {
                    (fragment as HeartPlusFragment2).onBtnBuyClick(item)
                }
            }

            if (item.skuCode == Const.STORE_ITEM_DAILY_PACK) {
                if (Util.getPreferenceBool(context, Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK, true)) {
                    helpMsgFreeTrial.apply {
                        visibility = View.VISIBLE
                        text = String.format(
                            context.getString(R.string.help_msg_month_free_trial),
                            1
                        )
                        setOnClickListener {
                            helpMsgFreeTrial.visibility = View.GONE

                            //툴팁 클릭 햇으니까 ->  false로  값 바꿔줌.
                            Util.setPreference(
                                context,
                                Const.PREF_SHOW_FREE_TRIAL_DAILY_PACK,
                                false
                            )
                        }
                    }
                } else {
                    helpMsgFreeTrial.visibility = View.GONE
                }

            }

            val paint = Paint()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT
            paint.textSize = Util.convertDpToPixel(context, 12f)

            val priceText = "${item.price}/${context.getString(R.string.month)}"
            var normalItemText = productList[position].price
            for (i in 0 until productList.size) {
                // 현재 item이 단품이고, 다음 item이 구독일 경우
                if (i + 1 < productList.size) {
                    if (productList[i].subscription == "N"
                            && productList[i + 1].subscription == "Y") {
                        normalItemText = productList[i].price
                        break
                    }
                }
            }

            val standardSize = if (priceText.length > normalItemText.length) {
                priceText
            } else {
                normalItemText
            }

            price.apply {
                text = priceText
                setPadding(
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 2f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 3f).toInt()
                )
            }
        }
    }

    inner class DefaultBindingProxy {
        val root: View
        val itemContainer: ConstraintLayout
        val section: AppCompatImageView
        val itemImg: AppCompatImageView
        val heartCount: AppCompatTextView
        val heartCountInfo: LinearLayoutCompat
        val heartCountDesc: AppCompatTextView
        val heartCountDesc2: AppCompatTextView
        val bonusPercent: AppCompatTextView
        val priorPriceLi: LinearLayoutCompat
        val priorPrice: AppCompatTextView
        val price: AppCompatTextView
        val border: View
        val arrow : AppCompatImageView
        val heartCount2: AppCompatTextView
        val bonus: AppCompatImageView

        constructor(binding: ItemChargeInappDefaultBinding) {
            root = binding.root
            itemContainer = binding.itemContainer
            section = binding.section
            itemImg = binding.itemImg
            heartCount = binding.heartCount
            heartCountInfo = binding.heartCountInfo
            heartCountDesc = binding.heartCountDesc
            heartCountDesc2 = binding.heartCountDesc2
            bonusPercent = binding.bonusPercent
            priorPriceLi = binding.priorPriceLi
            priorPrice = binding.priorPrice
            price = binding.price
            border = binding.border
            arrow = binding.arrow
            heartCount2 = binding.heartCount2
            bonus = binding.bonus
        }

        constructor(binding: ItemChargeInappDefaultOlderBinding) {
            root = binding.root
            itemContainer = binding.itemContainer
            section = binding.section
            itemImg = binding.itemImg
            heartCount = binding.heartCount
            heartCountInfo = binding.heartCountInfo
            heartCountDesc = binding.heartCountDesc
            heartCountDesc2 = binding.heartCountDesc2
            bonusPercent = binding.bonusPercent
            priorPriceLi = binding.priorPriceLi
            priorPrice = binding.priorPrice
            price = binding.price
            border = binding.border
            arrow = binding.arrow
            heartCount2 = binding.heartCount2
            bonus = binding.bonus
        }
    }

    inner class DefaultViewHolder(val binding: DefaultBindingProxy) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(item: StoreItemModel, position: Int): Unit = with(binding) {

            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

            if (position == 0) {
                section.visibility = View.VISIBLE
            } else {
                section.visibility = View.GONE
            }

            //이미지가 라이트모드, 다크모드 두개있기때문에 다크모드는 _dark.png로 바꿔줘야됨.
            var imageUrl = item.imageUrl
            if (Util.isDarkTheme(context as Activity)) {
                imageUrl = item.imageUrl.replace(".png", "_dark.png")
            }

            mGlideRequestManager
                    .load(imageUrl)
                    .into(itemImg)

            //메인 하트 지급숫자 및 메인밑에 서브하트,서브다이아 지급 두개.
            heartCount.text = "${numberFormat.format(item.amount)} ${context.getString(R.string.ever_heart)}"
            heartCountDesc.text = "${numberFormat.format(item.bonusAmount)} ${context.getString(R.string.weak_heart)} "
            heartCountDesc2.text = "${numberFormat.format(item.bonusExtraAmount)} ${context.getString(
                R.string.diamond
            )} "

            if(item.bonusExtraAmount == 0){
                heartCountDesc2.visibility = View.GONE
            } else {
                heartCountDesc2.visibility = View.VISIBLE
            }


            val dailyText: String = if (getItemViewType(productList.size - 1) == TYPE_SUBSCRIBING) {
                context.getString(R.string.subscribed)
            } else {
                "${productList[productList.size - 1].price}/${context.getString(R.string.month)}"
            }

            val paint = Paint()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT
            paint.textSize = Util.convertDpToPixel(context, 12f)

            price.apply {
                text = item.price
                setPadding(
                    Util.convertDpToPixel(context, 15f).toInt(),
                    Util.convertDpToPixel(context, 2f).toInt(),
                    Util.convertDpToPixel(context, 15f).toInt(),
                    Util.convertDpToPixel(context, 3f).toInt()
                )
            }

            itemContainer.setOnClickListener {
                if(BuildConfig.CHINA) {
                    (fragment as HeartPlusChinaFragment).onBtnBuyClick(item)
                } else {
                    (fragment as HeartPlusFragment2).onBtnBuyClick(item)
                }
            }

            // 다이아 상점
            if ( item.goods_type == "D" ) { //다이아몬드 상점.

                //모든상점 마지막 border는 없애줍니다.
                if( position == productList.size - 1 ) {
                    border.visibility = View.INVISIBLE
                }
                section.setImageResource(R.drawable.img_diashop_top)
                //다이아는 보너스 없다.
                heartCount.text = "${numberFormat.format(item.amount)} ${context.getString(R.string.diamond)}"
                heartCount.setPadding(0, 0, 0, 0)
                heartCountDesc.text = context.getString(R.string.shop_diamond_desc)
                bonusPercent.visibility = View.GONE
                priorPrice.visibility = View.GONE
                arrow.visibility = View.GONE
                heartCount2.visibility = View.GONE
                bonus.visibility = View.GONE
                heartCountInfo.visibility = View.GONE
            } else if(item.goods_type == "P"){ //패키지 상점.
                // (원스토어, 차이나는 구독상품이 없기 때문에 -1)
                val lastPackagePosition = productList.size - (if(BuildConfig.ONESTORE || BuildConfig.CHINA) 1 else 2)
                //모든상점 마지막 border는 없애줍니다. 패키지 상점은 총 4개가 오기때문에 -2해줘야됨.
                if( position == lastPackagePosition ) {
                    border.visibility = View.INVISIBLE
                }

                section.setImageResource(R.drawable.img_packageshop_top)
                bonusPercent.visibility = View.GONE
                priorPrice.visibility = View.GONE
                arrow.visibility = View.GONE
                bonus.visibility = View.VISIBLE
                heartCountInfo.visibility = View.VISIBLE
                heartCountDesc2.visibility = View.GONE

                heartCount.text = "${numberFormat.format(item.bonusExtraAmount)} ${context.getString(
                    R.string.diamond
                )}"
                heartCount2.text = "${numberFormat.format(item.amount)} ${context.getString(R.string.ever_heart)}"
                heartCountDesc.text = "${numberFormat.format(item.bonusAmount)} ${context.getString(
                    R.string.weak_heart
                )}"

                //패키지 상점에서 이전아이템이 웰컴이면 위쪽으로 공간 주기.
                if(position > 0){
                    if(productList[position - 1].skuCode.startsWith("welcome")){
                        val params = itemContainer.layoutParams as ViewGroup.MarginLayoutParams
                        params.topMargin = Util.convertDpToPixel(context, 30f).toInt()
                        itemContainer.layoutParams = params
                    }
                } else { //기본은 0dp.
                    val params = itemContainer.layoutParams as ViewGroup.MarginLayoutParams
                    params.topMargin = Util.convertDpToPixel(context, 0f).toInt()
                    itemContainer.layoutParams = params
                }

            } else{

                if( position == productList.size - 1 ) {
                    border.visibility = View.INVISIBLE
                }

                section.setImageResource(R.drawable.img_heartshop_top)

                val scale: Float = context.resources.configuration.fontScale

                if(scale >= 1.5f){
                    //하트상점 어르신모드 레이아웃은 제약을 heartCount로 줘야되서 다시 연결해준다.
                    val constraints = ConstraintSet()
                    constraints.clone(itemContainer)
                    constraints.connect(
                        price.id,
                        ConstraintSet.BOTTOM,
                        heartCountInfo.id,
                        ConstraintSet.TOP,
                        0
                    )
                    constraints.connect(
                        price.id,
                        ConstraintSet.START,
                        heartCount.id,
                        ConstraintSet.START,
                        0
                    )
                    constraints.connect(
                        price.id,
                        ConstraintSet.TOP,
                        heartCount.id,
                        ConstraintSet.BOTTOM,
                        0
                    )
                    constraints.connect(
                        price.id,
                        ConstraintSet.END,
                        priorPriceLi.id,
                        ConstraintSet.START,
                        0
                    )
                    constraints.applyTo(itemContainer)
                }

                //하트상점 position 2이상부터 밑에 빈공간이 채워지기때문에 이미지 위치조정해줌.
                if(position > 1){
                    val params = itemImg.layoutParams as ConstraintLayout.LayoutParams
                    params.bottomMargin = Util.convertDpToPixel(context, 15f).toInt()
                    itemImg.layoutParams = params
                }

                val params = heartCountInfo.layoutParams as ConstraintLayout.LayoutParams
                params.leftMargin = Util.convertDpToPixel(context, 4f).toInt()
                heartCountInfo.layoutParams = params
                heartCount2.visibility = View.GONE

                if(item.priorPrice !=null) {
                    priorPrice.apply {
                        val spanString = SpannableString("${item.priorPrice}")
                        spanString.setSpan(
                            StrikethroughSpan(),
                            0,
                            item.priorPrice.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        text = spanString
                    }
                }

                if(item.isFirstPriceCheck){
                    priorPriceLi.visibility = View.GONE
                }else{
                    priorPriceLi.visibility = View.VISIBLE
                }
            }

            if(!bannerUrl.isNullOrEmpty()) {
                mGlideRequestManager.load(bannerUrl).into(section)
            }
        }
    }

    inner class WelComeViewHolder(val binding: ItemChargeInappDefaultWelcomeBinding) : ViewHolder(binding.root) {
        override fun bind(item: StoreItemModel, position: Int): Unit = with(binding) {

            //카드뷰 그림자 처리 및 상단 이미지 처리.
            cdWelcome.background = ContextCompat.getDrawable(context, R.drawable.shop_welcom_radius)

            //productList의 position으로찾으면 2번째 아이템이 1번쨰로갈때 이미지가 변하기때문에 skucode의 맨마지막 숫자로 구분한다.
            val spllitedArray = item.skuCode.split("_")
            val numberOfPack = spllitedArray[spllitedArray.lastIndex].toInt()

            //상단 천막 이미지 처리, 웰컴패키지 타이틀 처리.
            if(numberOfPack == 1) {
                ivWelcomeTopImg.setBackgroundResource(R.drawable.img_store_package_2500)
                ivWelcomeSideSale.setImageResource(R.drawable.img_store_package_sale_2500)
                ivWelcomeSideSale2.setImageResource(R.drawable.img_store_package_sale_2500)
            } else {
                ivWelcomeTopImg.setBackgroundResource(R.drawable.img_store_package_9900)
                ivWelcomeSideSale.setImageResource(R.drawable.img_store_package_sale_9900)
                ivWelcomeSideSale2.setImageResource(R.drawable.img_store_package_sale_9900)
            }

            if(position == 0) {
                tvWelcomeTitle.visibility = View.VISIBLE
                tvWelcomeDesc.visibility = View.VISIBLE
            } else {
                tvWelcomeTitle.visibility = View.GONE
                tvWelcomeDesc.visibility = View.GONE
            }

            tvWelcomeTitle.text = context.getString(R.string.welcome_pack_title)
            tvWelcomeDesc.text = context.getString(R.string.welcome_pack_desc)

            itemImg.setAnimationFromUrl(item.imageUrl)
            itemImg.loop(true)
            itemImg.playAnimation()

            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

            if (position == 0) {
                section.visibility = View.VISIBLE
            } else {
                section.visibility = View.GONE
            }

            heartCountDesc2.visibility = View.GONE

            price.apply {
                text = item.price
                setPadding(
                    Util.convertDpToPixel(context, 15f).toInt(),
                    Util.convertDpToPixel(context, 2f).toInt(),
                    Util.convertDpToPixel(context, 15f).toInt(),
                    Util.convertDpToPixel(context, 3f).toInt()
                )
            }

            itemContainer.setOnClickListener {
                if(BuildConfig.CHINA) {
                    (fragment as HeartPlusChinaFragment).onBtnBuyClick(item)
                } else {
                    (fragment as HeartPlusFragment2).onBtnBuyClick(item)
                }
            }

            section.setBackgroundResource(R.drawable.img_packageshop_top)
            if (!bannerUrl.isNullOrEmpty()) {
                mGlideRequestManager.load(bannerUrl).into(section)
            }
            bonusPercent.visibility = View.GONE
            bonus.visibility = View.VISIBLE
            heartCountInfo.visibility = View.VISIBLE
            heartCountDesc2.visibility = View.GONE

            heartCount.text = "${numberFormat.format(item.bonusExtraAmount)} ${context.getString(
                R.string.diamond
            )}"
            heartCount2.text = "${numberFormat.format(item.amount)} ${context.getString(R.string.ever_heart)}"
            heartCountDesc.text = "${numberFormat.format(item.bonusAmount)} ${context.getString(
                R.string.weak_heart
            )}"

            if(item.priorPrice !=null) {
                priorPrice.apply {
                    val spanString = SpannableString("${item.priorPrice}")
                    spanString.setSpan(
                        StrikethroughSpan(),
                        0,
                        item.priorPrice.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    text = spanString
                }
            }

            //세일 퍼센트 계산.
            val price = item.priceAmountMicros / 1000000.0
            val priorPrice = item.welcomePriorPrice
            val percent = (price / priorPrice.toDouble() * 100.0).roundToInt()
            val saleOff = 100 - percent

            tvWelcomeSideSale.text = "${saleOff}%\nOFF"
        }
    }
    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal abstract fun bind(item: StoreItemModel, position: Int)
    }

    fun setBannerUrl(bannerUrl: String?) {
        this.bannerUrl = bannerUrl
    }

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_SUBSCRIPTION = 1
        const val TYPE_SUBSCRIBING = 2
        const val TYPE_DIAMOND = 3
        const val TYPE_PACKAGE = 4
        const val TYPE_WELCOME = 5
    }
}
