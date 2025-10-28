/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 어떤 링크인지 구분 해주기 위한 enum class.
 *
 * */

package net.ib.mn.link.enum

/**
 * @see
 * */

enum class LinkStatus(val status: String) {
    ARTICLES("articles"),
    COMMUNITY("community"),
    COUPON("coupon"),
    IDOL("idol"),
    SUPPORTS("supports"),
    SUPPORT("support"),
    THEMEPICK("themepick"),
    IMAGEPICK("imagepick"),
    LIVE("live"),
    BANNERGRAM("bannergram"),
    RECORDS("records"),
    QNA("qna"),
    BOARD("board"),
    OFFERWALL("offerwall"),
    QUIZZES("quizzes"),
    FACEDETECT("facedetect"),
    NOTICES("notices"),
    EVENTS("events"),
    MENU("menu"),
    AWARD("awards"),
    HOTTRENDS("hottrends"),
    FRIENDS("friends"),
    ATTENDANCE("attendance"),
    SCHEDULES("schedules"),
    MIRACLE("miracle"),
    STORE("store"),
    ONE_PICK("onepick"),
    HOF("hof"),
    MY_FEED("feed"),
    MY_INFO("myinfo"),
    HEARTPICK("heartpick"),
    HEARTPICK_MAIN("heartpick_main"),
    ROOKIE("rookie"),
    INVITE("invite"),
    AUTH("auth"), // access token 교환. startup에 남아있던거 옮김
    INVITE_SHARE("invite_share")
}