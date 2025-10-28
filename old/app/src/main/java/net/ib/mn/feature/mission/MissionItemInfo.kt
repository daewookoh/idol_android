package net.ib.mn.feature.mission

import net.ib.mn.R

enum class MissionItemInfo(val key: String, val title: Int, val subTitle: Int, val celebTitle: Int, val celebSubTitle: Int) {
    WELCOME_JOIN("welcome_join", R.string.welcome_mission_sign_up_title, R.string.welcome_mission_sign_up_desc, 0, 0),
    WELCOME_MOST("welcome_set_most", R.string.welcome_mission_set_most_title, R.string.welcome_mission_set_most_desc, R.string.celeb_welcome_mission_set_most_title, R.string.celeb_welcome_mission_set_most_desc),
    WELCOME_VOTE_MOST("welcome_vote_most", R.string.welcome_mission_vote_title, R.string.welcome_mission_vote_desc, 0, R.string.celeb_welcome_mission_vote_desc),
    WELCOME_ADD_FRIEND("welcome_add_friend", R.string.welcome_mission_add_friend_title, R.string.welcome_mission_add_friend_desc, 0, 0),
    WELCOME_POSTING("welcome_posting_most", R.string.welcome_mission_community_title, R.string.welcome_mission_community_desc, 0, R.string.celeb_welcome_mission_community_desc),
    WELCOME_VIDEO_AD("welcome_videoad", R.string.welcome_mission_video_ad_title, R.string.welcome_mission_video_ad_desc, 0, 0),
    WELCOME_ALL_CLEAR("welcome_all_clear", 0, 0, 0, 0)
}