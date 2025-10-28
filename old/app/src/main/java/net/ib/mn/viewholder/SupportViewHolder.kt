package net.ib.mn.viewholder

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.adapter.SearchedAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ItemSupportMainBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getAdDatePeriod
import java.text.NumberFormat
import java.util.Locale

class SupportViewHolder(
    val coroutineScope: CoroutineScope,
    val binding : ItemSupportMainBinding,
    val context: Context,
    private val mGlideRequestManager: RequestManager,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
    private final val onSupportButtonClick: (view: View, supportStatus: Int, model: SupportListModel) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    //타입 리스트
    private var typeList:ArrayList<SupportAdTypeListModel> = arrayListOf()
    private var adPeriod:String?=null

    @UnstableApi
    fun bind(item: SupportListModel, isShowViewMore: Boolean, supportListSize: Int, position: Int){


        when (position) {
            0 -> {//서포트
                if(supportListSize -1 == position) {
                    setMargins(itemView,0,30,0,30)
                } else {
                    setMargins(itemView,0,30,0,0)
                }
                binding.clSearchedSupport.visibility = View.VISIBLE           //메인 검색시 나오는  서포트 리스트 가장 맨위  서포트  문구들어있는  container
            }
            supportListSize - 1 -> {
                setMargins(itemView,0,0,0,30)
            }
            else -> {
                setMargins(itemView, 0, 0, 0, 0)
                binding.clSearchedSupport.visibility = View.GONE
            }
        }


        binding.photoBorder.bringToFront()
        binding.success.bringToFront()

        getTypeList(item, binding.adType)

        coroutineScope.launch(Dispatchers.IO) {
            item.idol = getIdolByIdUseCase(item.idol_id)
                .mapDataResource { it?.toPresentation() }
                .awaitOrThrow()!!
            withContext(Dispatchers.Main) {
                UtilK.setName(context, item.idol, binding.mostIdolName, binding.mostIdolGroup)
            }
        }

        //서포트 타이틀
        binding.title.text = item.title

        //썸네일  Webp로  사이즈 축소
        val lowestSizeImageUrl = UtilK.supportImageUrl(context, item.id)

        val idolId = item.idol_id
        //서포트 프로필
        mGlideRequestManager
            .load(lowestSizeImageUrl)//썸네일 url  o_st 로 오는걸  s_st로  변환한다.
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(idolId))
            .fallback(Util.noProfileImage(idolId))
            .placeholder(Util.noProfileImage(idolId))
            .dontAnimate()
            .into(binding.photo)

        //서포트 진행 % 보여주기
        val percentage = (((item.diamond.toDouble() / item.goal.toDouble()) * 100.0)).toInt()
        binding.tvAchievement.text = String.format(context.getString(R.string.support_achievement), percentage, LocaleUtil.getAppLocale(itemView.context))

        //광고 게시기간  설정

        // 각 서포트 상태별 뷰 처리
        when (item.status) {
            0 -> {//진행중
                with(binding){
                    inProgressLi.background = ContextCompat.getDrawable(context, R.drawable.bg_radius_brand500)
                    photoBorder.visibility = View.INVISIBLE
                    success.visibility = View.INVISIBLE
                    inProgressLi.visibility = View.VISIBLE
                    tvAchievement.visibility = View.VISIBLE
                    tvAchievement.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
                    binding.btnIntoSupportDetail.setImageResource(R.drawable.icon_main_arrow)
                    ad.text =   "${UtilK.getKSTDateString(item.created_at, itemView.context)} ~ ${UtilK.getKSTDateString(item.expired_at, itemView.context)}"
                }

                //전체 아이템 클릭 -> 실패 서포트
                binding.supportItemMainCon.setOnClickListener{
                    onSupportButtonClick(it, SearchedAdapter.SUPPORT_ING,item)
                }

                binding.articleResultRl.visibility = View.GONE
            }
            1 -> {//모금 성공
                binding.photoBorder.visibility = View.VISIBLE
                binding.photoBorder.setImageResource(R.drawable.img_success)
                binding.success.text = context.getString(R.string.support_success)
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
                binding.inProgressLi.visibility = View.GONE
                binding.tvAchievement.visibility = View.GONE
                binding.btnIntoSupportDetail.setImageResource(R.drawable.icon_main_arrow)
                binding.ad.text = "${String.format(context.resources.getString(R.string.format_include_date), UtilK.getKSTDateString(item.d_day, itemView.context) , adPeriod)}"

                //광고 좋아요 댓글.
                binding.articleResultRl.visibility = View.VISIBLE
                val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                binding.tvLikeCount.text = numberFormat.format(item.article.heart)
                binding.tvCommentCount.text = numberFormat.format(item.article.commentCount)

                //전체 아이템 클릭 ->  성공 서포트
                binding.supportItemMainCon.setOnClickListener{
                    onSupportButtonClick(it, SearchedAdapter.SUPPORT_SUCCESS,item)
                }
            }
        }


        if(supportListSize <= 3 ) {
            //서포트 리스트 더 보기 관련 처리
            binding.viewMoreSupportList.visibility = if (isShowViewMore) View.VISIBLE else View.GONE
        }else{
            binding.viewMoreSupportList.visibility = View.GONE
        }

        //더보기 클릭 - supportstatus 필요없어서  -1 (mock 값 줌)
        //더보기 뷰  없애줌.
        binding.viewMoreSupportList.setOnClickListener{
            onSupportButtonClick(it, -1,item)
        }
    }

    //뷰의 마진을 코드로 설정하기 위한  기능
    private fun setMargins(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    //타입 리스트
    private fun getTypeList(model: SupportListModel, adType: AppCompatTextView) {
        try{
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<SupportAdTypeListModel>>() {}.type
            typeList = gson.fromJson(Util.getPreference(context, Const.AD_TYPE_LIST), listType)

            for(i in 0 until typeList.size){
                if(model.type_id == typeList[i].id){
                    adPeriod = typeList[i].period.getAdDatePeriod(itemView.context)
                    adType.text = typeList[i].name
                }
            }
        }catch (e: IllegalStateException){
            e.printStackTrace()
        }
    }

}