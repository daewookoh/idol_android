package net.ib.mn.presentation.awards

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.data.remote.dto.AwardModel
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.link.LinkUtil
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

private const val GUIDE_DESC_SEPARATOR = "|"
private const val GUIDE_DESC_FIELD_COUNT = 11

/**
 * AwardsViewModel - 어워즈 화면 ViewModel
 */
@HiltViewModel
class AwardsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository
) : ViewModel() {

    val awardModel: StateFlow<AwardModel?> = configRepository.observeAwardModel()

    private val votable: String
        get() = configRepository.getVotable()

    /**
     * 가이드 설명 데이터 파싱 (pipe-delimited)
     * index 0: 타이틀, 1: 설명, 2: 투표기간 제목, 3: 투표대상 제목, 4: 투표대상 설명,
     * 5: 투표방법 제목, 6: 투표방법 설명, 7: 유의사항 제목, 8: 유의사항 설명,
     * 9: 투표규정 제목, 10: 투표규정 설명
     */
    fun getGuideDescriptions(): List<String>? {
        val desc = awardModel.value?.desc ?: return null
        if (!desc.contains(GUIDE_DESC_SEPARATOR)) return null
        val parts = desc.split(GUIDE_DESC_SEPARATOR)
        return if (parts.size >= GUIDE_DESC_FIELD_COUNT) parts else null
    }

    /**
     * 투표 기간 문자열 반환 (예: "2024. 1. 1. ~ 2024. 1. 31. (KST)")
     */
    fun getAwardPeriodText(): String {
        val beginDate = parseDate(configRepository.getAwardBegin()) ?: return ""
        val endDate = parseDate(configRepository.getAwardEnd()) ?: return ""
        return "${formatDate(beginDate)} ~ ${formatDate(endDate)} (KST)"
    }

    /**
     * 테마에 따른 로고 이미지 URL 반환
     */
    fun getLogoImageUrl(isDarkTheme: Boolean): String? =
        if (isDarkTheme) awardModel.value?.logoDarkImgUrl else awardModel.value?.logoLightImgUrl

    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = seoulTimeZone
            }.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(date: Date): String = SimpleDateFormat.getDateInstance(
        DateFormat.MEDIUM,
        LocaleUtil.getAppLocale(context)
    ).apply { timeZone = seoulTimeZone }.format(date)

    companion object {
        private val seoulTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    /**
     * 공유 메시지 생성
     */
    fun getShareMessage(): String {
        val awardsData = awardModel.value
        val title = awardsData?.awardTitle.orEmpty()
        val keyword = awardsData?.keyword.orEmpty()

        val params = buildList {
            add("awards")
            if (keyword.isNotEmpty()) add(keyword)
        }
        val link = LinkUtil.getAppLinkUrl(context, params)

        val shareMsg = when (votable) {
            "B" -> formatReadyMessage(title)
            "Y" -> formatRunMessage(title)
            else -> formatResultMessage(title)
        }

        return "$shareMsg\n$link"
    }

    private fun formatReadyMessage(title: String): String {
        val format = context.getString(
            if (BuildConfig.CELEB) R.string.celeb_share_awards_ready
            else R.string.share_awards_ready
        )
        val startDate = parseAwardBeginDate()
        return String.format(format, title, startDate)
    }

    private fun formatRunMessage(title: String): String {
        val format = context.getString(
            if (BuildConfig.CELEB) R.string.celeb_share_awards_run
            else R.string.share_awards_run
        )
        return String.format(format, title)
    }

    private fun formatResultMessage(title: String): String {
        val format = context.getString(
            if (BuildConfig.CELEB) R.string.celeb_share_awards_result
            else R.string.share_awards_result
        )
        return String.format(format, title)
    }

    private fun parseAwardBeginDate(): String {
        val date = parseDate(configRepository.getAwardBegin()) ?: return ""
        return formatDate(date)
    }
}
