package net.ib.mn.util

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 미디어 캐시 유틸리티
 * Old 프로젝트의 CacheUtil과 동일한 기능
 * - 1GB 캐시 크기
 * - LRU 캐시 정책
 */
@OptIn(UnstableApi::class)
object MediaCacheUtil {

    private var simpleCache: SimpleCache? = null
    private const val CACHE_SIZE_BYTES = 1024L * 1024 * 1024 // 1GB

    fun getSimpleCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            simpleCache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
            )
        }
        return simpleCache!!
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getSimpleCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * URL에서 비디오 duration을 가져옴 (Old 프로젝트의 String.getDuration()과 동일)
     * MediaMetadataRetriever 사용
     * @return duration in milliseconds, null if failed
     */
    suspend fun getVideoDuration(url: String): Long? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(url, hashMapOf<String, String>())
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }
}
