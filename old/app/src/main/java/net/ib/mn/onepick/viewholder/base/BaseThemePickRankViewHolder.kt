/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.base

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.ImageUtil
import net.ib.mn.utils.Util

/**
 * @see
 * */

open class BaseThemePickRankViewHolder<VDB : ViewDataBinding> (
    themePickBinding: VDB,
    private val glideRequestManager: RequestManager,
) : RecyclerView.ViewHolder(themePickBinding.root) {
    private lateinit var voteBtn: AppCompatButton
    private lateinit var blurImage: AppCompatImageView
    private lateinit var idolImage: AppCompatImageView

    constructor(
        themePickBinding: VDB,
        glideRequestManager: RequestManager,
        voteBtn: AppCompatButton,
        blurImage: AppCompatImageView,
        idolImage: AppCompatImageView,
    ) : this(themePickBinding, glideRequestManager) {
        this.voteBtn = voteBtn
        this.blurImage = blurImage
        this.idolImage = idolImage
    }

    open fun bind(
        item: ThemepickRankModel,
        theme:ThemepickModel,
        position: Int,
    ) {
        setBlurImage(item)
        setIdolImage(item)
        setStatusVoteBtn(item, theme)
    }

    private fun setBlurImage(item: ThemepickRankModel) {
        if (item.imageUrl.isNullOrEmpty()) {
            return
        }

        // 아래 주석 지우지 마세요 (히스토리 보관용)
        // 여기서 문제 발생 -> BlurTransformation 사용이 누적되면 glide 자체가 멈추는 현상 발생
//        glideRequestManager
//            .asBitmap()
//            .load(item.imageUrl)
//            .transform(BlurTransformation(91))
//            .into(blurImage)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            glideRequestManager
                .asBitmap()
                .load(item.imageUrl)
                .error(Util.noProfileThemePickImage(item.id))
                .into(blurImage)
            val blurRenderEffect = RenderEffect.createBlurEffect(
                Const.BLUR_SIZE, Const.BLUR_SIZE,
                Shader.TileMode.MIRROR
            )
            blurImage.setRenderEffect(blurRenderEffect)
        } else {
            glideRequestManager
                .asBitmap()
                .load(item.imageUrl)
                .error(Util.noProfileThemePickImage(item.id))
                .into(object: CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val blurred = ImageUtil.blur(itemView.context, resource, 30)
                        blurImage.setImageBitmap(blurred)
                    }
                })
        }
    }

    private fun setIdolImage(item: ThemepickRankModel) {
        if (item.imageUrl.isNullOrEmpty()) {
            glideRequestManager.load(Util.noProfileThemePickImage(item.id))
                .transform(CenterCrop(), RoundedCorners(26))
                .into(idolImage)
            return
        }

        glideRequestManager.load(item.imageUrl)
            .transform(CenterInside())
            .error(Util.noProfileThemePickImage(item.id))
            .fallback(Util.noProfileThemePickImage(item.id))
            .placeholder(Util.noProfileThemePickImage(item.id))
            .into(idolImage)
    }

    private fun setStatusVoteBtn(item: ThemepickRankModel, mTheme: ThemepickModel) {
        when (mTheme.status) {
            ThemepickModel.STATUS_PROGRESS -> {
                if (mTheme.voteId == 0 && mTheme.vote == "N") {
                    voteBtn.visibility = View.GONE
                } else {
                    voteBtn.visibility = View.VISIBLE
                    if (mTheme.voteId == item.id) {
                        voteBtn.background = ContextCompat.getDrawable(
                            itemView.context,
                            if (BuildConfig.CELEB) R.drawable.btn_themapick_vote_celeb_selected_result else R.drawable.btn_themapick_vote_selected_result,
                        )
                    } else {
                        voteBtn.background = ContextCompat.getDrawable(
                            itemView.context,
                            R.drawable.btn_themapick_vote_gray_result,
                        )
                    }
                }
            }
            else -> {
                voteBtn.visibility = View.GONE
            }
        }
    }
}