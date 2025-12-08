package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult

/**
 * 파일 업로드 Repository 인터페이스
 * Old 프로젝트의 FilesRepository 참고
 */
interface FilesRepository {

    /**
     * Presigned URL 요청
     *
     * @param bucket S3 버킷 이름 (null이면 기본 버킷)
     * @param filename 파일명
     * @param width 이미지 너비
     * @param height 이미지 높이
     * @param imageHash 이미지 해시 (중복 업로드 방지)
     * @param fileType 파일 타입 ("image", "video")
     * @return Presigned URL 응답
     */
    suspend fun getPresignedUrl(
        bucket: String? = null,
        filename: String?,
        width: Int?,
        height: Int?,
        imageHash: String?,
        fileType: String? = null
    ): PresignedUrlResult

    /**
     * CDN에 파일 업로드
     *
     * @param url S3 업로드 URL
     * @param awsAccessKeyId AWS Access Key ID
     * @param acl ACL 설정
     * @param key S3 key
     * @param policy 업로드 정책
     * @param signature 서명
     * @param file 파일 바이트 배열
     * @param filename 저장된 파일명
     * @param mimeType MIME 타입
     * @return 업로드 성공 여부
     */
    suspend fun writeCdn(
        url: String,
        awsAccessKeyId: String,
        acl: String,
        key: String,
        policy: String,
        signature: String,
        file: ByteArray,
        filename: String,
        mimeType: String
    ): Boolean
}

/**
 * Presigned URL 응답 모델
 */
data class PresignedUrlResult(
    val success: Boolean,
    val gcode: Int = 0,
    val url: String = "",
    val savedFilename: String = "",
    val awsAccessKeyId: String = "",
    val acl: String = "",
    val key: String = "",
    val policy: String = "",
    val signature: String = ""
) {
    companion object {
        // 이미 존재하는 이미지인 경우 gcode
        const val GCODE_ALREADY_EXISTS = 3900
    }
}
