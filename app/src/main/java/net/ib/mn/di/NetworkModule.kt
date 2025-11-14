package net.ib.mn.di

import android.content.Context
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.api.*
import net.ib.mn.data.remote.interceptor.AuthInterceptor
import net.ib.mn.util.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): AuthInterceptor = AuthInterceptor(context, preferencesManager)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(Constants.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(Constants.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        // ServerUrl.BASE_URL을 사용하여 동적으로 변경 가능하도록 수정
        // old 프로젝트처럼 런타임에 서버 URL 변경 지원
        val baseUrl = net.ib.mn.util.ServerUrl.BASE_URL
        android.util.Log.d("NetworkModule", "========================================")
        android.util.Log.d("NetworkModule", "🌐 Creating Retrofit singleton instance")
        android.util.Log.d("NetworkModule", "  - BASE_URL: $baseUrl")
        android.util.Log.d("NetworkModule", "  - ServerUrl.HOST: ${net.ib.mn.util.ServerUrl.HOST}")
        android.util.Log.d("NetworkModule", "========================================")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideConfigsApi(retrofit: Retrofit): ConfigsApi =
        retrofit.create(ConfigsApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideIdolApi(retrofit: Retrofit): IdolApi =
        retrofit.create(IdolApi::class.java)

    @Provides
    @Singleton
    fun provideAdApi(retrofit: Retrofit): AdApi =
        retrofit.create(AdApi::class.java)

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi =
        retrofit.create(MessageApi::class.java)

    @Provides
    @Singleton
    fun provideUtilityApi(retrofit: Retrofit): UtilityApi =
        retrofit.create(UtilityApi::class.java)

    @Provides
    @Singleton
    fun provideChartsApi(retrofit: Retrofit): ChartsApi =
        retrofit.create(ChartsApi::class.java)

    @Provides
    @Singleton
    fun provideHeartpickApi(retrofit: Retrofit): HeartpickApi =
        retrofit.create(HeartpickApi::class.java)

    @Provides
    @Singleton
    fun provideThemepickApi(retrofit: Retrofit): ThemepickApi =
        retrofit.create(ThemepickApi::class.java)

    @Provides
    @Singleton
    fun provideOnepickApi(retrofit: Retrofit): OnepickApi =
        retrofit.create(OnepickApi::class.java)

    @Provides
    @Singleton
    fun provideFavoritesApi(retrofit: Retrofit): FavoritesApi =
        retrofit.create(FavoritesApi::class.java)
}
