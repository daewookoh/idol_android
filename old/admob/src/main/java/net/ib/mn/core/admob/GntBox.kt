package net.ib.mn.core.admob

import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAdOptions
import net.ib.mn.core.admob.databinding.GntBoxBinding

@Composable
fun GntBox(
    modifier: Modifier = Modifier,
    adUnitId: String,
    loadSuccess: (Boolean) -> Unit = {},
    onAdOpened : (Boolean) -> Unit = {},
) {
    AndroidViewBinding(
        factory = GntBoxBinding::inflate,
        modifier = Modifier
            .then(modifier),
    ) {
        val adView = root.also {
            it.mediaView = this.mediaView
        }

        val adLoader = AdLoader.Builder(adView.context, adUnitId).forNativeAd { nativeAd ->

            with(nativeAd) {
                mediaContent?.let {
                    mediaView.mediaContent = it
                    mediaView.setImageScaleType(ImageView.ScaleType.FIT_XY)
                }
            }

            adView.setNativeAd(nativeAd)
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                layoutAd.visibility = View.VISIBLE
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
                    Toast.LENGTH_SHORT,
                ).show()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                onAdOpened(true)
            }
        }).withNativeAdOptions(
            NativeAdOptions.Builder()
                .setAdChoicesPlacement(
                    NativeAdOptions.ADCHOICES_TOP_RIGHT,
                )
                .build(),
        ).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}