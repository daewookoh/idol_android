/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import feature.common.exodusimagepicker.util.Util
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.core.admob.GntBox
import net.ib.mn.core.model.EndPopupModel
import net.ib.mn.databinding.DialogBaseBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.ext.openAppOrStore

/**
 * @see
 * */

class AdBannerDialog(
    private val title: SpannableString,
    private val subTitle: String,
    private val btnOneText: String,
    private val btnTwoText: String,
    private val isVisibleTitle: Boolean,
    private val context: Context,
    theme: Int,
    private val endPopupModel: EndPopupModel? = null,
    private val isLocalAd: Boolean = false,
    private val onAdOpened: (Boolean) -> Unit = {},
    private val confirmClick: () -> Unit,
) : Dialog(context, theme) {

    lateinit var binding: DialogBaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.inflate<DialogBaseBinding?>(
            layoutInflater,
            R.layout.dialog_base,
            null,
            false,
        )

        setContentView(binding.root)
        init()

        reConfigurationDialog()
        setCanceledOnTouchOutside(false)

        binding.btnConfirm.setOnClickListener {
            confirmClick()
            dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun init() = with(binding) {
        val activity = (context as? Activity)
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            confirmClick()
            return
        }

        if (!isLocalAd) {
            composeAdBanner.setContent {
                GntBox(
                    adUnitId = if (BuildConfig.DEBUG) {
                        Const.ADMOB_NATIVE_AD_TEST_UNIT_ID
                    } else {
                        if (BuildConfig.CELEB) {
                            Const.ADMOB_NATIVE_AD_ACTOR_UNIT_ID
                        } else {
                            Const.ADMOB_NATIVE_AD_UNIT_ID
                        }
                    },
                    loadSuccess = { isSuccess ->

                        if (isSuccess) {
                            binding.ivReplacementAdUrl.visibility = View.INVISIBLE
                            binding.composeAdBanner.visibility = View.VISIBLE
                            return@GntBox
                        }

                        binding.composeAdBanner.visibility = View.INVISIBLE
                        binding.ivReplacementAdUrl.visibility = View.VISIBLE
                    },
                    onAdOpened = { isOpened ->
                        onAdOpened(isOpened)
                    }
                )
            }

            binding.progress.visibility = View.GONE
            
            Glide.with(context)
                .load(endPopupModel?.imageUrl)
                .into(binding.ivReplacementAdUrl)
        } else {
            if (BuildConfig.CHINA) {
                binding.clAdBannerImage.visibility = View.GONE
            } else {
                binding.progress.visibility = View.GONE
                binding.ivReplacementAdUrl.visibility = View.VISIBLE

                Glide.with(context)
                    .load(endPopupModel?.imageUrl ?: "")
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            AppCompatResources.getDrawable(context, R.drawable.img_popup_default)?.let {
                                binding.ivReplacementAdUrl.apply {
                                    setImageDrawable(it)
                                }
                            }
                            setBannerClickListener(false)
                            return true
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            setBannerClickListener(true)
                            return false
                        }

                    })
                    .into(binding.ivReplacementAdUrl)
            }

        }

        clAdBanner.setViewTreeLifecycleOwner(context as LifecycleOwner)
        composeAdBanner.setViewTreeLifecycleOwner(context as LifecycleOwner)
        composeAdBanner.setViewTreeSavedStateRegistryOwner(context as SavedStateRegistryOwner)

        if (!isVisibleTitle) {
            tvTitle.visibility = View.GONE
        }

        tvTitle.text = title
        btnConfirm.text = btnOneText
        btnCancel.text = btnTwoText
        tvSubTitle.text = subTitle
    }

    private fun reConfigurationDialog() {
        val layoutParamWindowManager = WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            dimAmount = 0.7f
            gravity = Gravity.CENTER
        }
        window?.attributes = layoutParamWindowManager
        window?.setLayout(
            Util.convertDpToPixel(context, 310f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun setBannerClickListener(hasUrlLoaded: Boolean = false) {
        if (!hasUrlLoaded) {
            return
        }

        binding.ivReplacementAdUrl.setOnClickListener {

            endPopupModel?.linkUrl?.let {
                if (it.contains("playfillit.com")) {
                    context.openAppOrStore()
                    return@setOnClickListener
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(endPopupModel.linkUrl))

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            } ?: return@setOnClickListener
        }
    }
}