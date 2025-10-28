package com.exodus.bridge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject

/**
 * Copyright 2024-10-30 ExodusEnt Corp. All rights reserved.
 *
 * @author parkboo
 * Description: app 모듈과 core 모듈 사이에서 데이터를 공유하기 위한 클래스
 *  Retrofit으로 전환하면서 88888 처리 등을 app에서 손쉽게 처리하기 위함
 *
 **/

class SharedBridgeManager private constructor() {
    companion object {
        private val _sharedData = MutableLiveData<SharedData>() // gcode, msg로 구성된 pair
        val sharedData: LiveData<SharedData> get() = _sharedData

        fun getData(): SharedData? = _sharedData.value
        fun setData(gcode: Int, mcode: Int, msg: String) {
            _sharedData.postValue( SharedData(gcode, mcode, msg) )
        }

        fun setData(jsonObject: JSONObject) {
            val gcode = jsonObject.optInt("gcode")
            val mcode = jsonObject.optInt("mcode")
            val msg = jsonObject.optString("msg")
            _sharedData.postValue( SharedData(gcode, mcode, msg) )
        }

        fun clearData() {
            _sharedData.postValue(SharedData())
        }
    }
}