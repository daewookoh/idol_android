package net.ib.mn.feature.menu

import net.ib.mn.R

/**
 * 아이콘과 제목으로만 구성된 메뉴 아이템
 * 메뉴 설명은 향후를 위해 남겨둠
 */
enum class IconTextMenu(val icon: Int, val title: Int, val subTitle: Int) {
    VOTE_CERTIFICATE(R.drawable.icon_sidemenu_votingcertificate, R.string.certificate_title, R.string.certificate_menu_desc),
    FREE_BOARD(R.drawable.icon_sidemenu_free_board, R.string.hometab_title_freeboard, R.string.menu_sub_board),
    STORE(R.drawable.icon_sidemenu_shop, R.string.label_store, R.string.menu_sub_store),
    NOTICE(R.drawable.icon_sidemenu_notice, R.string.setting_menu01, R.string.menu_sub_notice),
    INVITE_FRIEND(R.drawable.icon_sidemenu_invite_friend, R.string.menu_invite_friend, R.string.menu_invite_friend),
    HISTORY(R.drawable.icon_sidemenu_record, R.string.menu_stats, R.string.menu_sub_record),
    GAME(R.drawable.icon_sidemenu_game, R.string.menu_minigame, R.string.menu_minigame),
    QUIZ(R.drawable.icon_sidemenu_quiz, R.string.menu_quiz, R.string.menu_sub_quiz),
    FACE(R.drawable.icon_sidemenu_similar, R.string.menu_face, R.string.menu_sub_face_matching),
}