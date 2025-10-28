/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 링크 관련 유틸 클래스.
 *
 * */

package net.ib.mn.utils.link

import android.content.Context
import android.net.Uri
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.UtilK
import java.net.URL

/**
 * @see getAppLinkUrl parasms, query에 따른 uri 만들어주는 함수.
 * */

object LinkUtil {

    fun getAppLinkUrl(context : Context, params: List<String>, querys: List<Pair<String, String>>? = null): String {

        val sharedUrl = if (ServerUrl.HOST == ServerUrl.HOST_TEST) {
            ServerUrl.HOST_BBB_TEST // 테섭일땐 공유 링크 bbb로 무조건 해달라는 요청이 있었습니다.
        } else {
            ServerUrl.HOST
        }

        val url = URL(sharedUrl)
        val scheme = url.protocol
        val authority = url.host

        val uriBuilder = Uri.Builder().scheme(scheme)
            .authority(authority)

        params.forEach { param ->
            uriBuilder.appendPath(param)
        }

        //뒤에 / 붙여주기로 웹이랑 합의봄.
        uriBuilder.encodedPath(uriBuilder.build().encodedPath + "/")

        querys?.forEach { query ->
            uriBuilder.appendQueryParameter(query.first, query.second)
        }

        // 언어 설정은 무조건 들어가게 해준다.
        uriBuilder.appendQueryParameter("locale", UtilK.getShareLocale(context))

        Logger.v("UriTest :: ${uriBuilder.build().toString()}")
        return uriBuilder
            .build()
            .toString()
    }

    fun getAppLinkUrlForWebView(context : Context, params: List<String>, querys: List<Pair<String, String>>? = null): String {

        val sharedUrl = if (ServerUrl.HOST == ServerUrl.HOST_TEST) {
            ServerUrl.HOST_BBB_TEST // 테섭일땐 공유 링크 bbb로 무조건 해달라는 요청이 있었습니다.
        } else {
            ServerUrl.HOST
        }

        val url = URL(sharedUrl)
        val scheme = url.protocol
        val authority = url.host

        val uriBuilder = Uri.Builder().scheme(scheme)
            .authority(authority)

        params.forEach { param ->
            uriBuilder.appendPath(param)
        }

        //뒤에 / 붙여주기로 웹이랑 합의봄.
        uriBuilder.encodedPath(uriBuilder.build().encodedPath + "/${UtilK.getShareLocale(context)}" + "/")

        querys?.forEach { query ->
            uriBuilder.appendQueryParameter(query.first, query.second)
        }

        Logger.v("WebView UriTest :: ${uriBuilder.build().toString()}")
        return uriBuilder
            .build()
            .toString()
    }
}