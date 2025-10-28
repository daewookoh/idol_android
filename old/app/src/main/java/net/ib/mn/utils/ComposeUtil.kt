package net.ib.mn.utils

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.skydoves.landscapist.coil.CoilImage
import net.ib.mn.R

@Composable
fun NetworkImage(
    context: Context,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val placeholderImage = R.drawable.menu_profile_default2

    CoilImage(
        imageRequest = {
            ImageRequest.Builder(context)
                .data(imageUrl.takeIf { !it.isNullOrEmpty() } ?: placeholderImage)
                .crossfade(true)
                .scale(Scale.FILL)
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .placeholder(placeholderImage) // 로딩 중 기본 이미지 표시
                .error(placeholderImage) // 에러 시 기본 이미지 표시
                .build()
        },
        imageLoader = {
            ImageLoader.Builder(context)
                .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
                .crossfade(true)
                .build()
        },
        modifier = modifier,
    )
}

@Composable
fun NetworkImageScaleFit(
    context: Context,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val placeholderImage = R.drawable.menu_profile_default2

    CoilImage(
        imageRequest = {
            ImageRequest.Builder(context)
                .data(imageUrl.takeIf { !it.isNullOrEmpty() } ?: placeholderImage)
                .crossfade(true)
                .scale(Scale.FIT)
                .networkCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .placeholder(placeholderImage) // 로딩 중 기본 이미지 표시
                .error(placeholderImage) // 에러 시 기본 이미지 표시
                .build()
        },
        imageLoader = {
            ImageLoader.Builder(context)
                .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.25).build() }
                .crossfade(true)
                .build()
        },
        modifier = modifier
            .fillMaxSize(),
    )
}