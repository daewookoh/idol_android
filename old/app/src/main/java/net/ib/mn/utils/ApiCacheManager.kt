package net.ib.mn.utils

import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by parkboo on 2018. 1. 16..
 */
class ApiCacheManager {
    companion object {
        val ourInstance = ApiCacheManager()

        @JvmStatic fun getInstance(): ApiCacheManager {
            return ourInstance
        }
    }

    data class ResponseData( val created : Long, val ttl: Long, val key: String, val jsonObject: JSONObject, val persistent : Boolean ) {

    }

    private val responses = ConcurrentHashMap<String, ResponseData>()

    fun putCache(key: String, response: JSONObject, ttl: Long) {
        val data = ResponseData( System.currentTimeMillis(),System.currentTimeMillis() + ttl, key, response, false )
        responses.put(key, data)
    }

    fun putCache(key: String, response: JSONObject, ttl: Long, persistent: Boolean) {
        val data = ResponseData( System.currentTimeMillis(),System.currentTimeMillis() + ttl, key, response, persistent )
        responses.put(key, data)
    }

    // 캐시에 들어간 시간. 일정 시간이 지나면 api 다시 부르게 하기 위함
    fun getCacheTime(key: String) : Long {
        val response = responses.get(key)
        if( response != null && response.created > 0 ) {
            return response.created
        }

        return 0
    }

    // 지정된 시간 이내에 같은 api 요청시 캐시된 응답 돌려주기
    fun getCache(key: String) : JSONObject? {
        val response = responses.get(key)
        if( response == null || System.currentTimeMillis() > response.ttl ) {
            Util.log("... cache miss: $key")
            return null
        }

        Util.log("!!! cache hit: $key")
        return response.jsonObject
    }

    fun clearCache(key: String) {
        responses.remove(key)
    }

    @Synchronized fun resetCache() {
        Util.log("!!!!!! reseting cache !!!!!!")

        val keyset = responses.keys
        try {
            for( key in keyset ) {
                Util.log("key="+key);
                val data : ResponseData? = responses[key]
                if( data != null && !data.persistent ) {
                    responses.remove(key)
                    Util.log("   deleted.");
                }
            }
        } catch( e: Exception ) {
            e.printStackTrace()
        }
    }

}