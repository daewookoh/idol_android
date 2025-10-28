/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.activity

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.admanager.AdManager
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ext.applySystemBarInsets

/**
 * @see 셀럽 전용 게시판 화면이다 (셀럽 같은경우 탭이 존재 하지 않기때문에 나눠 놓는게 좋음)
 * */

@AndroidEntryPoint
open class BoardCelebActivity(@LayoutRes val layoutRes: Int) :
    BaseActivity(),
    BaseDialogFragment.DialogResultHandler {

    private var isPurchasedDailyPack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layoutRes)

        val layout = findViewById<ConstraintLayout>(R.id.cl_container)
        layout.applySystemBarInsets()

        val idolAccount = IdolAccount.getAccount(this)

        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        // 데일리팩 구독 여부에 따라 광고 세팅
        setPurchasedDailyPackFlag(idolAccount)
        if (!isPurchasedDailyPack) {
            val adManager = AdManager.getInstance()
            with(adManager) {
                setAdManagerSize(this@BoardCelebActivity, fragmentContainer)
                setAdManager(this@BoardCelebActivity)
                loadAdManager()
            }
        }
    }

    private fun setPurchasedDailyPackFlag(account: IdolAccount?) {
        isPurchasedDailyPack = false
        account?.userModel?.subscriptions?.forEach { mySubscription ->
            if (mySubscription.familyappId == 1 ||
                mySubscription.familyappId == 2 ||
                mySubscription.skuCode == Const.STORE_ITEM_DAILY_PACK
            ) {
                isPurchasedDailyPack = true
                return@forEach
            }
        }
    }

    override fun onDestroy() {
        AdManager.getInstance().adManagerView?.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        AdManager.getInstance().adManagerView?.pause()
        super.onPause()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ENTER_WIDE_PHOTO.value -> {
                if (resultCode == RESULT_CANCELED && !isPurchasedDailyPack) {
                    AdManager.getInstance().loadAdManager()
                }
            }
        }
    }
}