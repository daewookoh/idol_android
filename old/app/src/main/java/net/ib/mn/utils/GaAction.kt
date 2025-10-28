/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: Firebase Analytics Action 종류.
 *
 * */

package net.ib.mn.utils

enum class GaAction(
    val label: String = "",
    var actionKey: String = Const.ANALYTICS_GA_DEFAULT_ACTION_KEY,
    val actionValue: String = Const.ANALYTICS_BUTTON_PRESS_ACTION,
    val paramKey: String = ""
) {

    //TOP SCREEN
    TOP_CHANGE_GENDER(label = "top_change_gender"),
    LIST_MYIDOL(label = "list_myidol"),
    LIST_HOF(label = "list_hof"),

    //MENU
    MENU_ATTENDANCE_CHECK(label = "menu_attendance_check"),
    MENU_SETTING(label = "menu_setting"),
    MENU_PUSH_LOG(label = "menu_pushlog"),

    //HOF
    HOF_ORDER_TIME(label = "hof_order_time"),
    HOF_ORDER_HEART(label = "hof_order_heart"),
    HELP_HISTORY(label = "help_history"),

    //MYHEARTINFO
    MYHEART_LEVEL(label = "myheart_level"),
    MYHEART_USAGE(label = "myheart_usage"),
    MYHEART_GET(label = "myheart_get"),

    //SEARCH
    SEARCH(label = "search"),
    SEARCH_COMMUNITY(label = "search_community"),
    SEARCH_TALK(label = "search_talk"),
    SEARCH_SCHEDULE(label = "search_schedule"),
    SEARCH_COMMENT(label = "search_comment"),
    SEARCH_REPORT(label = "search_report"),
    SEARCH_WIDEPHOTO(label = "search_widephoto"),
    SEARCH_FAVORITE(label = "search_favorite"),
    SEARCH_MOST(label = "search_most"),
    SEARCH_MORE(label = "search_more"),
    SEARCH_EDIT(label = "search_edit"),
    SEARCH_DELETE(label = "search_delete"),
    SEARCH_FEED(label = "search_feed"),
    SEARCH_SHARE(label = "search_share"),

    //FRIENDS
    INVITE(label = "invite"),

    //QUIZ
    QUIZ_REVIEW(label = "quiz_review"),
    QUIZ_EVALUTAION_REJECT(label = "quiz_evalutaion_reject"),
    QUIZ_WRITE(label = "quiz_write"),

    //STATS
    QUIZ_RANKING_FEED(label = "quiz_ranking_feed"),

    //FEED
    FEED_BLOCK(label = "feed_block"),
    FEED_UNBLOCK(label = "feed_unblock"),

    //NOTIFICATION
    PUSHLOG_FRIEND(label = "pushlog_friend"),
    PUSHLOG_COMMENT(label = "pushlog_comment"),
    PUSHLOG_SCHEDULE(label = "pushlog_schedule"),
    PUSHLOG_SUPPORT(label = "pushlog_support"),

    //VOTE
    VOTE(label = "vote"),
    TOP_FRIEND(label = "top_friend"),
    THEME_PICK_RESULT_VOTE("themedpick_result_vote"),
    IMAGE_PICK_RESULT_VOTE("imagepick_result_vote"),

    //LEAGUE
    MAIN_LEAGUE_TAB(label = "main_s_league_boy_solo"),
    HOF_TRENDS_TAB(label = "hof_s_league_boy_solo_trends"),
    HOF_DAILY_TAB(label = "hof_s_league_boy_solo_daily"),
    HOF_TREND_CHART(label = "hof_boy_solo_trend_chart"),
    HOF_DAILY_HISTORY(label = "hof_boy_solo_daily_history"),

    //BOTTOM_SHEET
    BOTTOM_SHEET_AD_SHOW(label = "show_dynamic_banner_"),
    BOTTOM_SHEET_AD_CLICK(label = "tap_dynamic_banner_"),

    //SMALL_TALK
    SHARE_SMALL_TALK(label = "share_small_talk"),
    LIKE_SMALL_TALK_TRUE(label = "like_small_talk_true"),
    LIKE_SMALL_TALK_FALSE(label = "like_small_talk_false"),
    WRITE_COMMENT_SMALL_TALK(label = "write_comment_small_talk"),
    COMMUNITY_SMALL_TALK(label = "community_small_talk"),
    SEARCH_SMALL_TALK_ARTICLE(label = "search_small_talk_article"),
    TAG_SMALL_TALK_ARTICLE(label = "tag_small_talk_article"),
    TAG_KEY(paramKey = "tag_key"),
    SORT_FILTER_BUTTON_SMALL_TALK_ARTICLE(label = "sort_filter_button_small_talk_article"),
    SORT_FILTER_SMALL_TALK_ARTICLE(label = "sort_filter_small_talk_article"),
    SORT_FILTER_KEY(paramKey = "sort_filter_key"),
    LOCALE_FILTER_BUTTON_SMALL_TALK_ARTICLE(label = "locale_filter_button_small_talk_article"),
    LOCALE_FILTER_SMALL_TALK_ARTICLE(label = "locale_filter_small_talk_article"),
    LOCALE_FILTER_KEY(paramKey = "locale_filter_key"),
    MOVE_SMALL_TALK_DETAIL_ARTICLE(label = "move_small_talk_detail_article"),
    MOVE_SMALL_TALK_WRITE_ARTICLE(label = "move_small_talk_write_article"),


    //COMMENT
    COMMENT_DELETE(label = "comment_delete"),
    COMMENT_REPORT(label = "comment_report"),
    COMMENT_EDIT(label = "comment_edit"),
    COMMENT_ARTICLE_SHARE(label = "comment_article_share"),

    //STORE
    STORE_IN_APP_PURCHASE_SUCCESS(label = "store_in_app_purchase_success"),

    //Notice, Event Share
    NOTICE_SHARE(label = "notice_share"),
    EVENT_SHARE(label = "event_share"),

    //ATTENDANCE
    ATTENDANCE_CHECK_REWARD(label = "attendance_check_reward", paramKey = "attendance_check_reward"),

    //WEBVIEW
    CLOSE_WEB_VIEW(label = "close_webview"),

    //ARTICLE WRITE
    COMMUNITY_POST(label = "community_post"),
    SMALL_TALK_POST(label = "small_talk_post"),
    FREEBOARD_POST(label = "freeboard_post"),
    POST_SETTING_ONLY_CM(label = "post_setting_only_cm"),

    //PUSH
    PUSH_COMMENT(label = "push_comment"),
    PUSH_FRIEND(label = "push_friend"),
    PUSH_COUPON(label = "push_coupon"),
    PUSH_SCHEDULE(label = "push_schedule"),
    PUSH_SUPPORT(label = "push_support"),
    PUSH_NOTICE(label = "push_notice"),

    //LIKE
    SUPPORT_SUCCESS_LIKE(label = "support_success_like"),
    COMMUNITY_LIKE(label = "community_like"),
    FREEBOARD_LIKE(label = "freeboard_like"),
    QNA_LIKE(label = "qna_like"),

    ORDER_HEART("order_heart"),
    ORDER_TIME("order_time"),
    ORDER_COMMENTS("order_comments"),
    ORDER_LIKES("order_likes"),

    // 메인 - 이달의 기적 실시간 순위 탭
    MIRACLE_SHARE("miracle_share"),
    MIRACLE_INFO("miracle_info"),

    //하트픽.
    HEARTPICK_SHARE(label = "heartpick_share"),
    HEARTPICK_COMMENT(label = "heartpick_comment"),
    HEARTPICK_VOTE_SHARE(label = "heartpick_vote_share"),

    //종료 광고 안내 팝업.
    END_POPUP_OPENED(label = "android_endpopup_native"),

    // LOGIN
    SPLASH(label = "Splash"),
    LOGIN(label = "Login"),
    LOGIN_EMAIL(label = "Login_email"),
    SIGNUP_EMAIL(label = "Signup_email"),
    LOGIN_KAKAO(label = "Login_kakao"),
    LOGIN_LINE(label = "Login_line"),
    LOGIN_FACEBOOK(label = "Login_facebook"),
    LOGIN_GOOGLE(label = "Login_google"),
    LOGIN_APPLE(label = "Login_apple"),
    JOIN_AGREE(label = "Join_agree"),
    JOIN_TERM(label = "Join_term"),
    JOIN_PRIVACY(label = "Join_privacy"),
    JOIN_AGREE_BUTTON(label = "join_agree_btn"),
    JOIN_FORM_EMAIL(label = "Join_form_email"),
    JOIN_FORM_SOCIAL(label = "Join_form_social"),
    SIGN_UP(label = "sign_up"),
    RANKING_BOY_INDV(label = "Ranking_boy_indv"), // 애돌 Only
    RANKING_GIRL_INDV(label = "Ranking_girl_indv"), // 애돌 Only
    RANKING_BOY_GROUP(label = "Ranking_boy_group"), // 애돌 Only
    RANKING_GIRL_GROUP(label = "Ranking_girl_group"), // 애돌 Only
    RANKING_OVERALL(label = "Ranking_overall"), // 셀럽 Only
    CHOEAE_POPUP(label = "Choeae_popup"),
    CHOEAE_POPUP_YES(label = "Choeae_popup_yes"),
    CHOEAE_POPUP_NO(label = "Choeae_popup_no"),
    CHOEAE_SETTING(label = "Choeae_setting"),

    // ROOKIE
    ROOKIE_SHARE(label = "rookie_share"),
    ROOKIE_INFO("rookie_info"),

    // VIDEO_AD
    ATTENDANCE_VIDEO_AD(label = "attendance_check_videoad"),

    // 토스트 관련 이벤트.
    RANKING_MY_IDOL(label = "ranking_myidol"),

    // 하단 네비 탭바.
    BTB_RANK(label = "btb_rank"),
    BTB_MY_IDOL(label = "btb_myidol"),
    BTB_MY_PAGE(label = "btb_mypage"),
    BTB_FREE_BOARD(label = "btb_freeboard"),
    BTB_MENU(label = "btb_menu"),
    BTB_MIRACLE(label = "btb_miracle"),
    BTB_THEME_PICK(label = "btb_themepick"),
    BTB_HOF(label = "btb_hof"),

    SUPPORT_ADDRESS("support_address"),
    SUPPORT_SUCCESS_SHARE("support_vote_share"),
    SUPPORT_SORT_IN_LIST("support_sort_adplace"),

    MISSION_ALL_CLEAR("welcome_allclear"),

    CERTIFICATE_SAVE("Certi_save"),
    CERTIFICATE_SHARE("Certi_share"),

    HEART_PICK_PRELAUNCH("voting_alert_heartpick"),
    THEME_PICK_PRELAUNCH("voting_alert_themepick"),
    IMAGE_PICK_PRELAUNCH("voting_alert_imagepick"),
    SYS_PUSH("sys_push"),

    THEME_PICK_PRELAUNCH_SHARE("themepick_preview_share"),
    HEART_PICK_PRELAUNCH_SHARE("heartpick_preview_share"),

    FRIEND_GO_INVITE("friend_go_invite_page"),
    FRIEND_INVITE_CODE("share_invite_code"),
    FRIEND_INVITE_BTN("share_invite_btn"),
}