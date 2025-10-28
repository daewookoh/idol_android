package net.ib.mn

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Priority
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class CustomGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val diskCacheSizeBytes = 100 * 1024 * 1024 // 100MB
        builder.setDiskCache(
            DiskLruCacheFactory(
                context.cacheDir.absolutePath + "/glide_cache",
                diskCacheSizeBytes.toLong()
            )
        )

        // 기본 RequestOptions 설정
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .skipMemoryCache(false)
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}