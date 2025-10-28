package net.ib.mn.adapter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.activity.HeartsFromFriendsActivity
import net.ib.mn.databinding.MyheartListChildItemBinding
import net.ib.mn.fragment.MyheartHistoryFragment
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class MyHeartExpandableRcyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    //collapse될때  아이템 사라지는것 구현을 위한 가변용 array 변수
    private var heartDiaLogList: JSONArray? = JSONArray()

    //받아온 로그 데이터 계속 유지하는 array
    private var cloneHeartDiaLogList: JSONArray? = JSONArray()

    //적립내역인지, 사용 내역인지 구분
    private var isSpendLog: Boolean = false

    //다이아 로그인지 하트 로그인지 구분
    private var checkLogType: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyHearListChildViewHolder(MyheartListChildItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MyHearListChildViewHolder).bind(
            cloneHeartDiaLogList?.get(position) as JSONObject,
            checkLogType
        )
    }

    override fun getItemViewType(position: Int): Int {
        return CHILD_VIEW_HOLDER
    }


    //로그 데이터들 받아서 넣어줌.
    fun addDataList(historyLog: JSONArray?, checkLogType: Int, isSpendLog: Boolean, isExpanded: Boolean) {
        this.isSpendLog = isSpendLog
        this.checkLogType = checkLogType
        this.cloneHeartDiaLogList = JSONArray()
        this.cloneHeartDiaLogList = historyLog
        collapse(isExpanded)
    }

    override fun getItemCount(): Int {
        return heartDiaLogList?.length()!!
    }

    //isExpanded 가  true이면  clone 해둔  로그 리스트를  뿌려주고
    //false이면  빈 array를 적용해서  리스트를 없애준다.
    fun collapse(isExpanded: Boolean) {
        heartDiaLogList = if (isExpanded) {
            cloneHeartDiaLogList
        } else {
            JSONArray()
        }
        notifyDataSetChanged()
    }


    //하트 아디어 childview viewholder
    inner class MyHearListChildViewHolder(val binding: MyheartListChildItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(heartDiaLogList: JSONObject, logType: Int) { with(binding) {

            val titleText = heartDiaLogList.optString("title")

            val currencyText = when (logType) {
                MyheartHistoryFragment.DIAMOND_LOG -> {
                    NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(heartDiaLogList.optString("diamond" , "0").toInt())
                }
                MyheartHistoryFragment.HEART_LOG -> {
                    NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(heartDiaLogList.optString("heart", "0").toInt())
                }
                else -> {
                    ""
                }
            }

            val desc = heartDiaLogList.optString("desc")

            if (desc.isNullOrEmpty() || desc == "null") {
                layoutSpendOnlyTitle.visibility = View.VISIBLE
                layoutSpend.visibility = View.GONE

                tvTitleOnly.text = titleText
            } else {
                layoutSpendOnlyTitle.visibility = View.GONE
                layoutSpend.visibility = View.VISIBLE

                tvTitle.text = titleText
                tvSubTitle.text = desc
            }

            if (isSpendLog) {
                tvCurrency.apply {
                    text = "- $currencyText"
                    setTextColor(context.getColor(R.color.text_gray))
                }

                when (logType) {
                    MyheartHistoryFragment.DIAMOND_LOG -> {
                        ivTypeIcon.setImageResource(R.drawable.icon_spend_dia)
                    }
                    MyheartHistoryFragment.HEART_LOG -> {
                        ivTypeIcon.setImageResource(R.drawable.icon_spend_heart)
                    }
                    else -> {
                        // no-op
                    }
                }
            } else {
                tvCurrency.apply {
                    text = currencyText.toString()
                    setTextColor(context.getColor(R.color.main_light))
                }

                when (logType) {
                    MyheartHistoryFragment.DIAMOND_LOG -> {
                        ivTypeIcon.setImageResource(R.drawable.icon_earn_dia)
                    }
                    MyheartHistoryFragment.HEART_LOG -> {
                        ivTypeIcon.setImageResource(R.drawable.icon_earn_heart)
                    }
                    else -> {
                        // no-op
                    }
                }
            }

            //친구에게서 받은   용도 일때
            if (heartDiaLogList.getString("key") == "E/friend") {
                ivMoreFriend.visibility = View.VISIBLE
                //친구에게 받은 하트는 클릭시  HeartsFromFriendsActivity 로 이동
                heartsFromFriends.setOnClickListener {
                    val intent = Intent(itemView.context, HeartsFromFriendsActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("title", heartDiaLogList.optString("title" , "title"))
                    intent.putExtras(bundle)
                    itemView.context.startActivity(intent)
                }
            }else{
                tvCurrency.visibility = View.VISIBLE
                //가끔  친구 보내기 데이터 있어서 visible로 해놓으면
                //다른 key에서  visisble로 남아있는 경우 있어서 gone처리 한번더 함.
                ivMoreFriend.visibility = View.GONE
            }

        }}
    }

    companion object {
        const val CHILD_VIEW_HOLDER = 1
    }
}