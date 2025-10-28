package net.ib.mn.support

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import net.ib.mn.utils.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.SupportAdPickAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.ActivitySupportAdPickBinding
import net.ib.mn.model.SupportListModel
import com.bumptech.glide.Glide
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.model.SupportAdType
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import java.util.Calendar

class SupportAdPickActivity : BaseActivity(),
        SupportAdPickAdapter.OnClickListener,
        View.OnClickListener{


    private lateinit var adList:ArrayList<SupportAdTypeListModel>
    private lateinit var supportAdPickAdapter:SupportAdPickAdapter
    private lateinit var mGlideRequestManager:RequestManager

    private var selectedAd : SupportAdTypeListModel? = null
    private var isForAdPick:Boolean = false

    private lateinit var binding: ActivitySupportAdPickBinding
    private val selectTagList = arrayListOf(SupportAdType.KOREA, SupportAdType.MOBILE, SupportAdType.FOREIGN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_ad_pick)
        binding.clSupportAdPick.applySystemBarInsets()

        isForAdPick = intent.getBooleanExtra(PARAM_IS_FOR_AD_PICK,false)

        //SupportAdPickActivity로 들어온 이유가
        //광고 종류 보기인지,  광고 선택인지를 구별해서 그에 맞게  view set을 진행
        if(isForAdPick){
            initSetForAdPick()
        }else{
            initSetForAdType()
        }


        adList = ArrayList()
        mGlideRequestManager = Glide.with(this)

        val llm = LinearLayoutManager(this)

        setAdTypeList()
        supportAdPickAdapter = SupportAdPickAdapter(this,
        mGlideRequestManager,
                isForAdPick,
                this)

        binding.supportAdPickRv.layoutManager = llm
        supportAdPickAdapter.setHasStableIds(true)
        binding.supportAdPickRv.adapter = supportAdPickAdapter

        binding.supportAdPickBtn.setOnClickListener(this)
        setTagEventListener()

        supportAdPickAdapter.setItems(adList)
    }

    //광고 종류를 위해서 왔을때  initSet
    private fun initSetForAdType(){
        supportActionBar?.setTitle(R.string.support_ad_types)
        binding.tvAdTypeListTitle.visibility = View.GONE
        binding.vShadow.visibility = View.GONE
        binding.layoutSupportAdPickBtn.visibility = View.GONE
    }


    //광고 선택을 위해 왔을때  initSet
    private fun initSetForAdPick(){
        supportActionBar?.setTitle(R.string.support_create_type)
        binding.tvAdTypeListTitle.visibility = View.VISIBLE
        binding.vShadow.visibility = View.VISIBLE
        binding.layoutSupportAdPickBtn.visibility = View.VISIBLE

    }


    private fun setAdTypeList() {
        binding.tvAllTag.isSelected = true

        try{
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<SupportAdTypeListModel>>() {}.type
            val parseAdlist:ArrayList<SupportAdTypeListModel> = gson.fromJson(Util.getPreference(this, Const.AD_TYPE_LIST), listType)
            //광고중 단종된건 리스트에 추가하지않는다. 
            for(i in parseAdlist.indices){
                if(parseAdlist[i].isViewable == "Y")
                    adList.add(parseAdlist[i])
            }
        }catch (e:IllegalStateException){
            e.printStackTrace()
        }
    }

    private fun setTagEventListener() {
        val mainTagList = arrayListOf(
            binding.tvAllTag,
            binding.tvDomesticTag,
            binding.tvForeignTag,
            binding.tvMobileTag
        )

        // 태그에 매핑된 SupportAdType 값 설정
        val tagMapping = mapOf(
            binding.tvDomesticTag to SupportAdType.KOREA,
            binding.tvMobileTag to SupportAdType.MOBILE,
            binding.tvForeignTag to SupportAdType.FOREIGN
        )

        // 공통 동작을 처리하는 함수
        fun setupTagListeners(allTag: View, tagList: List<View>, selectedTags: MutableList<SupportAdType>) {
            allTag.setOnClickListener {
                tagList.forEach { tag ->
                    tag.isSelected = false
                }
                allTag.isSelected = true

                // selectTagList를 모든 SupportAdType으로 초기화
                selectedTags.clear()
                selectedTags.addAll(SupportAdType.values())

                val filteredItems = adList.filter { item ->
                    selectedTags.any { category -> category.label == item.category }
                }

                supportAdPickAdapter.setItems(filteredItems)
            }

            tagList.forEach { tag ->
                tag.setOnClickListener {
                    val shouldSelect = !tag.isSelected

                    allTag.isSelected = false
                    tagList.forEach { it.isSelected = false }

                    selectedTags.clear()

                    if (shouldSelect) {
                        tag.isSelected = true
                        tagMapping[tag]?.let { selectedTags.add(it) }
                    }

                    if (selectedTags.isEmpty()) {
                        allTag.isSelected = true
                        selectedTags.addAll(SupportAdType.values())
                    }

                    val filteredItems = adList.filter { item ->
                        selectedTags.any { category -> category.label == item.category }
                    }

                    supportAdPickAdapter.setItems(filteredItems)
                }
            }
        }

        setupTagListeners(binding.tvAllTag, mainTagList.filter { it != binding.tvAllTag }, selectTagList)
    }

    override fun onItemClicked(item: SupportAdTypeListModel, view: View, position: Int) {

       if(isForAdPick){//광고 선택시  아이템 클릭 이벤트
           // 기존 체크 지우고
           for( ad in adList ) {
               ad.selected = false
           }
           item.selected = true
           selectedAd = item
           supportAdPickAdapter.notifyDataSetChanged()

           binding.supportAdPickBtn.background = ContextCompat.getDrawable(this, R.drawable.main_radius25)
           binding.supportAdPickBtn.isEnabled = true
       }else{
           startActivity(SupportAdInfoActivity.createIntent(this,item))
       }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.support_ad_pick_btn -> {
                val data = Intent()
                data.putExtra("support_ad_pick", selectedAd?.id)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

    companion object{

        const val PARAM_IS_FOR_AD_PICK = "isForShowAdPick"

        @JvmStatic
        fun createIntent(context:Context, isForAdPick:Boolean): Intent{
            val intent =  Intent(context, SupportAdPickActivity::class.java)
            intent.putExtra(PARAM_IS_FOR_AD_PICK,isForAdPick)
            return intent
        }
    }
}