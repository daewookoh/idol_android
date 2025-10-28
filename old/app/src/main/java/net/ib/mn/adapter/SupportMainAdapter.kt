package net.ib.mn.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.ItemSupportMainBinding
import net.ib.mn.model.SupportAdType
import net.ib.mn.model.SupportListModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getAdDatePeriod
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SupportMainAdapter(
        private val mContext: Context,
        private val mGlideRequestManager: RequestManager,
        private var onClickListener: OnClickListener,
        private val typeList: ArrayList<SupportAdTypeListModel>
) : RecyclerView.Adapter<SupportMainAdapter.ViewHolder>() {
    private var mItems: ArrayList<SupportListModel> = ArrayList<SupportListModel>()
    private var currentCategory: ArrayList<SupportAdType> = arrayListOf()

    interface OnClickListener {
        fun onItemClicked(item: SupportListModel, view: View, position: Int,adPeriod:String)
    }


    fun setItems(@NonNull items: List<SupportListModel>, month:Int, status:Int, isEmptyCallback: (isEmpty: Boolean)->Unit) {

        //date에서 월만 가져오기위한 format
        val monthFormat = SimpleDateFormat("MM", Locale.US)
        monthFormat.timeZone = Const.TIME_ZONE_KST

        mItems.clear()

        if(status == SUCCESS_SUPPORT) {//성공한 서포트일때는  현재 월을 비교해서 -> 한번더 해당월에 맞는 아이템인지 필터링해준다.
            for (i in 0 until items.size) {
                if (month == monthFormat.format(items[i].d_day).toInt()) {
                    mItems.add(items[i])
                }
            }
        }else{//성공한 서포트 이외  서포트 리스트는 -> 전부 리스트로 뿌려준다.
            mItems.addAll(items)
        }

        isEmptyCallback(mItems.isEmpty())

        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemSupportMain: ItemSupportMainBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_support_main,
            parent,
            false
        )
        return SupportMainViewHolder(itemSupportMain)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = mItems[position]
        holder.bind(friend, position)
    }

    fun setCategory(selectedTags: MutableList<SupportAdType>) {
        currentCategory.apply {
            clear()
            addAll(selectedTags)
        }
        notifyDataSetChanged()
    }

    inner class SupportMainViewHolder(val binding: ItemSupportMainBinding) : ViewHolder(binding.root) {
        //광고게시기간.
        private lateinit var adPeriod:String

        override fun bind(item: SupportListModel, position: Int) {

            binding.photoBorder.bringToFront()
            binding.success.bringToFront()

            UtilK.setName(mContext, item.idol, binding.mostIdolName, binding.mostIdolGroup)

            binding.title.text = item.title

            //포함 %주를 표현하기위해 getAdDate함수 adPeriod(광고게시간)계산
            for(i in 0 until typeList.size){
                if(item.type.id == typeList[i].id){
                    adPeriod = typeList[i].period.getAdDatePeriod(context = itemView.context)
                    binding.adType.text = typeList[i].name
                }
            }

            val percentage = (((item.diamond.toDouble() / item.goal.toDouble()) * 100.0)).toInt()
            binding.tvAchievement.text = String.format(mContext.getString(R.string.support_achievement), percentage, LocaleUtil.getAppLocale(itemView.context))

            // 한국어일때 DATE_FIELD값이 이상하게 나와서 MEDIUM으로 바꿈.
            val formatter = if(LocaleUtil.getAppLocale(itemView.context) == Locale.KOREA){
                DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
            }else{
                DateFormat.getDateInstance(DateFormat.DATE_FIELD, LocaleUtil.getAppLocale(itemView.context))
            }
            val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
            val dateFormat = SimpleDateFormat(localPattern, LocaleUtil.getAppLocale(itemView.context))

            if(item.status == 1){//성공일때
                binding.photoBorder.visibility = View.VISIBLE
                binding.photoBorder.setImageResource(R.drawable.img_success)
                binding.success.text = mContext.getString(R.string.support_success)
                //성공시 이미지랑 겹치니까 margin설정.
                var mlp: ViewGroup.MarginLayoutParams?
                if(LocaleUtil.getAppLocale(itemView.context) == Locale.JAPAN){
                    mlp = binding.success.layoutParams as ViewGroup.MarginLayoutParams
                    mlp.setMargins(0,70,0,0)
                    binding.success.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.toFloat())
                }else{
                    mlp = binding.success.layoutParams as ViewGroup.MarginLayoutParams
                    mlp.setMargins(0,30,0,0)
                    binding.success.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.8.toFloat())
                }
                binding.success.visibility = View.GONE
                binding.progressAchLi.visibility = View.GONE
                binding.inProgressLi.background = ContextCompat.getDrawable(mContext,R.drawable.bg_radius_gray300)
                binding.tvAchievement.setTextColor(ContextCompat.getColor(mContext,R.color.text_white_black))
                binding.tvAchievement.text = mContext.getString(R.string.support_end)
                binding.btnIntoSupportDetail.setImageResource(R.drawable.icon_main_arrow_finish)

                val dateString = UtilK.getKSTDateString(item.d_day, itemView.context)
                //성공했을때엔 광고게시기간으로 변경.
                binding.ad.text = String.format(mContext.getString(R.string.format_include_date), dateString , adPeriod)

                //광고 좋아요 댓글.
                binding.articleResultRl.visibility = View.VISIBLE
                val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                binding.tvLikeCount.text = numberFormat.format(item.article.heart)
                binding.tvCommentCount.text = numberFormat.format(item.article.commentCount)

                setAdTypeEmoji(item.type.category)
            }else if(item.status == 2){//실패일때
                binding.photoBorder.visibility = View.VISIBLE
                binding.photoBorder.setImageResource(R.drawable.img_finish_fail)
                binding.success.text = mContext.getString(R.string.support_failed)
                var mlp: ViewGroup.MarginLayoutParams?
                if(LocaleUtil.getAppLocale(itemView.context) == Locale.JAPAN){
                    mlp = binding.success.layoutParams as ViewGroup.MarginLayoutParams
                    mlp.setMargins(10,10,0,0)
                    binding.success.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.8.toFloat())
                }else{
                    mlp = binding.success.layoutParams as ViewGroup.MarginLayoutParams
                    mlp.setMargins(0,0,0,0)
                    binding.success.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.8.toFloat())
                }
                binding.success.visibility = View.VISIBLE
                binding.inProgressLi.background = ContextCompat.getDrawable(mContext,R.drawable.bg_radius_gray300)
                binding.tvAchievement.setTextColor(ContextCompat.getColor(mContext,R.color.text_white_black))
                binding.tvAchievement.text = mContext.getString(R.string.support_end)
                binding.btnIntoSupportDetail.setImageResource(R.drawable.icon_main_arrow_finish)
                binding.articleResultRl.visibility = View.GONE

                setAdTypeEmoji(item.type.category)
            }else{//진행중일때
                binding.inProgressLi.background = ContextCompat.getDrawable(mContext,R.drawable.bg_round_main200)
                binding.photoBorder.visibility = View.INVISIBLE
                binding.success.visibility = View.INVISIBLE
                binding.tvAchievement.setTextColor(ContextCompat.getColor(mContext,R.color.main_light))
                binding.btnIntoSupportDetail.setImageResource(R.drawable.icon_main_arrow)
                binding.progressAchLi.visibility = View.VISIBLE

                val createdAt = UtilK.getKSTDateString(item.created_at, itemView.context)
                val expiredAt = UtilK.getKSTDateString(item.expired_at, itemView.context)

                //진행중일땐 모금기간으로 변경.
                binding.ad.text = "$createdAt ~ $expiredAt"
                binding.articleResultRl.visibility = View.GONE

                binding.tvAdTypeEmoji.visibility = View.GONE
            }

            //썸네일  Webp로  사이즈 축소
            val lowestSizeImageUrl = UtilK.supportImageUrl(mContext, item.id)

            val listId = item.id
            mGlideRequestManager
                    .load(lowestSizeImageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(listId))
                    .fallback(Util.noProfileImage(listId))
                    .placeholder(Util.noProfileImage(listId))
                    .dontAnimate()
                    .into(binding.photo)

            val listener = View.OnClickListener { view->
                onClickListener.onItemClicked(item, view, position,binding.ad.text.toString())
            }

            binding.supportItemMainCon.setOnClickListener(listener)
        }

        private fun setAdTypeEmoji(category: String) {
            binding.tvAdTypeEmoji.apply {
                visibility = View.VISIBLE
                // 뒤에 공백 하나 추가 해주기
                text = when(category) {
                    SupportAdType.KOREA.label -> "\uD83C\uDDF0\uD83C\uDDF7 "
                    SupportAdType.FOREIGN.label -> "\uD83C\uDF0E "
                    SupportAdType.MOBILE.label -> "\uD83D\uDCF1 "
                    else -> ""
                }
            }
        }
    }

    companion object{
        const val SUCCESS_SUPPORT = 1
    }
    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal abstract fun bind(item: SupportListModel, position: Int)
    }

}