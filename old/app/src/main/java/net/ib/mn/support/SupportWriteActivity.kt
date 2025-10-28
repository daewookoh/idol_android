package net.ib.mn.support

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.adapter.BottomSheetAdItemAdapter
import net.ib.mn.adapter.SupportSearchAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapListDataResource
import net.ib.mn.databinding.ActivitySupportWriteBinding
import net.ib.mn.dialog.BaseDialogFragment.DialogResultHandler
import net.ib.mn.dialog.SupportDateTimePickerDialogFragment
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.*
import net.ib.mn.utils.ext.applySystemBarInsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class SupportWriteActivity : BaseActivity(),
        View.OnClickListener,
        BottomSheetAdItemAdapter.AdItemListener ,
        SupportSearchAdapter.OnClickListener,
        DialogResultHandler{

    //사진
    private var binImage: ByteArray? = null
    private var mGlideRequestManager: RequestManager? = null
    private var flag = true
    private var useSquareImage = true

    //날짜
    private lateinit var calendar: Calendar
    private var setDate: Date?=null
    private var limitedDate: Date? = null
    private lateinit var expiredDate: Date

    //나의 최애
    private var idolModel: IdolModel? = null

    //앱 광고 리스트
    private lateinit var mBottomSheetFragment: BottomSheetFragment
    private lateinit var selectionAd: SupportAdTypeListModel
    private var day: String? = null
    private var week: String? = null
    private var month: String? = null
    private lateinit var adList:ArrayList<SupportAdTypeListModel>

    //광고 상세화면 넘어온 값.
    private var resultId:Int?=null

    //검색.
    private lateinit var mListView: ListView
    private lateinit var idolList:ArrayList<IdolModel>
    private lateinit var adapter:SupportSearchAdapter
    private var searchFalg = true

    //내 다이아몬드
    private var myTotalDia:Int?=null

    private lateinit var binding: ActivitySupportWriteBinding

    @Inject
    lateinit var supportRepository: SupportRepositoryImpl

    @Inject
    lateinit var getAllIdolsUseCase: GetAllIdolsUseCase
    @Inject
    lateinit var accountManager: IdolAccountManager

    companion object{

        const val TIME_REQUEST_CODE = 4000
        const val SUPPORT_AD_PICK = 4010

        @JvmStatic
        fun createIntent(context: Context?): Intent? {
            return Intent(context, SupportWriteActivity::class.java)
        }

    }

    override fun onResume() {
        super.onResume()
        val account = IdolAccount.getAccount(this)
        if (account != null) {
            accountManager.fetchUserInfo(this, {
                myTotalDia = account.userModel?.diamond ?: 0
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_support_write)
        binding.supportWriteScroll.applySystemBarInsets()

        supportActionBar?.setTitle(R.string.support_create_title)

        adList = arrayListOf()

        mGlideRequestManager = Glide.with(this)
        calendar = Calendar.getInstance()

        idolList = ArrayList<IdolModel>()
        getAllIdols()
        //검색.
        adapter = SupportSearchAdapter(this,this, binding.supportWriteScroll)
        mListView = findViewById(android.R.id.list)
        mListView.adapter = adapter

        if(BuildConfig.CELEB) {
            binding.tvIdol.text = getString(R.string.actor)
        }

        binding.searchLi.setOnClickListener {
            showSearchText()
        }

        binding.searchBtn.setOnClickListener {
            if(searchFalg)
                showSearchText()
            else
                showNameGroupText()
        }

        binding.searchInput.setOnClickListener {
            if(searchFalg)
                showSearchText()
            else
                showNameGroupText()
        }

        binding.supportWriteParentLi.setOnClickListener{
            showNameGroupText()
        }

        //처음 들어갈때 키보드 숨기기.
        Util.hideSoftKeyboard(this, binding.supportWriteTitleTv)
        binding.searchInput.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {


            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.clear()
                val text = binding.searchInput.text.toString().trim().lowercase()
                for(i in 0 until idolList.size){
                    if(idolList[i].getName().lowercase(Locale.getDefault()).contains(text) ||
                            idolList[i].nameEn.lowercase(Locale.getDefault()).contains(text) ||
                            idolList[i].nameJp.lowercase(Locale.getDefault()).contains(text) ||
                            idolList[i].nameZh.lowercase(Locale.getDefault()).contains(text) ||
                            idolList[i].nameZhTw.lowercase(Locale.getDefault()).contains(text) ||
                            idolList[i].description.lowercase(Locale.getDefault()).contains(text)){
                        adapter.add(idolList[i])
                    }
                    adapter.notifyDataSetChanged()
                }

                if(binding.searchInput.text.isEmpty()){
                    adapter.clear()
                    adapter.notifyDataSetChanged()
                }
            }
        })

        //아이돌(나의 최애) 텍스트 넣기.
        try
        {
            val idolAccount = IdolAccount.getAccount(this)
            idolModel = idolAccount?.most
            if(!idolModel?.category.equals("B")) {   //최애가 비밀의 방이 아니면
                UtilK.setName(this, idolModel, binding.name, binding.group)
            }
        }catch (e:IllegalStateException){
            e.printStackTrace()
        }catch (e:NullPointerException){
            e.printStackTrace()
        }

        //앱내 광고 리스트 설정.
        setAdTypeList()

        //광고 선택.
        binding.supportWriteAdChoiceLi.setOnClickListener{
            startActivityForResult(SupportAdPickActivity.createIntent(this,true),SUPPORT_AD_PICK)
        }

        //버튼 활성화 조건..
        binding.supportWriteTitleTv.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                createButtonCondition()
            }
        })

        // 글자수 제한
        val filterArray = arrayOfNulls<InputFilter>(1)
        filterArray[0] = InputFilter.LengthFilter(Const.MAX_SUPPORT_LENGTH)
        binding.supportWriteTitleTv.filters = filterArray

        binding.supportWriteStartday.setOnClickListener(this)
        binding.supportWritePhotoUploadLi.setOnClickListener(this)
        binding.supportWriteCreateBtn.setOnClickListener(this)
        binding.supportWriteCheckBox.setOnClickListener(this)
        binding.supportPhotoUploadBtn.setOnClickListener(this)

        binding.supportWriteScroll.post{
            //스크롤뷰  항상  탑으로 올라가게 -> 기기마다  스크롤이  올라가져 있는 경우가 있어보임.
            binding.supportWriteScroll.fullScroll(ScrollView.FOCUS_UP)
        }

    }

    private fun getAllIdols(){
        lifecycleScope.launch(Dispatchers.IO) {
            val dbAllIdols = getAllIdolsUseCase()
                .mapListDataResource { it.toPresentation() }
                .awaitOrThrow() ?: listOf()

            idolList.addAll(dbAllIdols)
            idolList.sortBy { data -> data.getName(this@SupportWriteActivity) }
        }
    }

    //서버에서 불러온 광고일수 split 함수.
    private fun getAdDate(adDate: String) {
        when {
            adDate.contains("D") -> {
                day = selectionAd.period.substring(0, selectionAd.period.indexOf("D"))
                Util.log("SupportWriteActivity::day -> $day")
                if(setDate != null){
                    calendar.time = setDate
                    calendar.add(Calendar.DATE, day!!.toInt())
                    expiredDate = calendar.time

                    if(day!!.toInt()>1){//day 1 초과 복수일때
                        binding.supportWriteAdPeriodTv.text = String.format(getString(R.string.date_format_days), day!!.toInt())
                    }else{//1 이하일때
                        binding.supportWriteAdPeriodTv.text = getString(R.string.date_format_day)
                    }
                }
            }
            adDate.contains("W") -> {
                week = selectionAd.period.substring(0, selectionAd.period.indexOf("W"))
                Util.log("SupportWriteActivity::week -> $week")
                if(setDate != null){
                    calendar.time = setDate
                    calendar.add(Calendar.WEEK_OF_MONTH, week!!.toInt())
                    expiredDate = calendar.time

                    if(week!!.toInt()>1){//week 1 초 복수일
                        binding.supportWriteAdPeriodTv.text = String.format(getString(R.string.date_format_weeks), week!!.toInt())
                    }else{//1 이하일때
                        binding.supportWriteAdPeriodTv.text = getString(R.string.date_format_week)
                    }
                }
            }
            adDate.contains("M") -> {
                month = selectionAd.period.substring(0, selectionAd.period.indexOf("M"))
                Util.log("SupportWriteActivity::week -> $month")
                if(setDate != null){
                    calendar.time = setDate
                    calendar.add(Calendar.MONTH, month!!.toInt())
                    expiredDate = calendar.time
                    val m = month?.toInt() ?: 1
                    val period = if(m == 1) getString(R.string.date_format_month)
                        else String.format(getString(R.string.date_format_months), m)
                    binding.supportWriteAdPeriodTv.text = "${period}"
                }
            }
        }
    }
    //버튼 활성화 조건.
    private fun createButtonCondition(){
        if(binding.supportWriteTitleTv.text!!.isNotEmpty() && binImage !=null && binding.supportWriteSupportPeriodTv.text.isNotEmpty() && binding.supportWriteCheckBox.isChecked
                && binding.supportWriteAdPeriodTv.text.isNotEmpty() && binding.name.text.isNotEmpty() && binding.supportWriteTitleTv.text.toString().trim().isNotEmpty()){
            binding.supportWriteCreateBtn.isEnabled = true
            binding.supportWriteCreateBtn.background = resources.getDrawable(R.drawable.bg_radius_brand500)
        }else{
            binding.supportWriteCreateBtn.isEnabled = false
            binding.supportWriteCreateBtn.background = resources.getDrawable(R.drawable.bg_radius_gray300)
        }
    }


    private fun setAdTypeList() {
        try{
            val gson = IdolGson.getInstance()
            val listType = object : TypeToken<List<SupportAdTypeListModel>>() {}.type
            adList = gson.fromJson(Util.getPreference(this, Const.AD_TYPE_LIST), listType)
        }catch (e: IllegalStateException){
            e.printStackTrace()
        }
    }

    override fun onItemClicked(item: IdolModel, view: View, position: Int) {
        when(view.id){
            R.id.support_search_li -> {
                binding.searchInput.text = Editable.Factory.getInstance().newEditable("")
                adapter.clear()
                idolModel = item
                UtilK.setName(this, idolModel, binding.name, binding.group)
                showNameGroupText()
                createButtonCondition()
            }
        }
    }

    override fun onItemClick(adList: SupportAdTypeListModel) {
        selectionAd = adList
        binding.supportWriteAdChoiceTv.text = selectionAd.period

        binding.supportWriteDateStartLi.visibility = View.VISIBLE
        //만약 광고 게시가 하루면 광고게시기간 안보이게 하기. 또 앞의 숫자를 짤라서 저장하기.
        getAdDate(selectionAd.period)

        if(mBottomSheetFragment != null){
            mBottomSheetFragment.dismiss()
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            binding.supportWriteStartday.id -> {
               val dateDlg = SupportDateTimePickerDialogFragment(selectionAd.period,setDate, limitedDate)
                dateDlg.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
                val args = Bundle()
                dateDlg.arguments = args
                dateDlg.setActivityRequestCode(TIME_REQUEST_CODE)
                dateDlg.show(supportFragmentManager, "date")
                createButtonCondition()
            }
            binding.supportWritePhotoUploadLi.id, binding.supportPhotoUploadBtn.id -> {
                if(flag)
                {
                    onArticlePhotoClick(null)
                }else{
                    flag = true
                    binImage = null
                    binding.supportPhotoUploadBtn.visibility = View.VISIBLE
                    binding.supportWritePhotoTv.text = getString(R.string.quiz_write_image)
                    binding.supportPhoto.visibility = View.GONE
                }
                createButtonCondition()
            }
            binding.supportWriteCheckBox.id -> {
                if (binding.supportWriteCheckBox.isChecked){
                    binding.supportWriteCheckBox.setTextColor(ContextCompat.getColor(this,R.color.main))
                    binding.supportWriteCheckBox.buttonDrawable = ContextCompat.getDrawable(this, if(BuildConfig.CELEB) R.drawable.checkbox else R.drawable.checkbox_on)
                }else{
                    binding.supportWriteCheckBox.setTextColor(ContextCompat.getColor(this,R.color.gray200))
                    binding.supportWriteCheckBox.buttonDrawable = ContextCompat.getDrawable(this, if(BuildConfig.CELEB) R.drawable.checkbox else R.drawable.checkbox_off)
                }
                createButtonCondition()
            }
            binding.supportWriteCreateBtn.id -> {
                if(myTotalDia!! < selectionAd.require){
                    Util.showChargeDiamondWithBtn1(this@SupportWriteActivity, null,null, {
                        startActivity(NewHeartPlusActivity.createIntent(this@SupportWriteActivity, NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP))
                        Util.closeIdolDialog()
                    },{
                        startActivity(NewHeartPlusActivity.createIntent(this@SupportWriteActivity, NewHeartPlusActivity.FRAGMENT_DIAMOND_SHOP))
                        Util.closeIdolDialog()
                    },{
                        Util.closeIdolDialog()
                    })
                }else{
                    Util.showDefaultIdolDialogWithRedBtn2(this@SupportWriteActivity,
                            getString(R.string.support_create_button),
                            getString(R.string.support_create_confirm),
                            selectionAd.require.toString(),
                            R.string.confirm,
                            R.string.btn_cancel,
                            true,
                        true, false, false,
                        {
                        Util.showProgress(this)
                        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        val utcDate = formatter.format(setDate)
                        Util.log("setDate:: $utcDate")
                        createSupport(idolModel, binding.supportWriteTitleTv.text.toString(), selectionAd.id, utcDate, binImage)
                        Util.closeIdolDialog()
                    },{
                        Util.closeIdolDialog()
                    })
                }

            }
        }

    }

    private fun createSupport(idol: IdolModel?, title: String, adId: Int, utcDate: String , image: ByteArray?) {
        val idol = idol ?: return
        MainScope().launch {
            supportRepository.createSupport(idol.getId(),
                title,
                adId.toString(),
                utcDate,
                image,
                { response ->
                    if (response.optBoolean("success")) {
                        Util.closeProgress(3000)

                        Util.log("SupportWriteActivity:: createSupport success")
                        //개설후 유저정보(다이아정보) 볼러오기...
                        accountManager.fetchUserInfo(this@SupportWriteActivity)

                        Util.showDefaultIdolDialogWithBtn1(this@SupportWriteActivity,
                            null,
                            getString(R.string.support_create_success)){
                            Util.closeIdolDialog()
                            val data = Intent()
                            data.putExtra("write_result", true)
                            setResult(ResultCode.SUPPORT_WRITE.value, data)
                            finish()
                        }
                    } else {
                        Util.showDefaultIdolDialogWithBtn1(this@SupportWriteActivity,
                            null,
                            response.optString("msg")) {
                            finish()
                        }
                    }
                },
                { throwable ->
                    Util.showDefaultIdolDialogWithBtn1(this@SupportWriteActivity,
                        null,
                        throwable.message) {
                        finish()
                    }
                })
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == 1) {
            when (requestCode) {
                TIME_REQUEST_CODE -> {
                    binding.supportWriteSupportPeriodLi.visibility = View.VISIBLE
                    binding.supportWriteAdPeriodLi.visibility = View.VISIBLE
                    setDate = data!!.getSerializableExtra("date") as Date
                    limitedDate = data.getSerializableExtra("limitedDate") as Date
                    binding.supportWriteStartdayTv.text = getDateTimeString(setDate!!)
                    binding.supportWriteSupportPeriodTv.text = "${getDateTimeString(Date())} ~ ${getDateTimeString(limitedDate!!)}"
                    binding.supportWriteStartdayLabel.visibility = View.GONE
                    getAdDate(selectionAd.period)
                    createButtonCondition()
                }
            }
        }
    }

    // 언어별로 시간 포맷 해주기.
    private fun getDateTimeString(date: Date): String? {
        val f: DateFormat = DateFormat.getDateInstance(DateFormat.LONG, LocaleUtil.getAppLocale(this))
        val localPattern = (f as SimpleDateFormat).toLocalizedPattern()
        val transFormat = SimpleDateFormat(localPattern, LocaleUtil.getAppLocale(this))
        transFormat.timeZone = Const.TIME_ZONE_KST // 항상 KST로
        return transFormat.format(date)
    }




    //이밑에는 WriteArticleActivity에서 가져옴.
    private fun onArticlePhotoClick(uri: Uri?) {
        if (uri != null) chooseInternalEditor(uri) else {
//            Intent photoPickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            photoPickIntent.setType("image/*");
            val photoPickIntent = MediaStoreUtils.getPickImageIntent(this)
            val packageManager = packageManager
            if (photoPickIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(photoPickIntent, PHOTO_SELECT_REQUEST)
            } else {
                Util.showDefaultIdolDialogWithBtn1(this,
                        null,
                        getString(R.string.cropper_not_found)
                ) { view: View? -> Util.closeIdolDialog() }
            }
        }
    }


    private fun chooseInternalEditor(uri: Uri) {
        ImageUtil.chooseInternalEditor(this, uri, useSquareImage) {
            mTempFileForCrop = it
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == SUPPORT_AD_PICK && resultCode == Activity.RESULT_OK){
            resultId = data?.getIntExtra("support_ad_pick",0)

            for(i in adList.indices){
                if(resultId == adList[i].id){
                    selectionAd = adList[i]
                }
            }

            //초기 설정값 필요 없음 또 광고 선택하면 게시기간 안보이게 하기(아이폰이랑 맞춤)..
            binding.supportWriteStartdayLabel.visibility = View.VISIBLE
            binding.supportWriteStartdayTv.text = ""
            binding.supportWriteSupportPeriodTv.text = ""
            binding.supportWriteAdPeriodTv.text = ""
            setDate = null
            binding.supportWriteAdChoiceTv.text = selectionAd.name

            binding.supportWriteDateStartLi.visibility = View.VISIBLE
            binding.supportWriteSupportPeriodLi.visibility = View.GONE
            binding.supportWriteAdPeriodLi.visibility = View.GONE
        }

        if (requestCode == PHOTO_SELECT_REQUEST
                && resultCode == Activity.RESULT_OK) {
            chooseInternalEditor(data!!.data!!)
        } else if (requestCode == PHOTO_CROP_REQUEST && resultCode == Activity.RESULT_OK) {
            if (mTempFileForCrop != null) {
                onArticlePhotoSelected(Uri.fromFile(mTempFileForCrop))
                mTempFileForCrop?.deleteOnExit()
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                onArticlePhotoSelected(resultUri)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
        createButtonCondition()
    }


    private fun onArticlePhotoSelected(uri: Uri) {
        flag = false
        binding.supportPhotoUploadBtn.visibility = View.GONE
        binding.supportWritePhotoTv.text = getString(R.string.quiz_write_delete_image)
        if (binding.supportPhoto.visibility == View.GONE) {
            binding.supportPhoto.visibility = View.VISIBLE
        }

        mGlideRequestManager
                ?.load(uri)
                ?.into(binding.supportPhoto)

        ImageUtil.onArticlePhotoSelected(this, uri,
            photoSetCallback = { scaledBitmap ->
                // setImageUri가 안되는 폰이 있음.
                binding.supportPhoto.setImageBitmap(scaledBitmap)
            },
            byteArrayCallback = { stream ->
                if (Const.USE_MULTIPART_FORM_DATA) {
                    binImage = stream.toByteArray()
                }
            })
    }

    private fun showSearchText(){
        binding.supportWriteLi.visibility = View.GONE
        binding.searchInput.visibility = View.VISIBLE
        binding.searchInput.requestFocus()
        binding.supportWriteCon.visibility = View.VISIBLE
        binding.searchLi.visibility = View.GONE
        Util.showSoftKeyboard(this,binding.searchInput)
        searchFalg = false
    }

    private fun showNameGroupText(){
        binding.supportWriteLi.visibility = View.VISIBLE
        binding.searchInput.visibility = View.GONE
        binding.searchInput.clearFocus()
        binding.supportWriteCon.visibility = View.GONE
        binding.searchLi.visibility = View.VISIBLE
        Util.hideSoftKeyboard(this,binding.searchInput)
        searchFalg = true
    }

}