/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 여러개 이미지/gif일 경우 보여주는 WidePhotoFragment
 *
 * */

package net.ib.mn.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import net.ib.mn.R
import net.ib.mn.adapter.PhotoPagerAdapter
import net.ib.mn.databinding.FragmentMultiWidePhotoBinding
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.asyncPopBackStack
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import java.text.NumberFormat
import java.util.Locale

open class MultiWidePhotoFragment : BaseWidePhotoFragment(), View.OnClickListener {
    private lateinit var binding: FragmentMultiWidePhotoBinding

    private var itemPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMultiWidePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        getDataFromVM()
    }

    private fun init() {
        if (this.arguments == null) activity?.onBackPressed()
        itemPosition = this.requireArguments().getInt(PARAM_POSITION, 0)

        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(requireContext()))
        val fileCount = numberFormat.format(baseWidePhotoViewModel.articleModel.files.size)

        binding.vpPhoto.apply {
            adapter = PhotoPagerAdapter(context, baseWidePhotoViewModel.articleModel.files)
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            setCurrentItem(itemPosition, false)
        }
        binding.tvPhotoIdx.text = numberFormat.format(itemPosition+1).plus("/").plus(fileCount)

        // 커뮤니티 게시물 목록으로 이동함. -> 다시 여기로 이동
        binding.btnHeartBox.visibility = if (Util.getPreferenceBool(
                activity,
                Const.PREF_HEART_BOX_VIEWABLE,
                false
            ) && !isAggregatingTime
        ) View.VISIBLE else View.INVISIBLE

        with(binding) {
            btnHeartBox.setOnClickListener(this@MultiWidePhotoFragment)
            btnDownload.setOnClickListener(this@MultiWidePhotoFragment)
            btnClose.setOnClickListener(this@MultiWidePhotoFragment)
            vpPhoto.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    itemPosition = position
                    binding.tvPhotoIdx.text = numberFormat.format(itemPosition+1).plus("/").plus(fileCount)
                }
            })
            if(!baseWidePhotoViewModel.articleModel.files.isNullOrEmpty()){
                btnShare.visibility = View.VISIBLE
                btnShare.setOnClickListener(this@MultiWidePhotoFragment)
            } else {
                btnShare.visibility = View.GONE
            }
        }
        baseWidePhotoViewModel.addAdManagerView(context)
    }

    private fun getDataFromVM() {
        baseWidePhotoViewModel.adManagerAdView.observe(
            this,
            SingleEventObserver { adManagerAdView ->
                binding.admobNativeAdContainer.addView(adManagerAdView)
            },
        )
    }

    override fun onDestroyView() {
        binding.admobNativeAdContainer.removeAllViews()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnHeartBox -> {
                binding.btnHeartBox.visibility = View.INVISIBLE
                Util.showProgress(activity)
                mHeartBoxSendHandler.sendEmptyMessageDelayed(0, 1000)
            }

            binding.btnDownload -> {
                setUiActionFirebaseGoogleAnalyticsDialogFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "community_widephoto_download",
                )
                if (!Util.isSdPresent()) {
                    if (activity != null && isAdded) {
                        Toast.makeText(
                            activity,
                            getString(R.string.msg_unable_use_download_1),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29 이상: MediaStore 사용
                    baseWidePhotoViewModel.downloadImage(
                        requireContext(),
                        baseWidePhotoViewModel.getImageUrl(itemPosition) ?: ""
                    )
                } else {
                    baseWidePhotoViewModel.requestPermission(
                        this,
                        baseActivity,
                        targetSdkVersion,
                        itemPosition
                    )
                }

                if(!baseWidePhotoViewModel.articleModel.id.isNullOrEmpty()) {
                    baseWidePhotoViewModel.downloadCount(context, baseWidePhotoViewModel.articleModel.id.toLong(), itemPosition + 1)
                }
            }

            binding.btnClose -> {
                activity?.supportFragmentManager?.asyncPopBackStack {
                    if (dialog != null && dialog!!.isShowing) {
                        dismiss()
                    }
                }
            }
            binding.btnShare -> {
                setUiActionFirebaseGoogleAnalyticsDialogFragment(
                    GaAction.COMMENT_ARTICLE_SHARE.actionValue,
                    GaAction.COMMENT_ARTICLE_SHARE.label
                )
                val url = LinkUtil.getAppLinkUrl(
                    context = context ?: return,
                    params = listOf(
                        LinkStatus.ARTICLES.status,
                        baseWidePhotoViewModel.articleModel.id.toString()
                    )
                )
                UtilK.linkStart(
                    context = context,
                    url = url
                )
            }
        }
    }

    companion object {
        private const val PARAM_IS_SMALL_TALK = "param_is_small_talk"
        private const val PARAM_POSITION = "param_position"

        fun getInstance(model: ArticleModel, position:Int = 0, isSmallTalke: Boolean = false): MultiWidePhotoFragment {
            val fragment = MultiWidePhotoFragment()
            val args = Bundle()
            args.putSerializable(PARAM_MODEL, model)
            args.putBoolean(PARAM_IS_SMALL_TALK, isSmallTalke)
            args.putInt(PARAM_POSITION, position)
            fragment.arguments = args

            return fragment
        }
    }
}