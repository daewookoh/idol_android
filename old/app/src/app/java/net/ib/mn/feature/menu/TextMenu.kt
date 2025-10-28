package net.ib.mn.feature.menu

import net.ib.mn.R

enum class TextMenu(val title: Int, val subTitle: Int) {
    VOTE_CERTIFICATE(R.string.certificate_title, R.string.certificate_menu_desc),
    BOARD(R.string.menu_board, R.string.menu_sub_board),
    STORE(R.string.label_store, R.string.menu_sub_store),
    NOTICE(R.string.setting_menu01, R.string.menu_sub_notice),
    HISTORY(R.string.menu_stats, R.string.menu_sub_record),
    QUIZ(R.string.menu_quiz, R.string.menu_sub_quiz),
    FACE(R.string.menu_face, R.string.menu_sub_face_matching),
}