package net.ib.mn.presentation.welcomemission

import net.ib.mn.R

/**
 * 미션 항목 정보
 *
 * old 프로젝트와 동일한 구조
 * 각 미션 항목의 타이틀, 서브타이틀 리소스 ID 포함
 */
enum class MissionItemInfo(
    val key: String,
    val title: Int,
    val subTitle: Int,
    val celebTitle: Int = 0,
    val celebSubTitle: Int = 0
) {
    /**
     * 회원가입 완료
     */
    WELCOME_JOIN(
        key = "welcome_join",
        title = R.string.welcome_mission_sign_up_title,
        subTitle = R.string.welcome_mission_sign_up_desc
    ),

    /**
     * 최애 설정
     */
    WELCOME_SET_MOST(
        key = "welcome_set_most",
        title = R.string.welcome_mission_set_most_title,
        subTitle = R.string.welcome_mission_set_most_desc,
        celebTitle = R.string.celeb_welcome_mission_set_most_title,
        celebSubTitle = R.string.celeb_welcome_mission_set_most_desc
    ),

    /**
     * 최애에게 투표
     */
    WELCOME_VOTE_MOST(
        key = "welcome_vote_most",
        title = R.string.welcome_mission_vote_title,
        subTitle = R.string.welcome_mission_vote_desc,
        celebTitle = R.string.welcome_mission_vote_title,
        celebSubTitle = R.string.celeb_welcome_mission_vote_desc
    ),

    /**
     * 친구 추가
     */
    WELCOME_ADD_FRIEND(
        key = "welcome_add_friend",
        title = R.string.welcome_mission_add_friend_title,
        subTitle = R.string.welcome_mission_add_friend_desc
    ),

    /**
     * 게시글 작성
     */
    WELCOME_POSTING_MOST(
        key = "welcome_posting_most",
        title = R.string.welcome_mission_community_title,
        subTitle = R.string.welcome_mission_community_desc,
        celebTitle = R.string.welcome_mission_community_title,
        celebSubTitle = R.string.celeb_welcome_mission_community_desc
    ),

    /**
     * 영상광고 시청
     */
    WELCOME_VIDEOAD(
        key = "welcome_videoad",
        title = R.string.welcome_mission_video_ad_title,
        subTitle = R.string.welcome_mission_video_ad_desc
    ),

    /**
     * 모든 미션 완료 (ALL CLEAR)
     */
    WELCOME_ALL_CLEAR(
        key = "welcome_all_clear",
        title = R.string.welcome_mission_all_clear_reward_title,
        subTitle = R.string.impossible_all_clear
    );

    companion object {
        fun fromKey(key: String): MissionItemInfo? {
            return entries.find { it.key == key }
        }
    }
}
