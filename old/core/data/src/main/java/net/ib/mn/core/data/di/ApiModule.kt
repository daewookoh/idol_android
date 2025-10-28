package net.ib.mn.core.data.di

import android.content.Context
import android.util.Base64
import android.util.Log
import com.exodus.bridge.SharedBridgeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.ib.mn.core.data.BuildConfig
import net.ib.mn.core.data.R
import net.ib.mn.core.data.api.ApiLogger
import net.ib.mn.core.data.api.ArticlesApi
import net.ib.mn.core.data.api.AuthApi
import net.ib.mn.core.data.api.AwardsApi
import net.ib.mn.core.data.api.BlocksApi
import net.ib.mn.core.data.api.ChartsApi
import net.ib.mn.core.data.api.ChatApi
import net.ib.mn.core.data.api.CommentsApi
import net.ib.mn.core.data.api.ConfigsApi
import net.ib.mn.core.data.api.CouponApi
import net.ib.mn.core.data.api.EmoticonApi
import net.ib.mn.core.data.api.FavoritesApi
import net.ib.mn.core.data.api.FriendsApi
import net.ib.mn.core.data.api.GameApi
import net.ib.mn.core.data.api.HeartpickApi
import net.ib.mn.core.data.api.HofsApi
import net.ib.mn.core.data.api.IdolsApi
import net.ib.mn.core.data.api.ImagesApi
import net.ib.mn.core.data.api.InquiryApi
import net.ib.mn.core.data.api.MarketApi
import net.ib.mn.core.data.api.MessagesApi
import net.ib.mn.core.data.api.MiscApi
import net.ib.mn.core.data.api.MissionsApi
import net.ib.mn.core.data.api.NoticeEventApi
import net.ib.mn.core.data.api.OnepickApi
import net.ib.mn.core.data.api.PlayApi
import net.ib.mn.core.data.api.FilesApi
import net.ib.mn.core.data.api.QuizApi
import net.ib.mn.core.data.api.RecommendApi
import net.ib.mn.core.data.api.RedirectApi
import net.ib.mn.core.data.api.ReportApi
import net.ib.mn.core.data.api.ScheduleApi
import net.ib.mn.core.data.api.SearchApi
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.api.StampsApi
import net.ib.mn.core.data.api.SupportApi
import net.ib.mn.core.data.api.ThemepickApi
import net.ib.mn.core.data.api.TimestampApi
import net.ib.mn.core.data.api.TrendsApi
import net.ib.mn.core.data.api.UsersApi
import net.ib.mn.core.data.api.VoteHistoryApi
import net.ib.mn.core.data.repository.account.AccountPreferencesRepository
import net.ib.mn.core.data.repository.language.LanguagePreferenceRepository
import net.ib.mn.core.model.DateSerializer
import net.ib.mn.core.utils.AppConst
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WithoutPrefix

@Module
@InstallIn(SingletonComponent::class)
internal object ApiModule {

    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context,
        converterFactory: Converter.Factory
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ServerUrl.buildBasePath())
        .addConverterFactory(ScalarsConverterFactory.create()) // ScalarConverterFactory 추가
        .addConverterFactory(converterFactory)
        .client(okHttpClient)
        .build()

    // prefix (/api/v1) 없는 경우
    @WithoutPrefix
    @Provides
    fun provideRetrofitWithoutPrefix(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context,
        converterFactory: Converter.Factory
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ServerUrl.buildBasePathWithoutPrefix())
        .addConverterFactory(ScalarsConverterFactory.create()) // ScalarConverterFactory 추가
        .addConverterFactory(converterFactory)
        .client(okHttpClient)
        .build()

    @Provides
    @Singleton
    fun provideOkhttpClient(
        dataInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(dataInterceptor)
            .addInterceptor(httpLogCheck())
            .build()

    @Provides
    @Singleton
    fun provideInterceptor(
        accountPreferencesRepository: AccountPreferencesRepository,
        languagePreferenceRepository: LanguagePreferenceRepository,
        @ApplicationContext context: Context,
    ): Interceptor {
        return Interceptor { chain ->

            val account = accountPreferencesRepository.getAccount()
            val credential = "${account?.email}:${account?.domain}:${account?.token}"
            val authHeader =
                "Basic ${Base64.encodeToString(credential.toByteArray(), Base64.NO_WRAP)}"

            val originalRequest = chain.request()
            val method = originalRequest.method

            val requestBuilder = originalRequest.newBuilder()
                .header("Authorization", authHeader)
                .header(
                    "User-Agent",
                    "${(System.getProperty("http.agent") ?: "")} (${context.applicationInfo.packageName}/${context.getString(R.string.app_version)}/${BuildConfig.VERSION_CODE})"
                )
                .header("X-HTTP-APPID", AppConst.APP_ID)
                .header(
                    "X-HTTP-VERSION", context.getString(
                        R.string.app_version
                    )
                )
                .header("X-HTTP-NATION", languagePreferenceRepository.getSystemLanguage())

            if (method == "POST" || method == "DELETE") {
                requestBuilder.addHeader("X-Nonce", System.nanoTime().toString() + "")
            }

            val request = requestBuilder.build()

            requestBuilder.build().headers.forEach {
                Log.d("ApiLogger", "HEADER: ${it.first} : ${it.second}")
            }

            // 응답 처리
            val response = chain.proceed(request)

            // 2XX 이외의 응답은 passthrough
            if (response.code !in 200..299) {
                return@Interceptor response
            }

            // 88888 처리
            val responseBody = response.body?.string()
            responseBody?.let {
                try {
                    val jsonResponse = JSONObject(it)
                    val gcode = jsonResponse.optInt("gcode")
                    if( gcode == 88888 ) {
                        SharedBridgeManager.setData(jsonResponse)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@Interceptor response.newBuilder()
                .body(responseBody?.toResponseBody(response.body?.contentType()))
                .build()
        }

    }

    @Provides
    @Singleton
    fun provideConverterFactory(
        json: Json,
    ): Converter.Factory {
        return json.asConverterFactory("application/json".toMediaType())
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        serializersModule = SerializersModule {
            contextual(Date::class, DateSerializer)
        }
        ignoreUnknownKeys = true
    }

    private fun httpLogCheck(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor(ApiLogger())
        return loggingInterceptor.setLevel(
            HttpLoggingInterceptor.Level.BODY,
        )
    }

    @Provides
    fun provideApiIdols(
        retrofit: Retrofit
    ): IdolsApi = retrofit.create(IdolsApi::class.java)

    @Provides
    fun provideApiConfigs(
        retrofit: Retrofit
    ): ConfigsApi = retrofit.create(ConfigsApi::class.java)

    @Provides
    fun provideApiCharts(
        retrofit: Retrofit
    ): ChartsApi = retrofit.create(ChartsApi::class.java)

    @Provides
    fun provideApiArticles(
        retrofit: Retrofit
    ): ArticlesApi = retrofit.create(ArticlesApi::class.java)

    @Provides
    fun provideApiQuiz(
        retrofit: Retrofit
    ): QuizApi = retrofit.create(QuizApi::class.java)

    @Provides
    fun provideApiComments(
        retrofit: Retrofit
    ): CommentsApi = retrofit.create(CommentsApi::class.java)

    @Provides
    fun provideApiFriends(
        retrofit: Retrofit
    ): FriendsApi = retrofit.create(FriendsApi::class.java)

    @Provides
    fun provideApiGame(
        retrofit: Retrofit
    ): GameApi = retrofit.create(GameApi::class.java)

    @Provides
    fun provideApiInquiry(
        retrofit: Retrofit
    ): InquiryApi = retrofit.create(InquiryApi::class.java)

    @Provides
    fun provideApiFiles(
        retrofit: Retrofit
    ): FilesApi = retrofit.create(FilesApi::class.java)

    @Provides
    fun provideApiReport(
        retrofit: Retrofit
    ): ReportApi = retrofit.create(ReportApi::class.java)

    @Provides
    fun provideApiVoteHistory(
        retrofit: Retrofit
    ): VoteHistoryApi = retrofit.create(VoteHistoryApi::class.java)

    @Provides
    fun provideApiChat(
        retrofit: Retrofit
    ): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    fun provideApiSupport(
        retrofit: Retrofit
    ): SupportApi = retrofit.create(SupportApi::class.java)

    @Provides
    fun provideApiMarket(
        retrofit: Retrofit
    ): MarketApi = retrofit.create(MarketApi::class.java)

    @Provides
    fun provideApiSchedule(
        retrofit: Retrofit
    ): ScheduleApi = retrofit.create(ScheduleApi::class.java)

    @Provides
    fun provideApiHeartpick(
        retrofit: Retrofit
    ): HeartpickApi = retrofit.create(HeartpickApi::class.java)

    @Provides
    fun provideApiOnepick(
        retrofit: Retrofit
    ): OnepickApi = retrofit.create(OnepickApi::class.java)

    @Provides
    fun provideApiThemepick(
        retrofit: Retrofit
    ): ThemepickApi = retrofit.create(ThemepickApi::class.java)

    @Provides
    fun provideApiSearch(
        retrofit: Retrofit
    ): SearchApi = retrofit.create(SearchApi::class.java)

    @Provides
    fun provideApiPlay(
        retrofit: Retrofit
    ): PlayApi = retrofit.create(PlayApi::class.java)

    @Provides
    fun provideApiTrends(
        retrofit: Retrofit
    ): TrendsApi = retrofit.create(TrendsApi::class.java)

    @Provides
    fun provideApiAwards(
        retrofit: Retrofit
    ): AwardsApi = retrofit.create(AwardsApi::class.java)

    @Provides
    fun provideApiMessages(
        retrofit: Retrofit
    ): MessagesApi = retrofit.create(MessagesApi::class.java)

    @Provides
    fun provideApiCoupon(
        retrofit: Retrofit
    ): CouponApi = retrofit.create(CouponApi::class.java)

    @Provides
    fun provideApiUsers(
        retrofit: Retrofit
    ): UsersApi = retrofit.create(UsersApi::class.java)

    @WithoutPrefix
    @Provides
    fun provideApiTimestamp(
        retrofit: Retrofit
    ): TimestampApi = retrofit.create(TimestampApi::class.java)

    @Provides
    fun provideApiFavorites(
        retrofit: Retrofit
    ): FavoritesApi = retrofit.create(FavoritesApi::class.java)

    @Provides
    fun provideApiNoticeEvent(
        retrofit: Retrofit
    ): NoticeEventApi = retrofit.create(NoticeEventApi::class.java)

    @Provides
    fun provideApiHofs(
        retrofit: Retrofit
    ): HofsApi = retrofit.create(HofsApi::class.java)

    @Provides
    fun provideApiBlocks(
        retrofit: Retrofit
    ): BlocksApi = retrofit.create(BlocksApi::class.java)

    @Provides
    fun provideApiMissions(
        retrofit: Retrofit
    ): MissionsApi = retrofit.create(MissionsApi::class.java)

    @Provides
    fun provideApiRedirect(
        @WithoutPrefix retrofit: Retrofit
    ): RedirectApi = retrofit.create(RedirectApi::class.java)

    @Provides
    fun provideApiAuth(
        retrofit: Retrofit
    ): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    fun provideApiStamps(
        retrofit: Retrofit
    ): StampsApi = retrofit.create(StampsApi::class.java)

    @Provides
    fun provideApiEmoticon(
        retrofit: Retrofit
    ): EmoticonApi = retrofit.create(EmoticonApi::class.java)

    @Provides
    fun provideImagesApi(
        retrofit: Retrofit
    ): ImagesApi = retrofit.create(ImagesApi::class.java)

    @Provides
    fun provideMiscApi(
        retrofit: Retrofit
    ): MiscApi = retrofit.create(MiscApi::class.java)

    @Provides
    fun provideRecommendApi(
        retrofit: Retrofit
    ): RecommendApi = retrofit.create(RecommendApi::class.java)
}