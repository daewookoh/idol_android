package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.adapter.UserBlockAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.BlocksRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ActivityUserBlockListBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.UserBlockModel
import net.ib.mn.model.UserInfoModel
import net.ib.mn.model.UserModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.*
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class UserBlockListActivity :  BaseActivity(){

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var blocksRepository: BlocksRepository

    private lateinit var mUserBlockAdapter: UserBlockAdapter
    private lateinit var mGlideRequestManager: RequestManager
    private var userBlockList = ArrayList<UserBlockModel>()
    private var userBlockListSaved = ArrayList<UserBlockModel>()    //개발자 모드 -> 활동 유지 안함 키면 다른 액티비티 갔을 때 Activity가 Destroy돼서 feedActivity에서 다시 돌아오면 onCreate되어 리스트가 최신화 되는 현상 막기 위한 변수
    private val gson = IdolGson.getInstance()
    private var clickUserTs : Int = 0   //userInfoList로 가져온 유저 이메일을 통해 일치하는 이메일 찾아서 ts값 넣을 변수
    //차단된 유저 id ArrayList
    private var userBlockLocalIdList = ArrayList<Int>()
    private lateinit var binding: ActivityUserBlockListBinding

    companion object {
        private var clickUserPosition : Int = 0 //차단 리스트 클릭했을 때 해당 포지션 저장할 때 사용
        const val USER_BLOCK_LIST_SAVED = "userBlockListSaved"

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, UserBlockListActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_block_list)
        binding.clContainer.applySystemBarInsets()

        init()
        clickEvent()
        userBlockList()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(USER_BLOCK_LIST_SAVED,userBlockList)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState != null) {
            userBlockListSaved = savedInstanceState.getSerializable(USER_BLOCK_LIST_SAVED) as ArrayList<UserBlockModel>
        }
    }

    fun init(){
        supportActionBar?.title = getString(R.string.user_blocked2)
        mGlideRequestManager = Glide.with(this)
        mUserBlockAdapter = UserBlockAdapter(
            lifecycleScope, mGlideRequestManager, getIdolByIdUseCase)
        binding.rvUserBlock.apply {
            adapter = mUserBlockAdapter
        }

    }

    private fun clickEvent(){
        //차단, 차단해제 버튼 클릭 리스너
        mUserBlockAdapter.setOnItemClickListener(object:UserBlockAdapter.OnItemClickListener{
            override fun onUpdateUserBlockStatus(userId: Int, blockStatus: String, position: Int) {
                userBlockAddCancel(userId,blockStatus,position)
            }

            override fun feedActivityStart(email: String, userId: Int, position: Int) {
                clickUserPosition = position    //피드 들어가기 위해 클릭했을 때 클릭한 포지션 저장
                getUserTs(email)
                blockUserInfo(email, clickUserTs)
            }
        })
    }

    //유저 차단 리스트 불러오는 API
    private fun userBlockList(){
        lifecycleScope.launch {
            blocksRepository.getBlockList(
                "N",
                { response ->
                    if (response.optBoolean("success")) {
                        val listType = object : TypeToken<ArrayList<UserBlockModel>>() {}.type
                        userBlockList = gson.fromJson(response.getJSONArray("blocks").toString(), listType)
                        if(userBlockListSaved.isNotEmpty()){    //활동 유지 안함 켜져있어 액티비티 재생성됐을 경우
                            userBlockList = userBlockListSaved  //유저차단 리스트 기존 값 저장한 값 다시 보여줌
                        }
                        mUserBlockAdapter.setBlockArray(userBlockList)

                        if(userBlockList.size==0){  //차단 리스트가 없을 때
                            binding.emptyFriendRequest.visibility = View.VISIBLE
                        }
                    }
                }, {}
            )
        }
    }

    //차단, 차단 해제 눌렀을 때 불리는 API
    private fun userBlockAddCancel(userId : Int, blocks: String,position:Int){
        lifecycleScope.launch {
            blocksRepository.addBlock(
                userId,
                1,
                blocks,
                { response ->
                    if (response.optBoolean("success")) {
                        val blockList  = userBlockList.find { it.id == userId }
                        if (blockList != null) {
                            blockList.isBlocked = blocks
                        }
                        mUserBlockAdapter.updateUserBlockStatus(position,blockList)
                        val listType = object : TypeToken<ArrayList<Int>>() {}.type
                        userBlockLocalIdList = gson.fromJson(Util.getPreference(this@UserBlockListActivity, Const.USER_BLOCK_LIST).toString(), listType) //로컬에 있는 차단된 유저 리스트
                        if(blocks == "Y")
                            userBlockLocalIdList.add(userId)
                        else{
                            userBlockLocalIdList.remove(userId)
                        }
                        Util.setPreferenceArray(this@UserBlockListActivity, Const.USER_BLOCK_LIST, userBlockLocalIdList)
                    }
                    else{
                        Logger.v("block", "block false")
                        val responseMsg = ErrorControl.parseError(this@UserBlockListActivity, response)
                        if (responseMsg != null) {
                            Util.showDefaultIdolDialogWithBtn1(this@UserBlockListActivity,
                                null,
                                responseMsg,
                                { Util.closeIdolDialog()},true)
                        }
                    }
                }, { throwable ->
                    Util.showDefaultIdolDialogWithBtn1(
                        this@UserBlockListActivity,
                        null,
                        throwable.message,
                        { Util.closeIdolDialog() },
                        true,
                    )
                }
            )
        }
    }

    //클릭한 유저 ts값 찾는 함수
    private fun getUserTs(email : String){
        if(!Util.getPreference(this, Const.BLOCK_USER_INFO).isNullOrEmpty()){
            var userInfoList = ArrayList<UserInfoModel>()   //유저 이메일, ts 가지고 있는 리스트(getPreference로 가져올 예정)
            val listType = object : TypeToken<ArrayList<UserInfoModel>>() {}.type
            userInfoList = gson.fromJson(Util.getPreference(this, Const.BLOCK_USER_INFO).toString(), listType)  //setPreference는 feedActivity에서 해줌
            Logger.v("mingue","ts보내기 전 저장된 유저 목록 : "+userInfoList)
            val clickUserInfo = userInfoList?.find { it.email == email }

            clickUserTs =
                if(clickUserInfo == null){  //저장된 유저 목록 중 email 저장된 게 없으면 ts = 0
                    0
                }
                else {                      //있으면 저장된 ts저장
                    clickUserInfo.ts
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == ResultCode.BLOCKED.value){
            //유저 피드 들어가서 차단 버튼 누르고 뒤로 돌아오면 차단, 차단해제 버튼 최신화 안되는 현상 수정
            val userId = data?.getIntExtra(FeedActivity.PARAM_USER_ID,0)
            val userBlockModel = userBlockList.find { it.id == userId }
            if(userBlockModel!=null) {
                userBlockModel.isBlocked = data?.getStringExtra(FeedActivity.PARAM_USER_BLOCK_STATUS).toString()
                mUserBlockAdapter.notifyItemChanged(clickUserPosition)
            }
        }
    }

    //설정에서 차단 유저 목록을 통해 들어왔을 경우 불리는 API
    fun blockUserInfo(email : String, ts : Int){
        var thisBlockUserList = ArrayList<UserInfoModel>()
        val listType = object : TypeToken<ArrayList<UserInfoModel>>() {}.type
        if(!Util.getPreference(this,Const.BLOCK_USER_INFO).isNullOrEmpty()) {
            thisBlockUserList = gson.fromJson(Util.getPreference(this, Const.BLOCK_USER_INFO).toString(), listType)
        }

        lifecycleScope.launch {
            usersRepository.getUserInfo(
                email,
                ts,
                { response ->
                    if (response.optBoolean("success")) {
                        val userInfoModel = gson.fromJson(response.getJSONArray("objects").get(0).toString(), UserInfoModel::class.java)
                        if(thisBlockUserList.any{ it.id == userInfoModel.id && it.ts != userInfoModel.ts }) {   //유저모델은 들어가 있으니ㅏ, ts값이 다르다면
                            thisBlockUserList.remove(thisBlockUserList.find { it.id == userInfoModel.id })      //기존에 들어있던 해당 id의 userModel 삭제 후
                        }
                        thisBlockUserList.add(userInfoModel)    //해당 id의 userModel 새로운 것으로 add
                        Util.setPreferenceArray(this@UserBlockListActivity, Const.BLOCK_USER_INFO, thisBlockUserList)
                    }
                    //설정에서 피드 들어갔을 때, 유저 정보를 여기서 저장
                    val userInfoModel = thisBlockUserList.find { it.email == email }

                    if(userInfoModel!=null) {
                        val userModel = setUserModel(userInfoModel)
                        moveFeedActivity(userInfoModel.mostId, userModel)
                    }
                }, { t ->
                    if (t.toString().contains("Not Modified")) {
                        val userInfoModel = thisBlockUserList.find { it.email == email }
                        if(userInfoModel!=null) {
                            val userModel = setUserModel(userInfoModel)
                            moveFeedActivity(userInfoModel.mostId, userModel)
                        }
                    }
                }
            )
        }
    }

    private fun setUserModel(userInfoModel: UserInfoModel): UserModel {
        val userModel = UserModel()
        userModel.nickname = userInfoModel.nickname
        userModel.imageUrl = userInfoModel.imageUrl
        userModel.level = userInfoModel.level
        userModel.id = userInfoModel.id
        userModel.itemNo = userInfoModel.itemNo

        return userModel
    }

    @OptIn(UnstableApi::class)
    private fun moveFeedActivity(mostId: Int, userModel: UserModel) {
        //유저의 IdolModel 가저오고 startActiity 실행
        lifecycleScope.launch(Dispatchers.IO) {
            val idol = getIdolByIdUseCase(mostId)
                .mapDataResource { it?.toPresentation() }
                .awaitOrThrow()
            idol?.let {
                withContext(Dispatchers.Main) {
                    userModel.most = it
                    startActivityForResult(FeedActivity.createIntent(this@UserBlockListActivity, userModel), RequestCode.USER_BLOCK_CHANGE.value)  //넘어갈 때 userModel값 보내줌
                }
            }
        }
    }
}