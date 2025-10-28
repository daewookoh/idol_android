/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 언어 조합마다 보여주는 뷰가 다를때 사용 하기 위해 만들어 놓음
 *
 * */

package net.ib.mn.utils

/**
 * @see BOARD_KIN_QUIZZES_TOP100_LOCALES ex) 자유 게시판(지식돌) 같은경우 선언한 언어 제외 하고 탭이 없는 화면이 보여야됨.
 *
 * */

object SupportedLanguage {

    // 자유 게시판, 지식돌, 퀴즈, 퀴즈랭킹 TOP 100 지원언어.
    val BOARD_KIN_QUIZZES_TOP100_LOCALES = listOf("ko", "en", "ja")
}