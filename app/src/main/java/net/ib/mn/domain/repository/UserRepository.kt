package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.dto.*
import net.ib.mn.domain.model.ApiResult

/**
 * User Repository 인터페이스
 *
 * 사용자 관련 데이터 접근 추상화
 */
interface UserRepository {

    /**
     * 사용자 프로필 조회 (ETag 캐싱 지원)
     *
     * @param etag 이전 ETag 값 (캐시 검증용)
     * @param cacheControl Cache-Control 헤더 값 (캐시 제어용)
     * @param timestamp 캐시 무효화용 타임스탬프 (null이 아니면 ts에 설정되어 캐시 무효화)
     * @return Flow<ApiResult<UserSelfResponse>>
     */
    fun getUserSelf(etag: String? = null, cacheControl: String? = null, timestamp: Int? = null): Flow<ApiResult<UserSelfResponse>>

    /**
     * 사용자 상태 조회
     *
     * @return Flow<ApiResult<UserStatusResponse>>
     */
    fun getUserStatus(): Flow<ApiResult<UserStatusResponse>>

    /**
     * IAB 공개키 조회
     *
     * @return Flow<ApiResult<IabKeyResponse>>
     */
    fun getIabKey(): Flow<ApiResult<IabKeyResponse>>

    /**
     * 차단 사용자 목록 조회
     *
     * @return Flow<ApiResult<BlockListResponse>>
     */
    fun getBlocks(): Flow<ApiResult<BlockListResponse>>

    /**
     * 사용자 검증 (회원 여부 확인)
     *
     * @param type 검증 타입 (email, nickname 등)
     * @param value 검증할 값
     * @param appId 앱 ID
     * @return Flow<ApiResult<ValidateResponse>>
     */
    fun validateUser(
        type: String,
        value: String,
        appId: String
    ): Flow<ApiResult<ValidateResponse>>

    /**
     * 로그인
     *
     * @param domain 로그인 도메인 (email, kakao, google, line, facebook)
     * @param email 이메일
     * @param password 비밀번호
     * @param deviceKey 푸시 키
     * @param gmail Gmail 계정
     * @param deviceId 디바이스 ID
     * @param appId 앱 ID
     * @return Flow<ApiResult<SignInResponse>>
     */
    fun signIn(
        domain: String,
        email: String,
        password: String,
        deviceKey: String,
        gmail: String,
        deviceId: String,
        appId: String
    ): Flow<ApiResult<SignInResponse>>

    /**
     * 회원가입
     *
     * @param email 이메일
     * @param password 비밀번호
     * @param nickname 닉네임
     * @param domain 가입 도메인 (email, kakao, google, line, facebook)
     * @param recommenderCode 추천인 코드 (옵션)
     * @param appId 앱 ID
     * @return Flow<ApiResult<CommonResponse>>
     */
    fun signUp(
        email: String,
        password: String,
        nickname: String,
        domain: String,
        recommenderCode: String,
        appId: String
    ): Flow<ApiResult<CommonResponse>>

    /**
     * device id로 아이디 찾기
     *
     * @param deviceId 디바이스 ID
     * @return Flow<ApiResult<String>> - 찾은 아이디 (이메일 또는 도메인 설명)
     */
    fun findId(deviceId: String?): Flow<ApiResult<String>>

    /**
     * 비밀번호 찾기 (이메일로 비밀번호 재설정 링크 전송)
     *
     * @param email 이메일 주소
     * @return Flow<ApiResult<CommonResponse>>
     */
    fun findPassword(email: String): Flow<ApiResult<CommonResponse>>

    /**
     * UserSelf 데이터를 로드하고 DataStore와 Local DB에 저장
     *
     * @param cacheControl Cache-Control 헤더 값
     * @return Result<Boolean> - 성공 시 true, 401 에러 시 Exception("Unauthorized")
     */
    suspend fun loadAndSaveUserSelf(cacheControl: String? = null): Result<Boolean>
}
