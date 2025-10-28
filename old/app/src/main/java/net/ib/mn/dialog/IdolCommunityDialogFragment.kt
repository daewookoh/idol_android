package net.ib.mn.dialog

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.GalleryActivity
import net.ib.mn.activity.HallOfFameAggHistoryActivity
import net.ib.mn.activity.HallOfFameAggHistoryLeagueActivity
import net.ib.mn.activity.HeartVoteRankingActivity
import net.ib.mn.activity.NewFriendsActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.chatting.chatDb.ChatRoomList
import net.ib.mn.core.data.repository.FavoritesRepository
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.DialogCommunityBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.model.IdolModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.setIdolBadgeIcon
import net.ib.mn.utils.trimNewlineWhiteSpace
import net.ib.mn.viewmodel.CommunityActivityViewModel
import org.json.JSONException
import java.text.DateFormat
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class IdolCommunityDialogFragment : BaseDialogFragment() {
    private lateinit var binding: DialogCommunityBinding

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
    @Inject
    lateinit var sharedAppState: SharedAppState
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var favoritesRepository: FavoritesRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private val communityActivityViewModel: CommunityActivityViewModel by activityViewModels()
    private lateinit var mGlideRequestManager: RequestManager

    private var idolSoloFinalId: Int? = null
    private var idolGroupFinalId: Int? = null

    private lateinit var mContext: Context
    override fun onStart() {
        super.onStart()

        dialog ?: return

        dialog?.window?.apply {
            val lpWindow = attributes.apply {
                flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                dimAmount = 0.7f
                gravity = Gravity.CENTER
            }

            attributes = lpWindow
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog?.apply {
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_community, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mGlideRequestManager = Glide.with(this)

        binding.btnFavorite.isChecked = communityActivityViewModel.idolModel.value!!.isFavorite
            binding.btnMost.isChecked = communityActivityViewModel.idolModel.value!!.isMost

            if (communityActivityViewModel.idolModel.value!!.type == "B") {
                binding.rlMostFavorite.visibility = View.INVISIBLE
            }
            clickListener()

    }

    private fun clickListener() {
        binding.btnMost.setOnClickListener {
            val newMost = if (!binding.btnMost.isChecked) null else communityActivityViewModel.idolModel.value
            UtilK.showChangeMostDialog(
                mContext,
                newMost,
                sharedAppState,
                {
                    idolSoloFinalId = it.first
                    idolGroupFinalId = it.second
                    updateMost(newMost, binding.btnMost, binding.btnFavorite, binding.titleBurningday)
                },
            ) {
                binding.btnMost.isChecked = communityActivityViewModel.idolModel.value!!.isMost
            }
        }
        binding.btnFavorite.setOnClickListener {
            if (!binding.btnFavorite.isEnabled) {
                return@setOnClickListener
            }
            binding.btnFavorite.isEnabled = false
            communityActivityViewModel.idolModel.value?.isFavorite = binding.btnFavorite.isChecked
            if (binding.btnFavorite.isChecked) {
                addFavoriteApi()
            } else {
                removeFavorite()
            }
        }
        binding.titleVote.setOnClickListener {
            if (dialog!!.isShowing) {
                try {
                    dialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            setUiActionFirebaseGoogleAnalyticsDialogFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "community_profile_top30"
            )
            startActivity(HeartVoteRankingActivity.createIntent(mContext, communityActivityViewModel.idolModel.value))
        }
        binding.titleGallery.setOnClickListener {
            if (dialog!!.isShowing) {
                try {
                    dialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            setUiActionFirebaseGoogleAnalyticsDialogFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "community_bannergram"
            )
            startActivity(GalleryActivity.createIntent(mContext, communityActivityViewModel.idolModel.value))
        }
        binding.llCommunityShare.setOnClickListener {
            if (dialog!!.isShowing) {
                try {
                    dialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            setUiActionFirebaseGoogleAnalyticsDialogFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "community_share"
            )
            val params = listOf(LinkStatus.COMMUNITY.status)
            val querys = listOf(
                "idol" to communityActivityViewModel.idolModel.value?.getId().toString(),
                "group" to communityActivityViewModel.idolModel.value?.groupId.toString()
            )
            val msg = if (BuildConfig.CELEB) {
                String.format(
                    LocaleUtil.getAppLocale(mContext),
                    getString(R.string.celeb_community_main_share_msg),
                    communityActivityViewModel.idolModel.value?.getName(context)
                )
            } else {
                String.format(
                    LocaleUtil.getAppLocale(mContext),
                    getString(R.string.community_main_share_msg),
                    communityActivityViewModel.idolModel.value?.getName(context)
                )
            }

            val url = LinkUtil.getAppLinkUrl(context = mContext, params = params, querys = querys)
            UtilK.linkStart(context = mContext, url = url, msg = msg.trimNewlineWhiteSpace())
        }
        binding.titleBurningday.setOnClickListener {
            if (dialog!!.isShowing) {
                try {
                    dialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val showBurningDayPurchaseDialog = BurningDayPurchaseDialogFragment.getInstance()


            showBurningDayPurchaseDialog.setActivityRequestCode(RequestCode.CHAT_ROOM_CREATE.value)
            activity?.supportFragmentManager?.let { it1 -> showBurningDayPurchaseDialog.show(it1, "show_burning_day_purchase") }
        }
        binding.titleRank.setOnClickListener {
            if (dialog!!.isShowing) {
                try {
                    dialog!!.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            setUiActionFirebaseGoogleAnalyticsDialogFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "community_chart"
            )
            if (BuildConfig.CELEB) {
                startActivity(HallOfFameAggHistoryActivity.createIntent(mContext, communityActivityViewModel.idolModel.value))
            } else {
                startActivity(HallOfFameAggHistoryLeagueActivity.createIntent(mContext, communityActivityViewModel.idolModel.value))
            }
        }
        UtilK.setName(mContext, communityActivityViewModel.idolModel.value, binding.favorite, binding.group)

        val tmp = binding.photo.loadInfo as String?
        if (tmp == null || tmp != communityActivityViewModel.idolModel.value?.imageUrl) {
            if (communityActivityViewModel.getDialogProfileThumb() == null) { // /null 일 경우에  일반 이미지 url 적용
                communityActivityViewModel.setDialogProfileThumb(communityActivityViewModel.idolModel.value?.imageUrl)
            }

            val idolId = communityActivityViewModel.idolModel.value?.getId()?: 0
            mGlideRequestManager
                .load(communityActivityViewModel.getDialogProfileThumb())
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .into(binding.photo)
        }
        if (!communityActivityViewModel.idolModel.value?.birthDay.isNullOrEmpty()) {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", LocaleUtil.getAppLocale(mContext))
            val birthDay = simpleDateFormat.parse(communityActivityViewModel.idolModel.value?.birthDay)
            binding.birthday.text =
                DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(mContext)).format(birthDay)
            if (communityActivityViewModel.idolModel.value?.isLunarBirthday == "Y") {
                binding.birthday.text = binding.birthday.text.toString() + " " + getString(R.string.lunar_birthday)
            }
        } else {
            binding.birthday.text = ""
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val idolModel = getIdolByIdUseCase(communityActivityViewModel.idolModel.value?.getId()?:0)
                .mapDataResource { it?.toPresentation() }
                .awaitOrThrow()

            withContext(Dispatchers.Main) {
                idolModel?.let {
                    binding.tvMostCount.text = Util.mostCountLocale(mContext, it.mostCount)
                }
                
                if (communityActivityViewModel.idolModel.value?.type != "B") {
                    binding.tvMostCount.text =
                        (if (BuildConfig.CELEB) getString(R.string.actor_most_favorite) else getString(
                            R.string.most_favorite
                        )).plus(" : ").plus(binding.tvMostCount.text)
                }

                setIdolBadgeIcon(binding.imageAngel, binding.imageFairy, binding.imageMiracle, binding.imageRookie, binding.imageSuperRookie, communityActivityViewModel.idolModel.value ?: return@withContext)

                if (!communityActivityViewModel.idolModel.value?.isMost!! ||
                    communityActivityViewModel.idolModel.value?.type.equals("B", ignoreCase = true)
                ) {
                    binding.titleBurningday.visibility = View.GONE
                }
                try {
                    dialog!!.show()
                } catch (e: Exception) {
                }
            }
        }
    }


    private fun updateMost(
        item: IdolModel?,
        mostBtn: AppCompatCheckBox,
        favoriteBtn: AppCompatCheckBox,
        burningday: View
    ) {

        // 네트워크 문제 등으로 최애설정 실패 대비. 그냥 account.getMost()로 가져오면 setMost할 때 가져온 것까지 바뀌어 버리므로 새로 하나 clone
        val mostJson = IdolGson.getInstance().toJson(communityActivityViewModel.idolAccount?.most)
        val oldMost = IdolGson.getInstance().fromJson(mostJson, IdolModel::class.java)

        var actorId = 0
        var preGroupId = 0

        if (BuildConfig.CELEB) {
            actorId = communityActivityViewModel.idolAccount?.most?.getId() ?: 0
        } else {
            preGroupId = communityActivityViewModel.idolAccount?.most?.groupId ?: 0
        }

        communityActivityViewModel.idolAccount?.userModel?.most = item // 최애 변경하고 빠르게 화면을 빠져나가서 쵀애설정화면으로 가면 최애가 반영안되어 있는 현상 방지
        communityActivityViewModel.idolAccount?.saveAccount(mContext) // 이걸 해야 getAccount 할 때 저장한게 반영됨
        // 즐찾설정에서 온 경우에 최애 갱신하게
        requireActivity().setResult(RESULT_NEED_UPDATE)
        // 즐찾에서 커뮤로 들어가서 최애 변경하는 경우 대비
        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)

        lifecycleScope.launch {
            usersRepository.updateMost(
                userResourceUri = IdolAccount.getAccount(mContext)?.userResourceUri!!,
                idolResourceUri = item?.resourceUri,
                listener = { response ->
                    if (response.optBoolean("success") && communityActivityViewModel.idolAccount?.hasUserInfo() == true) {
                        // 최애가 바뀌었으므로, 최애 바뀜 여부를 체크 한다. -> FavoriteFragment에서 users/self 적게 부르기 위해서
                        CommunityActivity.isFavoriteChanged = true
                        Util.setPreference(
                            mContext,
                            Const.PREF_SHOW_SET_NEW_FRIENDS,
                            true,
                        )

                        if (item != null) {
                            communityActivityViewModel.idolAccount?.userModel?.most = item
                            burningday.visibility = View.VISIBLE
                            communityActivityViewModel.idolModel.value?.isMost = true
                            communityActivityViewModel.idolModel.value?.isFavorite = true
                            favoriteBtn.isChecked = true
                        } else {
                            communityActivityViewModel.idolAccount?.userModel?.most = null
                            burningday.visibility = View.GONE
                            communityActivityViewModel.idolModel.value?.isMost = false
                        }
                        communityActivityViewModel.changeMost()
                        communityActivityViewModel.idolAccount?.saveAccount(mContext) // 이걸 해야 getAccount 할 때 저장한게 반영됨

                        if (Util.getPreferenceBool(
                                mContext,
                                Const.PREF_SHOW_SET_NEW_FRIENDS,
                                true,
                            )
                        ) {
                            // 애돌은 최애를 바꿔도 그룹이 같으면 뉴 프렌즈 팝업 안 띄움, 셀럽은 바뀌면 띄움
                            var isChanged = false
                            item?.let {
                                isChanged = if (BuildConfig.CELEB) {
                                    actorId != item.getId()
                                } else {
                                    preGroupId != item.groupId
                                }
                            } ?: false

                            if (item != null && isChanged) {
                                Util.setPreference(
                                    mContext,
                                    Const.PREF_SHOW_SET_NEW_FRIENDS,
                                    false,
                                )
                                Util.showDefaultIdolDialogWithBtn2(
                                    mContext,
                                    getString(R.string.new_friends),
                                    getString(R.string.apply_new_friends_desp),
                                    R.string.yes,
                                    R.string.no,
                                    true,
                                    false,
                                    {
                                        Util.closeIdolDialog()
                                        startActivity(NewFriendsActivity.createIntent(mContext))
                                    },
                                ) { Util.closeIdolDialog() }
                            }
                        }

                        val notificationManager: NotificationManager =
                            activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(Const.NOTIFICATION_GROUP_ID_CHAT_MSG)

                        Handler().postDelayed({
                            accountManager.fetchUserInfo(mContext)
                        }, 1000)

                        idolSoloFinalId?.let {
                            ChatRoomList.getInstance(mContext)
                                .deleteRoomWithIdolId(it) {
                                    idolSoloFinalId = null
                                }
                        }
                        idolGroupFinalId?.let {
                            ChatRoomList.getInstance(mContext)
                                .deleteRoomWithIdolId(it) {
                                    idolGroupFinalId = null
                                }
                        }

                        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)
                        communityActivityViewModel.loadFavorites(requireActivity())
                    } else { // 최애 설정 실패시 원래 최애 복구
                        communityActivityViewModel.idolAccount?.userModel?.most = oldMost
                        communityActivityViewModel.idolAccount?.saveAccount(mContext) // 이걸 해야 getAccount 할 때 저장한게 반영됨
                        mostBtn.isChecked = !mostBtn.isChecked
                        UtilK.handleCommonError(mContext, response)
                    }
                },
                errorListener = { throwable ->
                    // 최애 설정 실패시 원래 최애 복구
                    communityActivityViewModel.idolAccount?.userModel?.most = oldMost
                    communityActivityViewModel.idolAccount?.saveAccount(mContext) // 이걸 해야 getAccount 할 때 저장한게 반영됨
                    Toast.makeText(
                        mContext,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                })
        }
    }

    private fun addFavoriteApi() {
        lifecycleScope.launch {
            favoritesRepository.addFavorite(
                communityActivityViewModel.idolModel.value?.getId() ?: return@launch,
                { response ->
                    if (response.optBoolean("success", true)) {
                        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)
                        try {

                            communityActivityViewModel.setMId(response.getInt("id"))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        communityActivityViewModel.idolModel.value?.isFavorite = false
                        binding.btnFavorite.isChecked = false
                        val responseMsg =
                            ErrorControl.parseError(mContext, response)
                        if (responseMsg != null) {
                            Toast.makeText(
                                mContext,
                                responseMsg,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    binding.btnFavorite.isEnabled = true
                }, {
                    communityActivityViewModel.idolModel.value?.isFavorite = false
                    binding.btnFavorite.isChecked = false
                    binding.btnFavorite.isEnabled = true
                    Toast.makeText(
                        mContext,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun removeFavorite() {
        lifecycleScope.launch {
            favoritesRepository.removeFavorite(
                communityActivityViewModel.getMId(),
                { response ->
                    if (response.optBoolean("success")) {
                        ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)
                    } else {
                        communityActivityViewModel.idolModel.value?.isFavorite = true
                        binding.btnFavorite.isChecked = true
                        UtilK.handleCommonError(mContext, response)
                    }
                    binding.btnFavorite.isEnabled = true
                }, {
                    communityActivityViewModel.idolModel.value?.isFavorite = true
                    binding.btnFavorite.isChecked = true
                    binding.btnFavorite.isEnabled = true
                    Toast.makeText(
                        mContext,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    companion object {
        const val RESULT_NEED_UPDATE = 1
        fun getInstance(): IdolCommunityDialogFragment {
            val fragment = IdolCommunityDialogFragment()
            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)

            return fragment
        }
    }
}