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
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

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
        val awardBegin = configRepository.getAwardBegin() ?: return ""

        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
            }
            val displayFormat = SimpleDateFormat.getDateInstance(
                DateFormat.MEDIUM,
                LocaleUtil.getAppLocale(context)
            ).apply {
                timeZone = TimeZone.getTimeZone("Asia/Seoul")
            }
            isoFormat.parse(awardBegin)?.let { displayFormat.format(it) }.orEmpty()
        } catch (e: Exception) {
            ""
        }
    }
}
