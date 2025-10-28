package net.ib.mn.viewholder

import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ItemSearchWallpaperBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.fragment.WidePhotoFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.WallpaperModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.RequestCode

class SearchedWallpaperViewHolder(
    val binding: ItemSearchWallpaperBinding,
    private val wallpaperModel: WallpaperModel,
    val lifecycleScope: LifecycleCoroutineScope,
    val getIdolByIdUseCase: GetIdolByIdUseCase
    ) : RecyclerView.ViewHolder(binding.root) {
    val glideRequestManager = Glide.with(itemView.context)

    fun bind(position: Int) {
        setImage(position)
        setMoreVisibility(position)
        onClick(position)
    }

    private fun setImage(position: Int) = with(binding) {
        glideRequestManager
            .load(wallpaperModel.imageUrls?.get(position)) // 썸네일 url  o_st 로 오는걸  s_st로  변환한다.
            .transform(CenterCrop(), RoundedCorners(30))
            .into(ivWallpaper)
    }

    private fun setMoreVisibility(position: Int) = with(binding) {
        if(wallpaperModel.imageUrls.isNullOrEmpty()) return@with

        // totalCount가 image_url size보다 크고, 마지막 이미지일 경우 옆에 더보기 보여줌
        clMore.visibility =
            if (wallpaperModel.imageUrls?.size == position + 1 && wallpaperModel.imageUrls?.size!! < wallpaperModel.totalCount) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun onClick(position: Int) = with(binding) {
        // 더보기 눌렀을 때
        clMore.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val idol = getIdolByIdUseCase(wallpaperModel.idolId)
                    .mapDataResource { it }
                    .awaitOrThrow()
                idol?.let {
                    itemView.context.startActivity(
                        // TODO unman idol model 제거
                        CommunityActivity.createIntent(itemView.context, idol.toPresentation(), isWallpaper = true)
                    )
                }
            }
        }

        // 이미지 눌렀을 때 ArticleModel 만들어서 widePhotoFragment로 이동
        clWallpaper.setOnClickListener {
            val model = ArticleModel()
            model.imageUrl = wallpaperModel.imageUrls?.get(position)
            WidePhotoFragment.getInstance(model)
                .apply { setActivityRequestCode(RequestCode.ENTER_WIDE_PHOTO.value) }
                .show((itemView.context as BaseActivity).supportFragmentManager, "wide_photo")
        }
    }
}