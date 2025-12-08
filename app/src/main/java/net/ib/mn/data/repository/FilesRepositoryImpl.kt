package net.ib.mn.data.repository

import net.ib.mn.data.remote.api.FilesApi
import net.ib.mn.domain.repository.FilesRepository
import net.ib.mn.domain.repository.PresignedUrlResult
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "FilesRepository"

/**
 * FilesRepository 구현체
 * Old 프로젝트의 FilesRepositoryImpl 참고
 */
class FilesRepositoryImpl @Inject constructor(
    private val filesApi: FilesApi
) : FilesRepository {

    override suspend fun getPresignedUrl(
        bucket: String?,
        filename: String?,
        width: Int?,
        height: Int?,
        imageHash: String?,
        fileType: String?
    ): PresignedUrlResult {
        return try {
            logD(TAG, "getPresignedUrl request: filename=$filename, width=$width, height=$height, hash=$imageHash, fileType=$fileType")

            val response = filesApi.getPresignedUrl(
                bucket = bucket,
                filename = filename,
                width = width,
                height = height,
                imageHash = imageHash,
                fileType = fileType
            )

            logD(TAG, "getPresignedUrl response: code=${response.code()}, isSuccessful=${response.isSuccessful}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                logE(TAG, "getPresignedUrl error: code=${response.code()}, body=$errorBody")
                return PresignedUrlResult(success = false)
            }

            val bodyString = response.body()?.string()
            logD(TAG, "getPresignedUrl body: $bodyString")

            if (bodyString.isNullOrEmpty()) {
                logE(TAG, "getPresignedUrl: empty body")
                return PresignedUrlResult(success = false)
            }

            val jsonObject = JSONObject(bodyString)
            val success = jsonObject.optBoolean("success", false)
            val gcode = jsonObject.optInt("gcode", 0)
            val savedFilename = jsonObject.optString("saved_filename", "")

            // 이미 존재하는 이미지인 경우 (success=false, gcode=3900)
            if (!success && gcode == PresignedUrlResult.GCODE_ALREADY_EXISTS) {
                return PresignedUrlResult(
                    success = true,
                    gcode = gcode,
                    savedFilename = savedFilename
                )
            }

            if (!success) {
                logE(TAG, "getPresignedUrl: success=false, gcode=$gcode")
                return PresignedUrlResult(success = false, gcode = gcode)
            }

            val url = jsonObject.optString("url", "")
            val fieldsObject = jsonObject.optJSONObject("fields")

            PresignedUrlResult(
                success = true,
                gcode = gcode,
                url = url,
                savedFilename = savedFilename,
                awsAccessKeyId = fieldsObject?.optString("AWSAccessKeyId", "") ?: "",
                acl = fieldsObject?.optString("acl", "") ?: "",
                key = fieldsObject?.optString("key", "") ?: "",
                policy = fieldsObject?.optString("policy", "") ?: "",
                signature = fieldsObject?.optString("signature", "") ?: ""
            )
        } catch (e: Exception) {
            logE(TAG, "getPresignedUrl", e)
            PresignedUrlResult(success = false)
        }
    }

    override suspend fun writeCdn(
        url: String,
        awsAccessKeyId: String,
        acl: String,
        key: String,
        policy: String,
        signature: String,
        file: ByteArray,
        filename: String,
        mimeType: String
    ): Boolean {
        return try {
            val response = filesApi.writeCdn(
                url = url,
                AWSAccessKeyId = MultipartBody.Part.createFormData("AWSAccessKeyId", awsAccessKeyId),
                acl = MultipartBody.Part.createFormData("acl", acl),
                key = MultipartBody.Part.createFormData("key", key),
                policy = MultipartBody.Part.createFormData("policy", policy),
                signature = MultipartBody.Part.createFormData("signature", signature),
                file = MultipartBody.Part.createFormData(
                    "file",
                    filename,
                    file.toRequestBody(mimeType.toMediaTypeOrNull())
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            logE(TAG, "writeCdn", e)
            false
        }
    }
}
