package net.ib.mn.di

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * ExoPlayer 캐시 모듈
 *
 * 비디오 재생을 위한 캐시 설정을 제공합니다.
 * - SimpleCache: 비디오 파일을 디스크에 캐싱
 * - CacheDataSource: 캐시를 사용하는 DataSource Factory
 */
@Module
@InstallIn(SingletonComponent::class)
object ExoPlayerModule {

    private const val CACHE_SIZE = 100L * 1024 * 1024 // 100MB
    private const val CACHE_DIR_NAME = "exoplayer_cache"

    /**
     * ExoPlayer용 SimpleCache 제공
     * - 100MB 크기 제한
     * - LRU 정책으로 오래된 캐시 자동 삭제
     */
    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val databaseProvider = StandaloneDatabaseProvider(context)
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)

        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    /**
     * 캐시를 사용하는 DataSource.Factory 제공
     */
    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        simpleCache: SimpleCache
    ): CacheDataSource.Factory {
        // HTTP DataSource (네트워크에서 비디오 다운로드)
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("IdolApp/1.0")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)

        // Cache DataSource (캐시 + 네트워크)
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // 기본 캐시 쓰기 사용
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
