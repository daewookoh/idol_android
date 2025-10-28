package net.ib.mn.utils

import android.net.Uri
import android.os.SystemClock
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.BaseCommentActivity
import net.ib.mn.adapter.EmoticonTabAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.ViewCommentBinding
import net.ib.mn.fragment.EmoticonFragment
import net.ib.mn.model.CommentModel
import net.ib.mn.model.EmoticonDetailModel
import net.ib.mn.model.EmoticonModel
import net.ib.mn.model.EmoticonsetModel
import java.util.ArrayList

var emoModel: EmoticonDetailModel? = null
private var mLastEmoTime = 0L   //같은 이모티콘을 눌렀을 때만 시간 저장하는 변수
private var mFirstEmoTime = 0L  //이모티콘을 클릭했을 때 처음에 시간 저장하는 변수
private var isEmoClicked = true    //같은 것을 클릭했으면

interface OnEmoticonClickListener {
    fun onClickEmoticon(emoticonId: Int)
}

// 글, 사진, 이모티콘 입력 UI에 대한 이벤트 리스터 연결
fun bindInputListener(view: View) {
    val rlEmoticon = view.findViewById<View?>(R.id.rl_emoticon) ?: return
    val viewCommentBinding: ViewCommentBinding = DataBindingUtil.getBinding(view.findViewById(R.id.view_comment))
        ?: return

    // 이미지/이모티콘 프리뷰에 대한 visibility 감시
    val clPreview = view.findViewById<View?>(R.id.cl_preview)
    clPreview?.visibilityChanged {
        updateSubmitButton(view)
    }

    viewCommentBinding.inputComment.doOnTextChanged { _, _, _, _ ->
        updateSubmitButton(view)
    }

    // 이모티콘 선택창 열리면 이모티콘 버튼 이미지 변경
    rlEmoticon.visibilityChanged {
        viewCommentBinding.btnEmoticon.setBackgroundResource(
            if( it.visibility == View.GONE) R.drawable.btn_chat_emoticon_off
            else R.drawable.btn_chat_emoticon_on
        )
    }
}

fun updateSubmitButton(view: View) {
    // 이미지/이모티콘 프리뷰가 없고 텍스트 입력한 것도 없으면 버튼 비활성화
    val viewCommentBinding: ViewCommentBinding = DataBindingUtil.getBinding(view.findViewById(R.id.view_comment))
        ?: return
    val clPreview = view.findViewById<View?>(R.id.cl_preview) ?: return
    viewCommentBinding.btnSubmit.isEnabled =
        !(clPreview.visibility != View.VISIBLE && (viewCommentBinding.inputComment.text?.isEmpty() ?: true))
}

fun getEmoticon(
    activity: BaseActivity,
    rootView: View,
    listener: OnEmoticonClickListener? = null,
) {
    try {
        val gson = IdolGson.getInstance()
        val emoSetListType = object : TypeToken<ArrayList<EmoticonsetModel?>?>() {}.type

        val emoListType = object : TypeToken<ArrayList<EmoticonDetailModel>>() {}.type

        val emoSetList = gson.fromJson<ArrayList<EmoticonsetModel>>(Util.getPreference(activity,
            Const.EMOTICON_SET
        ), emoSetListType)

        val emoFragList = ArrayList<EmoticonFragment>()

        val emoAllInfoList = gson.fromJson<ArrayList<EmoticonDetailModel>>(
            Util.getPreference(
                activity,
                Const.EMOTICON_ALL_INFO
            ), emoListType
        )

        for (i in 0 until emoSetList.size) {

            //서버에서 받아온 이모티콘 set에 캐싱된  set id와  일치하는지  체크하여서
            //이모티콘 프래그먼트를 추가해준다.
            if (emoAllInfoList.any { it.emoticonSetId == emoSetList[i].id }) {
                //setid에 와 각 EmoticonDetailModel 별  set id가  일치하는 애들을  필터링해준다.  각각 set에 맞춘 이모티콘 리스트를 뿌리기 위해써
                emoFragList.add(EmoticonFragment.newInstance(emoAllInfoList.filter {
                    it.emoticonSetId == emoSetList[i].id && !it.isSetCategoryImg
                }.distinctBy { it.id } as java.util.ArrayList<EmoticonDetailModel>)
                { model, view, _ ->
                    onEmoticonClick(
                        activity,
                        rootView,
                        model,
                        view,
                        listener
                    )
                }) //이모티콘 숫자 만큼 추가해줌.
            }
        }

        val vpChatEmoticon = rootView.findViewById<ViewPager2?>(R.id.vp_chat_emoticon) ?: return
        val rvChatEmoticon = rootView.findViewById<RecyclerView?>(R.id.rv_chat_emoticon) ?: return

        vpChatEmoticon.apply {
            activity.runOnUiThread {
                adapter = BaseCommentActivity.PagerAdapter(activity, emoFragList)
                offscreenPageLimit = emoSetList.size
            }
        }

        rvChatEmoticon.apply {
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@runOnUiThread
                }
                adapter = EmoticonTabAdapter(Glide.with(activity), emoSetList, emoAllInfoList) {
                        model, emoAllInfoList, view, position -> onItemClick(rootView, model, emoAllInfoList, view, position) }
                layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            }
        }

        vpChatEmoticon.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //페이지 슬라이딩 될때마다 해당아이템 선택해주기.
                rvChatEmoticon.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
                Logger.v("Scrolled:: selected ${position}")
            }
        })
    } catch (e: Exception) {
        try {
            //아래 exception으로  던짐
            throw  Exception("class : $activity \n exception -> $e");
        } catch (e1: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e1);
        }
        e.printStackTrace()
    }
}

fun onItemClick(
    rootView: View,
    setItems: ArrayList<EmoticonsetModel>,
    emoAllInfoList: ArrayList<EmoticonDetailModel>,
    @Suppress("UNUSED_PARAMETER") view: View?,
    position: Int,
) {
    //해당 아이템 클릭했을시 뷰페이저 스와이프.
    val vpChatEmoticon = rootView.findViewById<ViewPager2?>(R.id.vp_chat_emoticon) ?: return
    val rvChatEmoticon = rootView.findViewById<RecyclerView?>(R.id.rv_chat_emoticon) ?: return


    vpChatEmoticon.currentItem = position
    val size = rvChatEmoticon.adapter!!.itemCount
    for(i in 0 until size){
        if(i == position){//눌렀을때
            //이모티콘 -> filapth uri 로 변환
            val uri = Uri.parse(emoAllInfoList.find {
                it.isSetCategoryImg && it.emoticonSetId == setItems[i].id && it.title == "on"
            }?.filePath+".webp")
            //glide로 넣을시 화면이 느리게 업데이트된다.
            (rvChatEmoticon.findViewHolderForAdapterPosition(i) as EmoticonTabAdapter.ViewHolder).binding.ivTabEmoticon.setImageURI(uri)
        } else {//눌린거 풀렸을때
            val uri = Uri.parse(emoAllInfoList.find { it.isSetCategoryImg && it.emoticonSetId == setItems[i].id && it.title == "off"}?.filePath+".webp")
            (rvChatEmoticon.findViewHolderForAdapterPosition(i) as EmoticonTabAdapter.ViewHolder).binding.ivTabEmoticon.setImageURI(uri)
        }
    }
}

fun onEmoticonClick(
    activity: BaseActivity,
    rootView: View,
    model: EmoticonDetailModel,
    @Suppress("UNUSED_PARAMETER") view: View,
    listener: OnEmoticonClickListener? = null,
    ) {
    val viewCommentBinding: ViewCommentBinding = DataBindingUtil.getBinding(rootView.findViewById(R.id.view_comment))
        ?: return
    val clPreview = rootView.findViewById<View?>(R.id.cl_preview) ?: return
    val ivPreview = rootView.findViewById<AppCompatImageView?>(R.id.iv_preview) ?: return

    if(emoModel != null) {   //두번째부터 여기
        if(emoModel!!.id == model.id){ //아이디가 같으면 동일한 이모티콘 눌렀다는 뜻이므로 전송버튼 누름.
            mLastEmoTime = SystemClock.elapsedRealtime()
            if(isEmoClicked && (mLastEmoTime - mFirstEmoTime <300)){    //빠르게 눌렀다면

                viewCommentBinding.btnSubmit.callOnClick()
                isEmoClicked = false
            }
            else{   //느리게 눌렀다면
                mFirstEmoTime = SystemClock.elapsedRealtime()
                clPreview.visibility = View.VISIBLE
                isEmoClicked = true
            }
        } else { //같지않으면 다른 이모티콘이므로 프리뷰에 다시보여주고 모델 다시 세팅.
            clPreview.visibility = View.VISIBLE
            emoModel = model
            mFirstEmoTime = SystemClock.elapsedRealtime()
            isEmoClicked = true
        }
    } else { //맨처음 들어갔을때.
        clPreview.visibility = View.VISIBLE
        emoModel = model
        mFirstEmoTime = SystemClock.elapsedRealtime()
    }
    listener?.onClickEmoticon(model.id)

    //이모티콘 -> filapth uri 로 변환
    val uri = Uri.parse(model.filePath +".webp")
    Glide.with(activity)
        .load(uri.path)
        .transform(CenterCrop())
        .into(ivPreview)


}
