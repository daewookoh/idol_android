package net.ib.mn.chatting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.chatting.ChattingRoomActivity.Companion.PARAM_CHAT_ROOM_USER_ROLE
import net.ib.mn.chatting.chatDb.ChatDB
import net.ib.mn.chatting.chatDb.ChatRoomInfoList
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.chatting.model.ChatRoomInfoModel
import net.ib.mn.core.data.repository.ChatRepositoryImpl
import net.ib.mn.databinding.FragmentChattingInfoBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

@AndroidEntryPoint
class ChattingInfoFragment : BaseFragment(), View.OnClickListener {

    private var mChatRoomInfo:ChatRoomInfoModel? = null
    private var mAccount: IdolAccount? = null
    private var members = CopyOnWriteArrayList<ChatMembersModel>()
    private var membersCount = 0
    private var role = "N"
    private lateinit var glideRequestManager: RequestManager
    private lateinit var binding: FragmentChattingInfoBinding
    @Inject
    lateinit var chatRepository: ChatRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chatting_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mChatRoomInfo = arguments?.getSerializable(PARAM_CHAT_INFO) as ChatRoomInfoModel?
        try {
            //현재 argument로 넘겨줄때 ArrayList로 오고있음 , ArrayList랑 Copy는 List를 구현하고있음 그래서 ArrayList에서 바로 Copy로 캐스팅이 안됨.
            val memberList = arguments?.getSerializable(PARAM_CHAT_MEMBERS) as List<ChatMembersModel>
            members = memberList as CopyOnWriteArrayList<ChatMembersModel>
            membersCount = members.count { !it.deleted }    //삭제된 멤버도 delete = true로 바꾸고 가지고 있어서, 현재 멤버를 셀 때는 delete = false인 멤버의 개수를 세야한다.
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
        activity?.let {
            mAccount = IdolAccount.getAccount(it)
        }
        glideRequestManager = Glide.with(this)
        role = arguments?.getString(PARAM_CHAT_ROOM_USER_ROLE,"N").toString()

        //Ui set해주기.
        setRoomInfo()

        binding.liChatOut.setOnClickListener(this)
        binding.ivChatOut.setOnClickListener(this)
        binding.tvChatOut.setOnClickListener(this)
    }

    private fun setRoomInfo() {
        //인원표시.
        binding.tvCurPeople.text = membersCount.toString()

        //채팅방 상태.
        binding.tvRoomCategory.text = if(mChatRoomInfo?.isMostOnly == "Y"){
            "• ${getString(R.string.lable_show_private)}"
        }else{
            "• ${getString(R.string.chat_room_most_and_group)}"
        }

        binding.tvNicknameCategory.text = if(mChatRoomInfo?.isAnonymity == "Y"){
            "• ${getString(R.string.chat_room_anonymous)}"
        }else{
            "• ${getString(R.string.chat_room_nickname)}"
        }

        binding.tvLevelCategory.text = "• ${String.format(getString(R.string.chat_room_level_limit), mChatRoomInfo?.levelLimit)}"

        //방장정보 보여줌 여부(Y:기본방 N:나머지방).
        if(mChatRoomInfo?.isDefault == "Y"){
            binding.dividerCreator.visibility = View.GONE
            binding.tvCreator.visibility = View.GONE
            binding.conCreator.visibility = View.GONE
        }else{
            binding.dividerCreator.visibility = View.VISIBLE
            binding.tvCreator.visibility = View.VISIBLE
            binding.conCreator.visibility = View.VISIBLE

            //나머지방일때 (Y:익명방, N:실명방).
            if(mChatRoomInfo?.isAnonymity == "Y"){
                binding.ivPhoto.visibility = View.GONE
                binding.ivLevel.visibility = View.GONE
                val params = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.marginStart = 0
                binding.tvUsername.layoutParams = params
            }else{
                binding.ivPhoto.visibility = View.VISIBLE
                binding.ivLevel.visibility = View.VISIBLE
            }
        }

        //방장 정보 업데이트.
        for(i in 0 until members.size){
            if(members[i].role == "O"){
                Logger.v("members-> ${members[i].nickname}")
                glideRequestManager.load(members[i].imageUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .error(Util.noProfileImage(members[i].id))
                        .fallback(Util.noProfileImage(members[i].id))
                        .placeholder(Util.noProfileImage(members[i].id))
                        .into(binding.ivPhoto)

                binding.ivLevel.setImageDrawable(Util.getLevelImageDrawable(activity, members[i]))

                binding.tvUsername.text = members[i].nickname
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_chat_out,
            R.id.tv_chat_out,
            R.id.li_chat_out -> {
                //로컬에있는 사람이 방장인지 아닌지에따라 부제목을 다르게 띄워준다.
                val subTitle =
                        if(role != "O"){
                            getString(R.string.chat_room_leave_desc1)
                        }
                        else{
                            if(membersCount == 1){
                                getString(R.string.chat_room_leave_desc3)
                            }
                            else {
                                getString(R.string.chat_room_leave_desc2)
                            }
                        }

                Util.showDefaultIdolDialogWithBtn2(activity,
                        getString(R.string.chat_room_leave),
                        subTitle, {
                    leaveChatRoom()
                        Util.closeIdolDialog()
                }, {
                    Util.closeIdolDialog()
                })

            }
        }
    }

    private fun leaveChatRoom() {
        // 액티비티/컨텍스트는 호출 시점에 한 번만 얻어 둠
        val act = requireActivity()
        val ctx = requireContext()

        Util.showProgress(act)

        viewLifecycleOwner.lifecycleScope.launch(NonCancellable) {
            chatRepository.leaveChatRoom(
                mChatRoomInfo!!.roomId,
                { response ->
                    Util.closeProgress()

                    if (!response.optBoolean("success")) {
                        UtilK.handleCommonError(ctx, response)
                        return@leaveChatRoom
                    }

                    // DB 삭제는 IO 스레드에서
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = ChatDB
                            .getInstance(ctx, ChatRoomInfoList.getInstance(ctx).accountId!!)!!

                        db.runInTransaction {
                            db.ChatRoomInfoDao().deleteRoomInfo(mChatRoomInfo!!.roomId)
                            db.ChatDao().deleteChatRoomMessages(mChatRoomInfo!!.roomId)
                        }

                        // UI 처리도 안전하게 메인 스레드에서
                        withContext(Dispatchers.Main) {
                            if (members.size > 1 ||
                                (members.size <= 1 && mChatRoomInfo!!.isDefault == "Y")
                            ) {
                                act.setResult(Const.CHATTING_LIST_RESET)
                                act.finish()
                            }
                        }
                    }
                },
                { throwable ->
                    Util.closeProgress()
                    Toast.makeText(ctx, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                    if (Util.is_log()) showMessage(throwable.message)
                }
            )
        }
    }

    companion object {

        const val PARAM_CHAT_INFO = "chat_info"
        const val PARAM_CHAT_MEMBERS = "chat_members"

         @JvmStatic
         fun newInstance(chatRoomInfoModel: ChatRoomInfoModel, members: CopyOnWriteArrayList<ChatMembersModel>, role:String?): ChattingInfoFragment{
            val args = Bundle()
             args.putSerializable(PARAM_CHAT_ROOM_USER_ROLE,role)
             args.putSerializable(PARAM_CHAT_INFO, chatRoomInfoModel)
             args.putSerializable(PARAM_CHAT_MEMBERS, members)
             val fragment = ChattingInfoFragment()
             fragment.arguments = args
             return fragment
         }

    }
}