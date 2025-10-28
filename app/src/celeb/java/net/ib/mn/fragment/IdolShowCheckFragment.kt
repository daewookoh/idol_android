package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.annotation.OptIn
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.ib.mn.R
import net.ib.mn.databinding.IdolShowCheckFragmentBinding
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.UnstableApi
import net.ib.mn.adapter.IdolShowCheckAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.model.IdolTypeModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.utils.*

/**
 * 셀럽 카테고리 선택 fragment
 */

@androidx.media3.common.util.UnstableApi
class IdolShowCheckFragment : BaseFragment(), View.OnClickListener, IdolShowCheckAdapter.OnItemClickListener {

    private var typeList:String?=null
    private var types : ArrayList<TypeListModel> = ArrayList()  //서버에서 준 TypeList값 저장하는 ArrayList
    private var categorylist:ArrayList<IdolTypeModel> = ArrayList<IdolTypeModel>() //카테고리 체크되어있는지 없는지 필요해서 만들었었음(로컬 저장)
    private var scrollEnd = false   //스크롤 마지막 체크용
    private lateinit var mIdolShowCheckAdapter : IdolShowCheckAdapter
    public lateinit var binding: IdolShowCheckFragmentBinding

    var tempArrayList:ArrayList<IdolTypeModel> = ArrayList<IdolTypeModel>() // 카테고리 체크박스 true 모아놓은 변수
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = IdolShowCheckFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gson = IdolGson.getInstance()
        val listType = object : TypeToken<ArrayList<IdolTypeModel>>() {}.type

        if(!Util.getPreference(context, Const.MAIN_CHECK_VIEW).isNullOrEmpty()) {
            categorylist = gson.fromJson(Util.getPreference(context, Const.MAIN_CHECK_VIEW), listType)
        }
        tempArrayList = SummaryMainFragment.checkTrueList


        typeList = Util.getPreference(context, Const.PREF_TYPE_LIST)
        val listType2 = object : TypeToken<List<TypeListModel>>() {}.type
       try {
           types = gson.fromJson<ArrayList<TypeListModel>>(typeList, listType2)
           types = types.filter { it.isViewable == "Y" } as ArrayList<TypeListModel>
           mGlideRequestManager = Glide.with(this)
           mIdolShowCheckAdapter = IdolShowCheckAdapter(types, categorylist){ category, view, typeListModel, position ->
               categoryOnOff(category, view, typeListModel, position)
           }
           binding.rvShowCheck.apply {
               adapter = mIdolShowCheckAdapter
           }
       }catch (e:Exception){
           e.printStackTrace()
       }
        binding.imgArrowState.setOnClickListener(this)
        setRecyclerViewScrollListener()
    }

    private fun idolTypeModelAdd(CheckFilter : Boolean, idx : Int){
        val gson = IdolGson.getInstance()

        categorylist[idx].checkFilter = CheckFilter

        Util.setPreference(requireActivity(),Const.MAIN_CHECK_VIEW,gson.toJson(categorylist).toString())
    }

    /*
    //체크박스 클릭할 때 상황에 따라 분리
    private fun checkBoxClick(button: CheckBox, category: String?, type:String?, idx : Int, female : Boolean){
        if(button.isChecked){
            val gson = IdolGson.getInstance()
            val bundle = Bundle()

            //메인 카테고리 설정 ->  구글  애널리틱스 ui 이벤트 적용 -> 스위치 on
            setUiActionFirebase(Const.ANALYTICS_SWITCH_ON,type)

            if(type == "" && category == ""){
                bundle.putString("type", gson.toJson(types[0]))
                bundle.putInt(SummaryMainFragment.MAIN_INDEX, 0)
            } else if(type == "AF") {
                bundle.putString("type", gson.toJson(types[1]))
                bundle.putString(Const.KEY_CATEGORY, "F")
                bundle.putInt(SummaryMainFragment.MAIN_INDEX, 2)
            }
            else if(type == "AM"){
                bundle.putString("type", gson.toJson(types[1]))
                bundle.putInt(SummaryMainFragment.MAIN_INDEX, 1)
            }
            else if(type == "SF"){
                bundle.putString("type", gson.toJson(types[2]))
                bundle.putString(Const.KEY_CATEGORY, "F")
                bundle.putInt(SummaryMainFragment.MAIN_INDEX, 4)
            }else if(type == "SM"){
                bundle.putString("type", gson.toJson(types[2]))
                bundle.putInt(SummaryMainFragment.MAIN_INDEX, 3)
            }
            else if(type == "E"){
                bundle.putString("type", gson.toJson(types[3]))
                bundle.putInt(SummaryMainFragment.MAIN_INDEX, 5)
            }


            val summaryFragment = RankingPageFragment()
            summaryFragment.arguments = bundle

            //페이지 추가는 무조건 앞에서 부터한다(정렬페이지 뒤로가면 안됨)
            SummaryMainFragment.mSummaryAdapter?.addInFirst(summaryFragment)

            idolTypeModelAdd(true,category,type, idx)
        }
        else{
            if(lastFalseCheck()==1){
                button.isChecked = true
                Util.showDefaultIdolDialogWithBtn1(context,null,requireActivity().getString(R.string.necessarily_one_category)
                ) { Util.closeIdolDialog() }
            }
            else{

                //메인 카테고리 설정 ->  구글  애널리틱스 ui 이벤트 적용 -> 스위치 off
                setUiActionFirebase(Const.ANALYTICS_SWITCH_OFF,type)
                removeFragmentCheck(type,category)
                idolTypeModelAdd(false,category,type, idx)
            }
        }
    }
    */

    //메인 카테고리 설정 ->  구글  애널리틱스 ui 이벤트 적용
    private fun setUiActionFirebase(switchAction:String?,type:String?){
        when(type){
            ""->{"all_category" }
            "SM"->{"singer_male"}
            "SF"->{"singer_female"}
            "AM"->{"actor_male"}
            "AF"->{"actor_female"}
            "E"->{"entertainer_all"}
            else ->{""}
        }.apply {
          //label값 있으면  GoogleAnalytics 처리 진행
          if(this.isNotEmpty())
            setUiActionFirebaseGoogleAnalyticsFragment(
                switchAction,
                this
            )
        }
    }


    override fun onClick(v: View) {
        with(binding) {
            when(v.id) {
                imgArrowState.id -> {
                    if(!scrollEnd) {
                        rvShowCheck.smoothScrollToPosition(typeList!!.lastIndex)  //버튼 누르면 맨 아래로 가짐.
                        imgArrowState.setImageResource(R.drawable.img_arrow)
                        scrollEnd = true
                    }
                    else{
                        rvShowCheck.smoothScrollToPosition(0)
                        imgArrowState.setImageResource(R.drawable.img_arrow_down)
                        scrollEnd = false
                    }
                }
            }
        }
    }

    @androidx.media3.common.util.UnstableApi
    private fun removeFragmentCheck(type:String?, category:String?){

        for (i in 0 until SummaryMainFragment.mSummaryAdapter?.fragments!!.size) {
            if (SummaryMainFragment.mSummaryAdapter!!.fragments[i] is RankingPageFragment) {
                val fragment = SummaryMainFragment.mSummaryAdapter!!.fragments[i] as RankingPageFragment

                if (fragment.type == type && fragment.typeCheck() == category) {
                    SummaryMainFragment.mSummaryAdapter!!.remove(fragment)
                    SummaryMainFragment.mSummaryAdapter!!.notifyDataSetChanged()
                    break
                }
            }
        }
    }

    private fun isOnlyOneCheck() : Boolean {
        return categorylist.count { it.is_viewable == "Y" && it.checkFilter } == 1
    }

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun categoryOnOff(
        category: String?,
        view: View,
        typeListModel: TypeListModel,
        position: Int
    ) {
        if((view as CheckBox).isChecked){
            val gson = IdolGson.getInstance()
            val bundle = Bundle()

            //메인 카테고리 설정 ->  구글  애널리틱스 ui 이벤트 적용 -> 스위치 on
            setUiActionFirebase(Const.ANALYTICS_SWITCH_ON, typeListModel.type)
            bundle.putString("type", gson.toJson(typeListModel))
            bundle.putInt(SummaryMainFragment.MAIN_INDEX, position)
            if(typeListModel.isFemale){ //여자일때 구분.
                bundle.putString(Const.KEY_CATEGORY, "F")
            }

            val summaryFragment = RankingPageFragment()
            summaryFragment.arguments = bundle

            //페이지 추가는 무조건 앞에서 부터한다(정렬페이지 뒤로가면 안됨)
            SummaryMainFragment.mSummaryAdapter?.addInFirst(summaryFragment)

            idolTypeModelAdd(true, position)
        } else {
            if(isOnlyOneCheck()){
                view.isChecked = true
                Util.showDefaultIdolDialogWithBtn1(context,null,requireActivity().getString(R.string.necessarily_one_category)
                ) { Util.closeIdolDialog() }
            }
            else{
                //메인 카테고리 설정 ->  구글  애널리틱스 ui 이벤트 적용 -> 스위치 off
                setUiActionFirebase(Const.ANALYTICS_SWITCH_OFF,typeListModel.type)
                removeFragmentCheck(typeListModel.type, category)
                idolTypeModelAdd(false, position)
            }
        }
    }

    private fun setRecyclerViewScrollListener(){
        with(binding) {
            rvShowCheck.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    val totalItemCount = (rvShowCheck.layoutManager as LinearLayoutManager).itemCount
                    val firstVisibleItems = (rvShowCheck.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                    val lastVisibleItems = (rvShowCheck.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                    if(firstVisibleItems==0){                       //스크롤할 때 첫 아이템이 보인다면 아래로 내려주는 버튼 보이게
                        imgArrowState.setImageResource(R.drawable.img_arrow_down)
                        scrollEnd = false
                    }
                    else if(lastVisibleItems==totalItemCount-1){    //스크롤할 때 마지막 아이템이 보인다면 위로 올려주는 버튼 보이게
                        imgArrowState.setImageResource(R.drawable.img_arrow)
                        scrollEnd = true
                    }
                }
            })
        }
    }
}

