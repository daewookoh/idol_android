package net.ib.mn.onepick

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import net.ib.mn.R
import net.ib.mn.databinding.ItemThemePickBlurViewpagerItemsBinding
import net.ib.mn.databinding.ItemThemePickViewpagerItemsBinding
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK

class ThemepickViewPagerAdapter(private val context: Context,
                                private var mItems: ArrayList<ThemepickRankModel>,
                                private val glideRequestManager: RequestManager,
                                private val mTheme: ThemepickModel,
                                private val isPrelaunch: Boolean = false,
                                private val onItemClick: (ThemepickRankModel, View, Int, View) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface onItemClickListener{
        fun onItemClick(model: ThemepickRankModel, ivView:View, position: Int, ibView: View)
    }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when(viewType){
                TYPE_SQUARE -> {
                    val binding = ItemThemePickViewpagerItemsBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    ThemeViewPagerHolder(binding)
                }
                else -> {
                    val binding = ItemThemePickBlurViewpagerItemsBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    ThemeBlurViewPagerHolder(binding)
                }
            }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(mItems.size > 0){ //indexOutOfBoundsException방지.
            if(holder.itemViewType ==  TYPE_SQUARE){
                (holder as ThemeViewPagerHolder).apply {
                    bind(mItems[position % mItems.size], position)
                }
            }else{
                (holder as ThemeBlurViewPagerHolder).apply {
                    bind(mItems[position % mItems.size], position)
                }
            }
        }
    }

    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun getItemViewType(position: Int): Int {
        return if(mTheme.image_ratio == "S"){
            TYPE_SQUARE
        }else{
            TYPE_RECTANGLE
        }

    }
    override fun getItemId(position: Int): Long {
        return mItems[position].hashCode().toLong()
    }


    //테마픽 1:1 viewPager
    inner class ThemeViewPagerHolder(val binding: ItemThemePickViewpagerItemsBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ThemepickRankModel, position: Int) = with(binding) {

            //상단 이미지.
            if (model.imageUrl == null){
                glideRequestManager.load(Util.noProfileThemePickImage(model.id))
                    .transform(CenterCrop())
                    .into(ivThemePickInnerViewpager)
            } else {
                val imageUrl = UtilK.themePickImageUrl(context, model.id, mTheme.dummy)

                glideRequestManager.load(imageUrl)
                    .transform(CenterCrop())
                    .error(Util.noProfileThemePickImage(model.id))
                    .fallback(Util.noProfileThemePickImage(model.id))
                    .placeholder(Util.noProfileThemePickImage(model.id))
                    .into(ivThemePickInnerViewpager)
            }

            //선택되어있으면 클릭 가능하게 해주고 버튼 보여줌.
            if(model.isSelected){
                val listener = View.OnClickListener { view ->
                    onItemClick(model, view, position, ibThemePickInnerViewpager)
                }
                ivThemePickInnerViewpager.setOnClickListener(listener)
                ibThemePickInnerViewpager.visibility = View.VISIBLE
                ivThemePickInnerViewpager.colorFilter = null
            }
            else{//선택이 안되어있으면 클릭 못하게해주고. 버튼 없애줌.
                ivThemePickInnerViewpager.setOnClickListener(null)
                ibThemePickInnerViewpager.visibility = View.GONE
                ivThemePickInnerViewpager.setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY)
            }

            if (isPrelaunch) {
                ibThemePickInnerViewpager.visibility = View.GONE
            }

            //뷰페이저 드래그될때마다 바뀌니까 넣어준다.
            ibThemePickInnerViewpager.isSelected = model.isClicked
        }
    }


    //Themepick 1:1 아닌 viewPager
    inner class ThemeBlurViewPagerHolder(val binding: ItemThemePickBlurViewpagerItemsBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(model: ThemepickRankModel, position: Int) = with(binding) {
            //상단 이미지.
            if (model.imageUrl == null){
                glideRequestManager.load(Util.noProfileThemePickImage(model.id))
                    .transform(CenterCrop(), RoundedCorners(26))
                    .into(ivThemePickInnerViewpager)
            } else {
                val imageUrl = UtilK.themePickImageUrl(context, model.id, mTheme.dummy)

                // BlurTransformation이 Glide 먹통현상을 발생시켜 변경
//                glideRequestManager
//                    .asBitmap()
//                    .load(imageUrl)
//                    .error(Util.noProfileThemePickImage(model.id))
//                    .fallback(Util.noProfileThemePickImage(model.id))
//                    .placeholder(Util.noProfileThemePickImage(model.id))
//                    .transform(CenterCrop(), BlurTransformation(91))
//                    .into(ivBlur)
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    glideRequestManager
                        .asBitmap()
                        .load(imageUrl)
                        .error(Util.noProfileThemePickImage(model.id))
                        .into(ivBlur)
                    val blurRenderEffect = RenderEffect.createBlurEffect(
                        Const.BLUR_SIZE, Const.BLUR_SIZE,
                        Shader.TileMode.MIRROR
                    )
                    ivBlur.setRenderEffect(blurRenderEffect)
                } else {
                    glideRequestManager
                        .asBitmap()
                        .load(imageUrl)
                        .error(Util.noProfileThemePickImage(model.id))
                        .into(object: CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                val blurred = ImageUtil.blur(itemView.context, resource, 30)
                                ivBlur.setImageBitmap(blurred)
                            }
                        })
                }

                glideRequestManager.load(imageUrl)
                    .transform(CenterInside())
                    .into(ivThemePickInnerViewpager)

                ivThemePickInnerViewpager.bringToFront()
                ibThemePickInnerViewpager.bringToFront()

            }

            //선택되어있으면 클릭 가능하게 해주고 버튼 보여줌.
            if(model.isSelected){
                val listener = View.OnClickListener { view ->
                    onItemClick(model, view, position, ibThemePickInnerViewpager)
                }
                ivThemePickInnerViewpager.setOnClickListener(listener)
                ibThemePickInnerViewpager.visibility = View.VISIBLE
                ivThemePickInnerViewpager.colorFilter = null
            }
            else{//선택이 안되어있으면 클릭 못하게해주고. 버튼 없애줌.
                ivThemePickInnerViewpager.setOnClickListener(null)
                ibThemePickInnerViewpager.visibility = View.GONE
                ivThemePickInnerViewpager.setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY)
            }

            if (isPrelaunch) {
                ibThemePickInnerViewpager.visibility = View.GONE
            }

            //뷰페이저 드래그될때마다 바뀌니까 넣어준다.
            ibThemePickInnerViewpager.isSelected = model.isClicked
        }

    }

    companion object{
        val TYPE_SQUARE = 0
        val TYPE_RECTANGLE = 1
    }

}