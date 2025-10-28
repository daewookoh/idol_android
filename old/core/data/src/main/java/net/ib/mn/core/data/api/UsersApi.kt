/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.di.WithoutPrefix
import net.ib.mn.core.data.dto.AlterNicknameDTO
import net.ib.mn.core.data.dto.BlockUserDTO
import net.ib.mn.core.data.dto.ChangePasswordDTO
import net.ib.mn.core.data.dto.DailyRewardDTO
import net.ib.mn.core.data.dto.FindPasswordDTO
import net.ib.mn.core.data.dto.IabVerifyDTO
import net.ib.mn.core.data.dto.InhouseOfferwallDTO
import net.ib.mn.core.data.dto.ProvideHeartDTO
import net.ib.mn.core.data.dto.PushFilterDTO
import net.ib.mn.core.data.dto.SetProfileImageDTO
import net.ib.mn.core.data.dto.SignInDTO
import net.ib.mn.core.data.dto.SignUpDTO
import net.ib.mn.core.data.dto.StatusDTO
import net.ib.mn.core.data.dto.UpdateMostDTO
import net.ib.mn.core.data.dto.UpdateProfileDTO
import net.ib.mn.core.data.dto.UpdatePushKeyDTO
import net.ib.mn.core.data.dto.UpdateTutorialDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


/**
 * Users api
 * */

interface UsersApi {
    /**
    * 회원가입
    * */
    @POST("users/")
    suspend fun signUp(
        @Header("SIGNATURE") signature: String,
        @Body body: SignUpDTO
    ): Response<ResponseBody>

    @GET("users/give_reward_heart/")
    suspend fun giveRewardHeart(
        @Header("SIGNATURE") signature: String,
        @Query("type") type: String? = null,
    ): Response<ResponseBody>

    @POST("users/email_signin/")
    suspend fun signIn(
        @Body body: SignInDTO
    ): Response<ResponseBody>

    @GET("users/check_event/")
    suspend fun getEvent(
        @Query("version") version: String,
        @Query("gmail") gmail: String,
        @Query("is_vm") isVM: String,
        @Query("device_id") deviceId: String,
    ): Response<ResponseBody>

    @POST("users/find_passwd/")
    suspend fun findPassword(
        @Body body: FindPasswordDTO
    ): Response<ResponseBody>

    @POST("users/change_password/")
    suspend fun changePassword(
        @Body body: ChangePasswordDTO
    ): Response<ResponseBody>

    @POST("users/add_image/")
    suspend fun setProfileImage(
        @Body body: SetProfileImageDTO
    ): Response<ResponseBody>

    // 최애 삭제
    @POST("users/delmost/")
    suspend fun deleteMost(
    ): Response<ResponseBody>

    // 최애 변경
    @PATCH
    suspend fun updateMost(
        @Url resourceUri: String, // 본인의 resource uri
        @Body most: UpdateMostDTO,
    ): Response<ResponseBody>

    @POST("users/validate/")
    suspend fun validate(
        @Body params: Map<String, String?>
    ): Response<ResponseBody>

    @POST("users/alter_nickname/")
    suspend fun alterNickname(
        @Body body: AlterNicknameDTO
    ): Response<ResponseBody>

    @POST("users/provide_heart/")
    suspend fun provideHeart(
        @Body body: ProvideHeartDTO
    ): Response<ResponseBody>

    @GET("users/is_active_time/")
    suspend fun isActiveTime(
    ): Response<ResponseBody>

    @GET("users/new_friends_recommend/")
    suspend fun newFriendsRecommend(
    ): Response<ResponseBody>

    @GET("users/status/")
    suspend fun getStatus(
        @Query("user_id") userId: Int,
    ): Response<ResponseBody>


    @POST("users/status/")
    suspend fun setStatus(
        @Body body: StatusDTO,
    ): Response<ResponseBody>

    // 오토클릭커  휴먼체크 팝업  문제를 틀렸을때  -> 유저를 ban 시키기위한  api를 부른다.
    @GET("users/ban_autoclicker/")
    suspend fun banAutoClicker(
    ): Response<ResponseBody>

    @GET("users/heart_diamond_log/")
    suspend fun getHeartDiamondLog(
    ): Response<ResponseBody>

    // get처럼 보이지만 post임
    @POST("users/iab_get_key/")
    suspend fun getIabKey(
    ): Response<ResponseBody>

    @POST("users/dropout/")
    suspend fun dropout(
    ): Response<ResponseBody>

    @GET("users/ranked_user/")
    suspend fun getRankedUser(
        @Query("idol_id") idolId: Int,
        @Query("league") league: String? = null,
    ): Response<ResponseBody>

    @POST("users/iab_verify/")
    suspend fun iabVerify(
        @Body body: IabVerifyDTO
    ): Response<ResponseBody>

    @POST("payments/google/item/")
    suspend fun paymentGoogleItem(
        @Body body: IabVerifyDTO
    ): Response<ResponseBody>

    @POST("payments/google/subscription/")
    suspend fun paymentGoogleSubscription(
        @Body body: IabVerifyDTO
    ): Response<ResponseBody>

    @POST("payments/google/subscription/check/")
    suspend fun paymentGoogleSubscriptionCheck(
        @Body body: IabVerifyDTO
    ): Response<ResponseBody>

    @POST("payments/google/restore/")
    suspend fun paymentGoogleRestore(
        @Body body: IabVerifyDTO
    ): Response<ResponseBody>

    @POST("users/pushkey_update/")
    suspend fun updatePushKey(
        @Body body: UpdatePushKeyDTO
    ): Response<ResponseBody>

    @POST("users/pushfilter/")
    suspend fun updatePushFilter(
        @Body body: PushFilterDTO
    ): Response<ResponseBody>

    // 인하우스 광고 표시여부
    @POST("users/inhouse_offerwall_check/")
    suspend fun inhouseOfferwallCheck(
    ): String

    // 인하우스 광고 클릭
    @POST("users/inhouse_offerwall_create/")
    suspend fun inhouseOfferwallCreate(
        @Body body: InhouseOfferwallDTO
    ): Response<ResponseBody>

    @GET("users/offerwall_callback/")
    suspend fun getOfferwallCallback(
        @Query("user_id") userId: Int,
        @Query("ad_id") adId: Int,
        @Query("click_url") clickUrl: String? = null,
    ): Response<ResponseBody>

    @GET("users/total_top_ranker/")
    suspend fun getTopRanker(
    ): Response<ResponseBody>

    // 친구가 보낸 하트
    // GET으로 보이지만 POST임
    @POST("users/friendheart_log/")
    suspend fun getFriendHeartLog(
    ): Response<ResponseBody>

    // device id로 아이디 찾기
    @GET("users/find_id/")
    suspend fun findId(
        @Query("device_id") deviceId: String? = null,
    ): Response<ResponseBody>

    @GET("users/search_nickname/")
    suspend fun searchNickname(
        @Query("q") q: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int? = null,
    ): Response<ResponseBody>

    @POST("users/nativex_order/")
    suspend fun createNativeXOrder(
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    // PaymentWall
    @GET
    suspend fun getPaymentwallSignature(
        @Url url: String,
    ): Response<ResponseBody>

    @GET("users/nativex_signature/")
    suspend fun getNativeXSignature(
        @Query("app_string") body: String,
        @Query("trade_no") tradeNo: String,
    ): Response<ResponseBody>

    @GET("users/info/")
    suspend fun getUserInfo(
        @Query("email") email: String,
        @Query("ts") ts: Int,
    ): Response<ResponseBody>

    @GET("users/daily_rewards/")
    suspend fun getDailyRewards(
    ): Response<ResponseBody>

    @POST("users/daily_reward/")
    suspend fun postDailyReward(
        @Body body: DailyRewardDTO,
    ): Response<ResponseBody>

    @GET("users/wechat_token/")
    suspend fun getWechatToken(
        @Query("code") code: String,
    ): Response<ResponseBody>

    @PATCH
    suspend fun updateProfile(
        @Url resourceUri: String,
        @Body body: UpdateProfileDTO,
    ): Response<ResponseBody>

    @POST("users/update_tutorial/")
    suspend fun updateTutorial(
        @Body body: UpdateTutorialDTO
    ): Response<ResponseBody>

    @GET("users/web_token/")
    suspend fun getWebToken(
    ): Response<ResponseBody>
}