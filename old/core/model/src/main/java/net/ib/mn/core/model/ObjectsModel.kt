package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ObjectsModel(
    @SerialName("badword") val badword: List<BadWordModel> = emptyList(),
    @SerialName("board_tag") val boardTag: List<TagModel>? = null,
    @SerialName("end_popup") val endPopup: EndPopupModel? = null,
    @SerialName("event_list") val eventList: String? = null,
    @SerialName("family_app_list") val familyAppList: List<FamilyAppModel>? = null,
    @SerialName("help_infos") val helpInfos: HelpInfosModel? = null,
    @SerialName("idol_board_tag") val idolBoardTag: List<TagModel>? = null,
    @SerialName("lgcode") val lgcode: List<String>? = emptyList(),
    @SerialName("new_picks") val newPicks: NewPicksModel? = null,
    @SerialName("notice_list") val noticeList: String? = null,
    @SerialName("sns") val sns: List<SnsModel> = emptyList(),
    @SerialName("upload_video_spec") val uploadVideoSpec: UploadVideoSpecModel? = null
)