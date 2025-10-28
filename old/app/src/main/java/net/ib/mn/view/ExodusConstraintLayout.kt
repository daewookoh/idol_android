package net.ib.mn.view

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.AutoClickerDetector
import net.ib.mn.utils.Const
import net.ib.mn.utils.Const.AUTO_CLICK_COUNT_PREFERENCE_KEY
import net.ib.mn.utils.Util

/**
 * ProjectName: idol_app_renew
 *
 * Description:  접근성 기능 관련  뷰의 터치 이벤트 intercept를 위해
 * 만든  커스텀  costraintlayout 이다.
 * 혹시나  나중에  더 constraint layout 관련  기능 확장이 필요하면,  이곳에서 추가 해주세요!
 * */
class ExodusConstraintLayout:ConstraintLayout {
    private var isVideoItem = true
    private var isHeartPlusActivity =false

    private var gson:Gson = IdolGson.getInstance()
    private val banPackageListType = object : TypeToken<ArrayList<String>>() {}.type

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    var onBanAutoClicker: (() -> Unit)? = null

    //무료 충전소에서는  비디오 아이템만 로그를 보내야 하므로,
    //비디오 아이템인지 여부를  체크하는 함수를 추가함.
    fun checkVideoItem(checkVideoItem: Boolean,isHeartPlusActivity: Boolean){
        this.isVideoItem = checkVideoItem
        this.isHeartPlusActivity=isHeartPlusActivity
    }
    //childe 뷰의  터치 이벤트를  가지고와서  accessiblity 가  켜져잇는 경우엔 서버로  로그를 보낸다.
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if(isVideoItem) {
            //터치 타입의  접근성 기능을 사용하는  서비스들의  리스트
            val enabledServices = AutoClickerDetector.checkAccessibilityServicesEnabled(context)

            //서버로부터 받아온  매크로툴 리스트
            val macroToolsList =ConfigModel.getInstance(context).macroTools

            //서버로 부터 받아온  매크로 패키지 이름들을  넣어줌.
            val macroPackagesList=ArrayList<String>()
            if(macroToolsList != null && macroToolsList.length()>0){//매크로 툴 리스트 값이 null이 아니거나 1이상이라면 패키지이름 넣어줌.
                for(i in 0 until  macroToolsList.length()){
                    macroPackagesList.add(macroToolsList[i].toString())
                }
            }


            //감지된  서비스들이 잇을 경우만 서버로 로그를 보내준다.
            if (!enabledServices.isNullOrEmpty()) {

                //서버로부터 블락당한  패키지 존재 여부를 체크 한다.
                var isBannedPackageExist = false


                    //autoclick 의심 되는 서비스의  패키지명  리스트
                    val autoClickPkgs = mutableListOf<String>()
                    for (i in 0 until enabledServices.size) {
                        val info: AccessibilityServiceInfo = enabledServices[i]

                        //패키지명이 null 이 아닐 경우  담아준다.
                        if (!info.resolveInfo.serviceInfo.packageName.isNullOrEmpty()) {
                            //서버에서 지정한 매크로 패키지 리스트에  포함되는 패키지일 경우.
                            if (macroPackagesList.contains(info.resolveInfo.serviceInfo.packageName)) {
                                isBannedPackageExist = true

                                //autocliker  count가  max count를 넘었을때는   휴먼체크 팝업을 보여준다.
                                if(Util.getPreferenceInt(context,AUTO_CLICK_COUNT_PREFERENCE_KEY,0) >= Const.AUTO_CLICKER_MAX_COUNT) {

                                    // auto clicker 가 count가 max를 넘을때 ->  비광 적립 대기 타이머가 진행중이라면  휴먼체크 팝업을 안보여주고  그냥 넘어가게 한다. -> count도 안올림
                                     if(Util.getPreferenceLong(context, Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY, Const.DEFAULT_VIDEO_DISABLE_TIME) ==  Const.DEFAULT_VIDEO_DISABLE_TIME){


                                            //휴먼체크 팝업이 나오면 auto click count 는 다시 reset 해준다.
                                            Util.setPreference(context,AUTO_CLICK_COUNT_PREFERENCE_KEY,0)
                                            this.isClickable = isHeartPlusActivity//휴면체크 팝업이 뜰때는   광고 화면으로 넘어가면 안되므로, click을 막음.

                                            //휴면 체크 팝업 실행
                                            Util.showHumanCheckDialog(context) {
                                                isBannedPackageExist = if (it) {//정답이 맞을때

                                                    //클릭 가능여부 다시 풀어줌.
                                                    this.isClickable = !isHeartPlusActivity

                                                    Util.closeIdolDialog()
                                                    (context as Activity).startActivityForResult(
                                                        MezzoPlayerActivity.createIntent(context, Const.ADMOB_REWARDED_VIDEO_FREECHARGE_UNIT_ID)
                                                        , BaseActivity.Companion.MEZZO_PLAYER_REQ_CODE
                                                    )

                                                    //서버에 블락당한  패키지 존재여부 true
                                                    false
                                                } else {//정답이 틀릴때

                                                    //클릭 가능여부 다시 풀어줌.
                                                    this.isClickable = !isHeartPlusActivity

                                                    //휴먼체크 팝업 close
                                                    Util.closeIdolDialog()

                                                    //문제를 틀렸으므로  오답 팝업을 띄움
                                                    Util.showDefaultIdolDialogWithBtn1(context,null, context.getString(R.string.auto_clicker_human_check_fail)) {

                                                        //user 비광 ban 용  api 부름
                                                        banAutoClicker()
                                                        isBannedPackageExist =true
                                                        Util.closeIdolDialog()
                                                    }

                                                    //서버에 블락당한  패키지 존재여부 true
                                                    true
                                                }
                                        }

                                    }else{ //타이머 적립 타이머 적용중이므로, 휴면체크 팝업 x이므로 for문 바로 break 처리
                                        break//for문  break;
                                    }

                                }else{//maxcount를 안넘었을때

                                    if(Util.getPreferenceInt(context,AUTO_CLICK_COUNT_PREFERENCE_KEY,0) != Const.AUTO_CLICKER_BAN_CONFIRM_VALUE){
                                        //블락된 접근성 패키지를 사용하지만, play 횟수가  max count를 넘지 않고 비광밴이 아직 안당한  경우는 비광을 실행하고,
                                        //auto click count를  1씩 올려준다.
                                        Util.setPreference(context, AUTO_CLICK_COUNT_PREFERENCE_KEY, Util.getPreferenceInt(context, AUTO_CLICK_COUNT_PREFERENCE_KEY, 0) + 1)
                                    }

                                }
                            } else {//서버의 매크로 리스트에 없는 경우 일단 감지된 접근성 패키지 정보 넣어줌.
                                autoClickPkgs.add(info.resolveInfo.serviceInfo.packageName)
                            }
                        }
                    }//for문 끝



                //return된 패키지 리스트는 아직 보내지 않은 패키지들이므로, 서버에 해당  패키지 목록을 보내준다.
                // 임시 제거 (향후 필요할 경우 이 파일 바깥에서 처리하는 방식으로 변경)
//                checkNewPackageExist(autoClickPkgs as ArrayList<String>).apply {
//                    if(isNotEmpty()){
//
//                        for(i in 0 until this.size){
//                            //서버로 보낼로그 작성
//
//                            Util.postVMLog(context, this[i], Const.ACCESSIBILITY_DETECT_LOG)
//                        }
//                    }
//                }

                if (!isBannedPackageExist) {//서버에서  블락된 패키지가 하나도 없는 경우에는 클릭 허용하고,  서버로 로그를 보내준다.
                    this.isClickable = !isHeartPlusActivity
                }

            }else{//접근성 서비스가 아예 없을때는 클릭 허용
                this.isClickable = !isHeartPlusActivity
            }
        }
        return false
    }

    //감지된 패키지명  로컬에 저장

    //감지된 접근성 패키지 중   로컬에 캐싱된거있는지 체크해서
    //캐싱이 안된 패키지의 경우,  return 시켜준다.->  서버에 로그로 보내기위해
    private fun checkNewPackageExist(autoClickPkgs:ArrayList<String>):ArrayList<String>{
        val newCheckedPackage = ArrayList<String>()//새롭게 감지된 패키지 리스트
            try {

                //기존  서버에 이미 보내고  로컬에 캐싱된  접근성 패키지 리스트
                var bannedPackageList: ArrayList<String>? = gson.fromJson(Util.getPreference(context, Const.AUTO_CLICK_CACHE_PREF_KEY), banPackageListType)
                if(bannedPackageList == null){
                   bannedPackageList = ArrayList()
                }

                //새롭게 감지된 패키지가 있는지 체크후, 있으면  추가해서 쉐어드에 저장
                autoClickPkgs.forEach {
                    if(bannedPackageList.isNullOrEmpty() || !bannedPackageList.contains(it.trim())){
                        bannedPackageList.add(it.trim())
                        newCheckedPackage.add(it.trim())
                        Util.setPreference(context,Const.AUTO_CLICK_CACHE_PREF_KEY,bannedPackageList.toString())
                    }
                }
            }catch (e:Exception){
                //혹시나  문제 생기면  로컬 캐싱한거 다 지워주고  처음부터 reset
                Util.removePreference(context,Const.AUTO_CLICK_CACHE_PREF_KEY)
                e.printStackTrace()
            }
        return newCheckedPackage
    }

    private fun banAutoClicker() {
        onBanAutoClicker?.invoke()
    }
}