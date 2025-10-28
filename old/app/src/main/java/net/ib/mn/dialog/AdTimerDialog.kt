/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 비디오 광고 타이머 팝업.
 *
 * */

package net.ib.mn.dialog

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.databinding.DialogRewardAdTimerBinding
import net.ib.mn.dialog.base.BaseDialog

/**
 * @see
 * */

class AdTimerDialog(
    private val title: String,
    private val message: String,
    private val context: Context,
    theme: Int,
    private val coroutineScope: LifecycleCoroutineScope,
    private val showAd: () -> Unit,
) : BaseDialog<DialogRewardAdTimerBinding>(context, theme,  R.layout.dialog_reward_ad_timer) {

    override fun DialogRewardAdTimerBinding.onCreate() {

        with(binding) {
            tvTitle.text = title
            tvMsg.text = message
            btnCancel.setOnClickListener {
                cancel()
            }
        }

        startTimer()
    }

    private fun startTimer() = coroutineScope.launch(Dispatchers.Default) {
        // 로딩을 위해 딜레이 살짝 넣어줌.
        delay(500L)

        val startTime = System.currentTimeMillis() + 3 * 1000

        var isContinue = true
        while (isContinue) {
            val currentTime = System.currentTimeMillis()
            val diffTIme = startTime - currentTime

            if (diffTIme < 0) isContinue = false

            binding.pgTimeLimit.progress = diffTIme.toInt()

            when (diffTIme) {
                in 2000L..3000L -> {
                    withContext(Dispatchers.Main) {
                        binding.tvProgress.text = "2s"
                    }
                }

                in 1000L..2000L -> {
                    withContext(Dispatchers.Main) {
                        binding.tvProgress.text = "1s"
                    }
                }

                in 0L..1000L -> {
                    withContext(Dispatchers.Main) {
                        binding.tvProgress.text = "0s"
                    }
                }

                else -> {
                    if (!isShowing) {
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        binding.tvProgress.text = "0s"
                        showAd()
                        this@AdTimerDialog.cancel()
                    }
                }
            }
        }
    }
}