package net.ib.mn.di

import android.content.Context
import net.ib.mn.data.repository.AuthRepository
import net.ib.mn.data.remote.api.AdApi
import net.ib.mn.data.remote.api.ArticlesApi
import net.ib.mn.data.remote.api.AwardsApi
import net.ib.mn.data.remote.api.ChartsApi
import net.ib.mn.data.remote.api.ChatApi
import net.ib.mn.data.remote.api.CommentsApi
import net.ib.mn.data.remote.api.ConfigsApi
import net.ib.mn.data.remote.api.EmoticonApi
import net.ib.mn.data.remote.api.FavoritesApi
import net.ib.mn.data.remote.api.FilesApi
import net.ib.mn.data.remote.api.FriendsApi
import net.ib.mn.data.remote.api.HeartpickApi
import net.ib.mn.data.remote.api.IdolApi
import net.ib.mn.data.remote.api.ImagesApi
import net.ib.mn.data.remote.api.MarketApi
import net.ib.mn.data.remote.api.MessageApi
import net.ib.mn.data.remote.api.MiscApi
import net.ib.mn.data.remote.api.ImagepickApi
import net.ib.mn.data.remote.api.ReportApi
import net.ib.mn.data.remote.api.ScheduleApi
import net.ib.mn.data.remote.api.SearchApi
import net.ib.mn.data.remote.api.StampsApi
import net.ib.mn.data.remote.api.ThemepickApi
import net.ib.mn.data.remote.api.TrendsApi
import net.ib.mn.data.remote.api.UserApi
import net.ib.mn.data.remote.api.UsersApi
import net.ib.mn.data.remote.api.UtilityApi
import net.ib.mn.data.remote.api.WikiApi
import net.ib.mn.data.remote.interceptor.AuthInterceptor
import net.ib.mn.data.remote.interceptor.GcodeInterceptor
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
        authRepository: AuthRepository
    ): AuthInterceptor = AuthInterceptor(context, authRepository)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    fun provideGcodeInterceptor(): GcodeInterceptor = GcodeInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        gcodeInterceptor: GcodeInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(Constants.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(Constants.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(gcodeInterceptor)  // gcode 88888 (점검 상태) 감지
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
        return Retrofit.Builder()
            .baseUrl(net.ib.mn.util.ServerUrl.BASE_URL)
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
    fun provideImagepickApi(retrofit: Retrofit): ImagepickApi =
        retrofit.create(ImagepickApi::class.java)

    @Provides
    @Singleton
    fun provideFavoritesApi(retrofit: Retrofit): FavoritesApi =
        retrofit.create(FavoritesApi::class.java)

    @Provides
    @Singleton
    fun provideArticlesApi(retrofit: Retrofit): ArticlesApi =
        retrofit.create(ArticlesApi::class.java)

    @Provides
    @Singleton
    fun provideMiscApi(retrofit: Retrofit): MiscApi =
        retrofit.create(MiscApi::class.java)

    @Provides
    @Singleton
    fun provideStampsApi(retrofit: Retrofit): StampsApi =
        retrofit.create(StampsApi::class.java)

    @Provides
    @Singleton
    fun provideWikiApi(retrofit: Retrofit): WikiApi =
        retrofit.create(WikiApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideScheduleApi(retrofit: Retrofit): ScheduleApi =
        retrofit.create(ScheduleApi::class.java)

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi =
        retrofit.create(UsersApi::class.java)

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApi =
        retrofit.create(ReportApi::class.java)

    @Provides
    @Singleton
    fun provideFriendsApi(retrofit: Retrofit): FriendsApi =
        retrofit.create(FriendsApi::class.java)

    @Provides
    @Singleton
    fun provideAwardsApi(retrofit: Retrofit): AwardsApi =
        retrofit.create(AwardsApi::class.java)

    @Provides
    @Singleton
    fun provideTrendsApi(retrofit: Retrofit): TrendsApi =
        retrofit.create(TrendsApi::class.java)

    @Provides
    @Singleton
    fun provideMarketApi(retrofit: Retrofit): MarketApi =
        retrofit.create(MarketApi::class.java)

    @Provides
    @Singleton
    fun provideCommentsApi(retrofit: Retrofit): CommentsApi =
        retrofit.create(CommentsApi::class.java)

    @Provides
    @Singleton
    fun provideEmoticonApi(retrofit: Retrofit): EmoticonApi =
        retrofit.create(EmoticonApi::class.java)

    @Provides
    @Singleton
    fun provideFilesApi(retrofit: Retrofit): FilesApi =
        retrofit.create(FilesApi::class.java)

    @Provides
    @Singleton
    fun provideImagesApi(retrofit: Retrofit): ImagesApi =
        retrofit.create(ImagesApi::class.java)

    @Provides
    @Singleton
    fun provideSearchApi(retrofit: Retrofit): SearchApi =
        retrofit.create(SearchApi::class.java)
}
