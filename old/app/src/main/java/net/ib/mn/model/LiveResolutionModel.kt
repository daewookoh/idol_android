package net.ib.mn.model

import java.io.Serializable


/**
 * ProjectName: idol_app_renew
 *
 * Description: 라이브에서 해상도 관련 모델 설정
 * 해상도의  width 와  height 그리고   auto 여부,  선택됨 여부를 체크한다.
 *
 * */
data class LiveResolutionModel(
     var width: Int =0 ,
     var height: Int =0,
     var isAutoResolution: Boolean = false,
     var isSelected: Boolean = false
):Serializable
