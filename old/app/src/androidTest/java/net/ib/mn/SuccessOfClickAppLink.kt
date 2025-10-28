/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 링크 관련 테스트 코드이다. 단순히 링크를 눌렀을때 해당 화면으로 이동이 되는지 확인한다.
 *
 * */

package net.ib.mn

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertTrue
import net.ib.mn.account.IdolAccount
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


/**
 * @see
 * */


@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class SuccessOfClickAppLink {

    enum class LinkForTest(
        val lastPathSegment: String,
        var visibleOfTextOnView: String,
        val extraPath: String = ""
    ) {
        ATTENDANCE("attendance", "출석체크"),
        FRIENDS("friends", "친구"),
        FREE_BOARD("board", "자유게시판"),
        QNA_BOARD("qna", "지식돌"),
        SUPPORT_IN_PROGRESS("inprogress", "인증샷 보기", "supports"),
        SUPPORT_SUCCESS("success", "서포트 메인 가기", "supports"),
        MENU("menu", "메뉴"),
        QUIZZES("quizzes", if (BuildConfig.CELEB) "연예 퀴즈" else "아이돌 퀴즈"),
        FACE_DETECT("facedetect", "닮은꼴 찾기"),
        NOTICE_MAIN("notices", "공지사항"),
        EVENT_MAIN("events", "이벤트"),
        RECORDS_MAIN("records", "기록실"),
        RECORDS_ANGEL_MAIN("angel", "기부천사", "records"),
        RECORDS_FAIRY_MAIN("fairy", "기부요정", "records"),
        RECORDS_MIRACLE_MAIN("miracle", "이달의 기적", "records"),
        FREE_CHARGE("offerwall", "무료 충전소"),
        STORE("store", "상점", "store"),
        STORE_HEART("heart", "3,400 에버 하트", "store"),
        STORE_PACKAGE("package", "웰컴패키지", "store"),
        STORE_DIA("dia", "유의사항", "store"),
        MY_FEED("feed", "나의 피드"),
        MY_INFO("myinfo", "나의 정보"),
        COMMUNITY("community", "")
    }

    private lateinit var uiDevice: UiDevice
    private lateinit var context: Context

    @Before
    fun setUp() {

        // UI Automator 인스턴스 초기화
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun click_friends_link() {
        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.FRIENDS.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.friend))
    }

    @Test
    fun click_attendance_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.ATTENDANCE.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.attendance_check))
    }

    @Test
    fun click_free_board_link() {

        uiDevice.pressHome()

        // URL 스킴 인텐트 생성 및 발생
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = getDefaultIntent(getLink(context, LinkForTest.FREE_BOARD.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.menu_freeboard))
    }

    @Test
    fun click_qna_board_link() {

        uiDevice.pressHome()

        // URL 스킴 인텐트 생성 및 발생
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = getDefaultIntent(getLink(context, LinkForTest.QNA_BOARD.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.menu_menu_kin))
    }

    @Test
    fun click_SUPPORT_IN_PROGRESS_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.SUPPORT_IN_PROGRESS.lastPathSegment,
                LinkForTest.SUPPORT_IN_PROGRESS.extraPath
            )
        )
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.support_success_list))
    }

    @Test
    fun click_SUPPORT_SUCCESS_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.SUPPORT_SUCCESS.lastPathSegment,
                LinkForTest.SUPPORT_SUCCESS.extraPath
            )
        )
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.support_main))
    }

    @Test
    fun click_MENU_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.MENU.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.title_tab_menu))
    }

    @Test
    fun click_QUIZZES_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.QUIZZES.lastPathSegment))
        context.startActivity(intent)
        val visibleText = if (BuildConfig.CELEB) context.getString(R.string.actor_menu_quiz) else context.getString(R.string.menu_quiz)
        assertTureInputText(visibleText)
    }

    @Test
    fun click_FACE_DETECT_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.FACE_DETECT.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.face_title))
    }

    @Test
    fun click_NOTICE_MAIN_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.NOTICE_MAIN.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.title_notice))
    }

    @Test
    fun click_EVENT_MAIN_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.EVENT_MAIN.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.title_event))
    }

    @Test
    fun click_RECORDS_MAIN_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.RECORDS_MAIN.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.menu_stats))
    }

    @Test
    fun click_RECORDS_ANGEL_MAIN_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.RECORDS_ANGEL_MAIN.lastPathSegment,
                LinkForTest.RECORDS_ANGEL_MAIN.extraPath
            )
        )
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.charity_angel))
    }

    @Test
    fun click_RECORDS_FAIRY_MAIN_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.RECORDS_FAIRY_MAIN.lastPathSegment,
                LinkForTest.RECORDS_FAIRY_MAIN.extraPath
            )
        )
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.charity_fairy))
    }

    @Test
    fun click_RECORDS_MIRACLE_MAIN_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.RECORDS_MIRACLE_MAIN.lastPathSegment,
                LinkForTest.RECORDS_MIRACLE_MAIN.extraPath
            )
        )
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.miracle_month))
    }

    @Test
    fun click_FREE_CHARGE_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.FREE_CHARGE.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.btn_free_heart_charge))
    }

    @Test
    fun click_STORE_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.STORE.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.label_store))
    }

    @Test
    fun click_STORE_HEART_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.STORE_HEART.lastPathSegment,
                LinkForTest.STORE_HEART.extraPath
            )
        )
        context.startActivity(intent)

        // 한글만 테스트 해주세요.
        assertTureInputText(LinkForTest.STORE_HEART.visibleOfTextOnView)
    }

    @Test
    fun click_STORE_PACKAGE_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.STORE_PACKAGE.lastPathSegment,
                LinkForTest.STORE_PACKAGE.extraPath
            )
        )
        context.startActivity(intent)

        // 한글만 테스트 해주세요.
        assertTureInputText(context.getString(R.string.welcome_pack_title))
    }

    @Test
    fun click_STORE_DIA_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(
            getLink(
                context,
                LinkForTest.STORE_DIA.lastPathSegment,
                LinkForTest.STORE_DIA.extraPath
            )
        )
        context.startActivity(intent)

        // 한글만 테스트 해주세요.
        assertTureInputText(context.getString(R.string.level_heart_guide_title4))
    }

    @Test
    fun click_MY_FEED_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.MY_FEED.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.feed_my_feed))
    }

    @Test
    fun click_MY_INFO_link() {

        uiDevice.pressHome()

        val intent = getDefaultIntent(getLink(context, LinkForTest.MY_INFO.lastPathSegment))
        context.startActivity(intent)

        assertTureInputText(context.getString(R.string.my_info))
    }

    @Test
    fun click_COMMUNITY_link() {

        val myMost = IdolAccount.getAccount(context)?.most

        val baseLink = getLink(context, LinkForTest.COMMUNITY.lastPathSegment)
        val baseUri = Uri.parse(baseLink)

        val uri = baseUri.buildUpon()
            .appendQueryParameter("idol", getMostId().first.toString())
            .appendQueryParameter("group", getMostId().second.toString())
            .appendQueryParameter("locale", "ko")
            .build()

        val intent = getDefaultIntent(uri.toString())
        context.startActivity(intent)
        assertTureInputText(myMost?.getName(context) ?: "")
    }

    @Test
    fun click_COMUNITY_HOME_link() {

        val baseLink = getLink(context, LinkForTest.COMMUNITY.lastPathSegment)
        val baseUri = Uri.parse(baseLink)

        val uri = baseUri.buildUpon()
            .appendQueryParameter("idol", getMostId().first.toString())
            .appendQueryParameter("group", getMostId().second.toString())
            .appendQueryParameter("locale", "ko")
            .appendQueryParameter("tab", "community")
            .build()

        val intent = getDefaultIntent(uri.toString())
        context.startActivity(intent)
        assertTrueViewExist("${context.packageName}:id/tv_filter")
    }

    @Test
    fun click_COMUNITY_CHATTING_link() {

        val baseLink = getLink(context, LinkForTest.COMMUNITY.lastPathSegment)
        val baseUri = Uri.parse(baseLink)

        val uri = baseUri.buildUpon()
            .appendQueryParameter("idol", getMostId().first.toString())
            .appendQueryParameter("group", getMostId().second.toString())
            .appendQueryParameter("locale", "ko")
            .appendQueryParameter("tab", "idoltalk")
            .build()

        val intent = getDefaultIntent(uri.toString())
        context.startActivity(intent)
        assertTrueViewExist("${context.packageName}:id/tv_filter_entire")
    }

    @Test
    fun click_COMUNITY_SCHEDULE_link() {

        val baseLink = getLink(context, LinkForTest.COMMUNITY.lastPathSegment)
        val baseUri = Uri.parse(baseLink)

        val uri = baseUri.buildUpon()
            .appendQueryParameter("idol", getMostId().first.toString())
            .appendQueryParameter("group", getMostId().second.toString())
            .appendQueryParameter("locale", "ko")
            .appendQueryParameter("tab", "schedule")
            .build()

        val intent = getDefaultIntent(uri.toString())
        context.startActivity(intent)
        assertTrueViewExist("${context.packageName}:id/tv_text_language")
    }

    private fun assertTureInputText(text: String) {
        val uniqueText = uiDevice.findObject(UiSelector().text(text))
        assertTrue(uniqueText.waitForExists(LAUNCH_TIME_OUT)) // 5초 내에 해당 요소가 나타나는지 확인
    }

    private fun assertTrueViewExist(resourceId: String) {
        val targetView = uiDevice.findObject(UiSelector().resourceId(resourceId))
        assertTrue(targetView.waitForExists(LAUNCH_TIME_OUT))
    }

    private fun getMostId(): Pair<Int?, Int?> {
        val myMost = IdolAccount.getAccount(context)?.most

        val idolId = myMost?.getId()
        val groupId = if (BuildConfig.CELEB) myMost?.getGroup_id() else myMost?.groupId

        return Pair(idolId, groupId)
    }

    private fun getDefaultIntent(link: String) = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun getLink(context: Context, lastPathSegment: String, extraPath: String = ""): String {
        var host = Util.getPreference(context, Const.PREF_SERVER_URL)
        if (host.isNullOrEmpty()) {
            host = ServerUrl.HOST
        }
        return if (extraPath.isEmpty()) {
            "${host}/${lastPathSegment}"
        } else {
            "${host}/${extraPath}/${lastPathSegment}"
        }
    }

    companion object {
        const val LAUNCH_TIME_OUT = 10000L
    }

}