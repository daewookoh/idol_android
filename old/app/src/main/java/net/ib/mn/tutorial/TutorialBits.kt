package net.ib.mn.tutorial

object TutorialBits {

    const val NO_TUTORIAL = -1

    /**
     * 메인 화면 관련 튜토리얼 비트 정의
     */
    const val MAIN_GENDER = 0        // 남녀 토글 버튼
    const val MAIN_BANNER_GRAM = 1          // 아이돌 프로필 이미지 (배너 펼쳐짐)
    const val MAIN_COMMUNITY = 2       // 커뮤니티 이동 아이콘
    const val MAIN_SEARCH = 3        // 메인 돋보기 버튼 (핫트렌드 이동)
    const val MAIN_FRIEND = 4        // 친구 버튼
    const val MAIN_MIRACLE = 5          // 기적 탭 버튼
    const val MAIN_HEART_PICK = 6    // 하트픽 탭 버튼
    const val MAIN_VOTE = 7          // 메인 투표 버튼
    const val MAIN_ONE_PICK = 8         // 원픽 탭 버튼
    const val RANKING = 9              // 하단바 랭킹 버튼
    const val MY_IDOL = 10             // 하단바 나의 최애 버튼
    const val PROFILE = 11             // 하단바 나의 정보 버튼
    const val MENU = 12                // 하단바 메뉴 버튼
    /**
     * 메뉴 화면 관련 튜토리얼 비트 정의
     */
    const val MENU_SUPPORT = 14             // 서포트 버튼
    const val MENU_CERTIFICATE = 15         // 투표 인증서 버튼
    const val MENU_DAILY_STAMP = 16         // 출석 체크 버튼
    const val MENU_QUIZ = 17                // 퀴즈 버튼
    const val MENU_NOTICE = 18              // 공지사항 버튼
    const val MENU_EVENT = 19               // 이벤트 버튼
    const val MENU_STATS = 20               // 기록실 버튼
    const val MENU_FREE_HEART = 21          // 무료 충전소 버튼
    const val MENU_HEART_SHOP = 22          // 하트 상점 버튼
    const val MENU_SETTINGS = 23            // 우측 상단 설정 버튼
    const val MENU_NOTIFICATION = 24        // 우측 상단 알림 버튼
    /**
     * 커뮤니티 및 피드 관련 튜토리얼 비트 정의
     */
    const val COMMUNITY_WIKI = 25        // 커뮤니티 아이돌 프로필 (위키로 이동)
    const val COMMUNITY_FANTALK = 26        // 팬톡 탭 버튼
    const val COMMUNITY_CHAT = 27            // 채팅 탭 버튼
    const val COMMUNITY_SCHEDULE = 28        // 스케줄 탭 버튼
    const val COMMUNITY_WRITE = 29          // 게시글 작성 버튼
    const val COMMUNITY_MORE = 30         // 더보기(...) 버튼
    const val COMMUNITY_FAN_TALK_DETAIL = 31     // 팬톡 상세 화면
    const val COMMUNITY_FAN_TALK_WRITE = 32      // 팬톡 작성 버튼
    const val COMMUNITY_FEED_USER_PROFILE = 33        // 피드 유저 프로필
    const val COMMUNITY_FEED_VOTE = 34         // 피드 투표 버튼
    const val COMMUNITY_FEED_LIKES = 35               // 피드 좋아요 버튼
    const val COMMUNITY_FEED_COMMENTS = 36            // 피드 댓글 버튼
    /**
     * 마이하트 화면 관련 튜토리얼 비트 정의
     */
    const val MY_HEAET_VIDEO_AD = 37            // 비디오 광고 버튼
    const val MY_HEART_HEART_SHOP = 38          // 하트 상점 버튼
    const val MY_HEART_FREE_HEART = 39              // 친구 초대 버튼
    const val MY_HEART_EARN = 40             // 내역 보기 버튼
    /**
     * 친구 화면 관련 튜토리얼 비트 정의
     */
    const val FRIEND_NEW_FACE = 41            // 친구 화면 상단 NewFace 버튼
    const val FRIEND_INVITE = 42       // 친구 화면 하단 초대 버튼
    /**
     * 서포트 화면 관련 튜토리얼 비트 정의
     */
    const val SUPPORT_TOGGLE = 43              // 인증샷 보기 <-> 서포트 메인 토글 버튼

    const val MENU_FRIEND_INVITE = 44 // 메뉴 - 친구 초대 버튼

    val all = listOf(
        MAIN_GENDER, MAIN_BANNER_GRAM, MAIN_COMMUNITY,
        MAIN_SEARCH, MAIN_FRIEND, MAIN_MIRACLE,
        MAIN_HEART_PICK, MAIN_VOTE, MAIN_ONE_PICK,
        RANKING, MY_IDOL, PROFILE, MENU,
        MENU_SUPPORT, MENU_CERTIFICATE, MENU_DAILY_STAMP, MENU_QUIZ, MENU_NOTICE,
        MENU_EVENT, MENU_STATS, MENU_FREE_HEART, MENU_HEART_SHOP, MENU_SETTINGS, MENU_NOTIFICATION,
        COMMUNITY_WIKI, COMMUNITY_FANTALK, COMMUNITY_CHAT, COMMUNITY_SCHEDULE,
        COMMUNITY_WRITE, COMMUNITY_MORE, COMMUNITY_FAN_TALK_DETAIL, COMMUNITY_FAN_TALK_WRITE,
        COMMUNITY_FEED_USER_PROFILE, COMMUNITY_FEED_VOTE, COMMUNITY_FEED_LIKES, COMMUNITY_FEED_COMMENTS,
        MY_HEAET_VIDEO_AD, MY_HEART_FREE_HEART, MY_HEART_HEART_SHOP, MY_HEART_EARN,
        FRIEND_NEW_FACE, FRIEND_INVITE, SUPPORT_TOGGLE, MENU_FRIEND_INVITE
    )
}

