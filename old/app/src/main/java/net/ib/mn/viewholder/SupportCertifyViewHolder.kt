package net.ib.mn.viewholder

import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.databinding.ItemSupportPhotoBinding
import net.ib.mn.model.SupportListModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getAdDatePeriod
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SupportCertifyViewHolder(
    val binding: ItemSupportPhotoBinding,
    private val mGlideRequestManager: RequestManager,
    private val itemCount: Int,
    private var getVideoPlayView: NewCommentAdapter.GetVideoPlayView?,
    private val supportInfo: JSONObject,
    private val supportListModel: SupportListModel
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(supportInfoJson: JSONObject, supportListModel: SupportListModel) {
        with(binding) {
            ivAdTypeList.visibility = View.INVISIBLE

            // 한국어일때 DATE_FIELD값이 이상하게 나와서 MEDIUM으로 바꿈.
            val locale = LocaleUtil.getAppLocale(itemView.context)
            val formatter = if (LocaleUtil.getAppLocale(itemView.context) == Locale.KOREA) {
                DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            } else {
                DateFormat.getDateInstance(DateFormat.DATE_FIELD, locale)
            }
            val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
            val dateFormat = SimpleDateFormat(localPattern, locale)

            tvIdolName.text = checkValueExist(supportInfoJson, "name")
            tvIdolGroup.text = checkValueExist(supportInfoJson, "group")
            tvSupportTitle.text = checkValueExist(supportInfoJson, "title")

            val dateString = UtilK.getKSTDateString(supportListModel.d_day, itemView.context)
            tvAdPeriod.text = String.format(
                itemView.context.getString(R.string.format_include_date),
                dateString,
                supportListModel.type.period.getAdDatePeriod(root.context)
            )

            tvAdTypeUp.text = supportListModel.type.name

            //모금기간.
            val createdAt = UtilK.getKSTDateString(supportListModel.created_at, itemView.context)
            val expiredAt = UtilK.getKSTDateString(supportListModel.expired_at, itemView.context)

            tvDesignPeriod.text = String.format("%s ~ %s", createdAt, expiredAt)

            if (supportListModel.article.content.isNullOrEmpty()) {
                tvAdLocation.visibility = View.GONE
            } else {
                tvAdLocation.visibility = View.VISIBLE
                tvAdLocation.text = supportListModel.article.content
            }

            binding.layoutCheckLocation.visibility =
                if (supportListModel.type.locationImageUrl == null && supportListModel.type.locationMapUrl == null) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            val idolId = supportListModel.idol.getId()
            mGlideRequestManager
                .load(checkValueExist(supportInfoJson, "profile_img_url"))
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(imgIdolProfile)


            if (supportListModel.article.heart > 0) {
                likeCount.text = supportListModel.article.heart.toString()
            } else {
                if (supportListModel.like) {
                    //-로 나오는 경우에서  좋아요를 눌렀던 상황이라면 +1
                    likeCount.text = 1.toString()
                } else {
                    //-로 나오는 경우는  일단  유저에게는  0으로 보이게 조치
                    likeCount.text = 0.toString()
                }

            }

            //댓글 숫자 적용
            commentCount.text = supportListModel.article.commentCount.toString()

            toggleLikeIcon(supportListModel.like)

            when {
                supportListModel.article.imageUrl != null -> {

                    supportListModel.article.imageUrl?.apply {

                        //이미지일때
                        if (endsWith(".png") || endsWith(".jpg")) {

                            videoSupportResult.visibility = View.GONE

                            //glide 이미지  적용할때도 프로그래스 보여줌.
                            progressBar.visibility = View.VISIBLE

                            //유저 프로필
                            mGlideRequestManager
                                .load(supportListModel.article.imageUrl)
                                .error(R.drawable.img_support_no_photo)
                                .fallback(R.drawable.img_support_no_photo)
                                .placeholder(R.drawable.img_support_no_photo)
                                .dontAnimate()
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable>,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        model: Any,
                                        target: Target<Drawable>,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {

                                        //이미지 로드 가능하므로, 서포트이미지뷰  visible 처리
                                        imgSupportResult.visibility = View.VISIBLE

                                        //이미지 로드완료되면  프로그래스 gone처리
                                        progressBar.visibility = View.GONE
                                        return false
                                    }
                                }).into(imgSupportResult)

                            isCertifyMediaExist(true)

                        } else {
                            isCertifyMediaExist(false)
                        }
                    }
                }

                supportListModel.article.umjjalUrl != null -> {
                    supportListModel.article.umjjalUrl?.apply {
                        if (endsWith(".mp4")) {//영상일때
                            imgSupportResult.visibility = View.INVISIBLE
                            videoSupportResult.visibility = View.VISIBLE
                            getVideoPlayView?.getExoVideoPlayView(
                                videoSupportResult,
                                null, null,
                                supportListModel.article.umjjalUrl
                            )
                            //인증 미디어 url 이 있으니까 -> 인증샷 준비중  멘트  Invisible로
                            isCertifyMediaExist(true)
                        } else {
                            isCertifyMediaExist(false)
                        }
                    }
                }

                else -> {
                    isCertifyMediaExist(false)
                }
            }
        }
    }

    //인증샷 여부에따라  인증샷 화면 header 의  뷰들을 분기해서 보여준다.
    private fun isCertifyMediaExist(exist: Boolean) {
        with(binding) {
            if (exist) {
                tvTitleDesignPeriod.visibility = View.GONE
                tvDesignPeriod.visibility = View.GONE
                tvForEmptyPhoto.visibility = View.GONE
                ivAdTypeList.visibility = View.GONE
                tvExpandTouch.visibility = View.GONE
            } else {
                //인증샷 없으니까  인증샷 준비중 멘트 visible로 처리
                tvTitleDesignPeriod.visibility = View.VISIBLE
                tvDesignPeriod.visibility = View.VISIBLE
                tvForEmptyPhoto.visibility = View.VISIBLE

                //xml에 적용해놓으니까  default로 보이는경우가 있어서
                //미디어가 없을때 체크해서  넣어줌.
                mGlideRequestManager
                    .load(R.drawable.img_support_no_photo)
                    .into(imgSupportResult)

                mGlideRequestManager
                    .load(if (BuildConfig.CELEB) R.drawable.icon_my_question_mark else R.drawable.icon_my_help)
                    .into(ivAdTypeList)

                ivAdTypeList.visibility = View.VISIBLE
                tvExpandTouch.visibility = View.VISIBLE
            }
        }
    }

    fun toggleLikeIcon(isLike: Boolean) {
        val likeImageDrawable = if (isLike) {
            R.drawable.icon_board_like_active
        } else {
            R.drawable.icon_board_like
        }
        binding.imgSupportLikeIcon.setImageResource(likeImageDrawable)
    }

    //json으로 넘어오는 value들 중에  null 값 여부 판단후 해당 값 또는 빈값을  return
    private fun checkValueExist(checkJson: JSONObject, checkvalue: String): String {
        return if (checkJson.has(checkvalue)) {
            return if (supportInfo.getString(checkvalue).isEmpty()) {
                ""
            } else {
                supportInfo.getString(checkvalue)
            }
        } else {
            ""
        }
    }
}



