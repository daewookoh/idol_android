package net.ib.mn.core.admob

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.NativeAdOptions
import net.ib.mn.core.admob.databinding.GntRadiusBannerBinding

@Composable
fun GntBannerWithRadius(
    modifier: Modifier = Modifier,
    adUnitId: String,
    loadSuccess : (Boolean) -> Unit = {}
) {
    AndroidViewBinding(
        factory = GntRadiusBannerBinding::inflate,
    ) {
        val adView = root.also {
            it.headlineView = this.tvHeadLine
            it.bodyView = this.tvBody
        }

        val adLoader = AdLoader.Builder(adView.context, adUnitId).forNativeAd { nativeAd ->

            with(nativeAd) {
                headline?.let {
                    tvHeadLine.text = it
                }
                body?.let {
                    tvBody.text = it
                }

                // You cannot start a load for a destroyed activity 방지
                val context = root.context
                if(context is Activity && !context.isDestroyed && !context.isFinishing) {
                    icon?.let {
                        Glide.with(context)
                            .load(it.uri)
                            .transform(CenterCrop(), RoundedCorners(26))
                            .into(ivIcon)
                    }
                }
            }

            adView.setNativeAd(nativeAd)
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                loadSuccess(true)
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                loadSuccess(false)

                if (!BuildConfig.DEBUG) {
                    return
                }

                Toast.makeText(
                    adView.context,
                    "Failed to load ad: ${p0.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }).withNativeAdOptions(
            NativeAdOptions.Builder()
                .setAdChoicesPlacement(
                    NativeAdOptions.ADCHOICES_TOP_LEFT
                )
                .build()
        ).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}