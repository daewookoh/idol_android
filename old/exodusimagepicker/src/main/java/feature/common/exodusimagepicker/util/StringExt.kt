/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package feature.common.exodusimagepicker.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * @see modelToString 데이터 모델 확인용.
 * */

//model을  json형식의  string으로 변환 한다.
fun Any.modelToString(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(this)
}

//json 형식의 string을 model로 변환한다.
inline fun <reified T : Any> String.stringToModel(): T? {
    val gson = Gson()
    return try {
        gson.fromJson(this, T::class.java)
    } catch (e: Exception) { //혹시나 string이  model 포맷에 안맞으면  null을  리턴해줌.
        null
    }
}

//json  변환시 com.google.gson.internal.LinkedTreeMap cannot be cast to my class 이런 에러 나오는 경우 있어서, 이경우에는 typetoken으로 처리
//출처: https://juntcom.tistory.com/126 [쏘니의 개발블로그]
inline fun <reified T : Any> String.stringToModelWithTypeToken(): T? {
    val gson = Gson()
    return try {
        gson.fromJson<T>(this, object : TypeToken<T>() {}.type)
    } catch (e: Exception) { //혹시나 string이  model 포맷에 안맞으면  null을  리턴해줌.
        null
    }
}