package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.ScheduleIdolAdapter
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.ActivityScheduleWriteIdolBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONObject
import java.util.Collections
import javax.inject.Inject

// 스케줄 아이돌 선택 화면. 셀럽에서는 사용하지 않음.
@AndroidEntryPoint
class ScheduleWriteIdolActivity : BaseActivity(), View.OnClickListener,
    ScheduleIdolAdapter.OnIdolClickListener {
    private var mGroup: IdolModel? = null
    private var mAdapter: ScheduleIdolAdapter? = null
    private var mIdols: ArrayList<IdolModel> = ArrayList()
    private var posY = 0
    private var fixY = 0
    private var resultIntent: Intent? = null
    private var isGroupSelected = false
    private var isSelected = false
    @Inject
    lateinit var idolsRepository: IdolsRepository

    //아이돌이 아무도 선택이 안되어있는 경우 true , 아이돌 선택이 하나라도 되어있는 경우 false
    private var isNoIdolSelected = false

    private lateinit var binding: ActivityScheduleWriteIdolBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScheduleWriteIdolBinding.inflate(layoutInflater)
        binding.scrollView.applySystemBarInsets()
        setContentView(binding.root)

        mIdols = intent.getSerializableExtra(CommunityActivity.PARAM_IDOL) as ArrayList<IdolModel>
        isGroupSelected = intent.getBooleanExtra(PARAM_GROUP_SELECTED, false)

        //아이돌이 처음에 선택되어있는지 여부를 체크한다. -> deafult값은 true로  아이돌이 아무도 선택안되어있음으로 적용 .
        isNoIdolSelected = intent.getBooleanExtra(PARAM_NO_IDOL_SELECTED, true)

        mAdapter = ScheduleIdolAdapter(this, mIdols, this, isNoIdolSelected)

        binding.individualView.setAdapter(mAdapter)
        binding.individualView.isExpanded = true
        binding.group.setOnClickListener(this)
        binding.scrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                posY = scrollY
            })
        binding.scrollView.addOnLayoutChangeListener(
            View.OnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
                //                scrollView.removeOnLayoutChangeListener(this);
                binding.scrollView.scrollTo(0, fixY)
            })

        posY = 0
        resultIntent = Intent()

        val actionbar = supportActionBar
        actionbar!!.setTitle(getString(R.string.stats_idol))
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeButtonEnabled(false)

        try {
            if (mIdols!![0].type.equals("G", ignoreCase = true)) {
                mGroup = mIdols!![0]
                if (isGroupSelected) binding.group.callOnClick()
            }
            loadIdols(mIdols!![0].groupId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (BuildConfig.CELEB) {
            binding.tvGroup.setText(R.string.actor_lable_group)
            binding.tvIndividual.setText(R.string.actor_lable_individual)
        }
    }

    private fun loadIdols(id: Int) {
        val listener: ((JSONObject) -> Unit) = { response ->
            val gson = getInstance(true)
            val listType = object : TypeToken<List<IdolModel?>?>() {
            }.type
            val idols =
                try {
                    gson.fromJson<MutableList<IdolModel?>>(
                        response.optJSONArray("objects")?.toString() ?: "", listType
                    )
                } catch (_: Exception) {
                    Collections.emptyList<IdolModel?>()
                }
            for (idol in idols) {
                if (idol!!.type.equals("G", ignoreCase = true)) mGroup = idol
                if (idol.getId() == mIdols[0].getId()) {
//                        idol.setLocalizedName(mContext);
                    mIdols[0].setName(idol.getName(mContext))
                }
            }
            idols.remove(mGroup)
            idols.sortWith(Comparator { lhs: IdolModel?, rhs: IdolModel? ->
                // 190406 이름순->나이순으로 변경
                if (rhs!!.birthDay == null) return@Comparator if ((lhs!!.birthDay == null)) 0 else -1
                if (lhs!!.birthDay == null) return@Comparator 1
                lhs.birthDay!!.compareTo(rhs.birthDay!!)
            })
            mAdapter!!.addAll(idols)
            for (idol in mIdols) {
                if (TextUtils.isEmpty(idol.getName(mContext))) {
                    for (idol2 in idols) {
                        if (idol.getId() == idol2!!.getId()) {
                            idol.setName(idol2.getName(mContext))
                        }
                    }
                }
            }
            binding.groupTv.text = mGroup!!.getName(mContext)
            mAdapter!!.notifyDataSetChanged()
        }

        val errorListener: ((Throwable) -> Unit) = { throwable ->
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(throwable.message)) {
                errorText += throwable.message
            }
            makeText(
                this@ScheduleWriteIdolActivity, errorText,
                Toast.LENGTH_SHORT
            ).show()
        }

        lifecycleScope.launch {
            idolsRepository.getGroupMembers(
                id,
                listener,
                errorListener
            )
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.group -> if (!binding.groupCheck.isChecked) {
                isSelected = true
                binding.group.setBackgroundResource(R.drawable.schedule_write_bg_select2)
                binding.groupTv.setTextColor(ContextCompat.getColor(this, R.color.gray580))
                binding.groupCheck.isChecked = !binding.groupCheck.isChecked
                mIdols!!.clear()
                mIdols!!.add(mGroup!!)
                fixY = posY
                binding.individualView.adapter = null
                binding.individualView.adapter = mAdapter
            }
        }
    }

    override fun onBackPressed() {
        resultIntent!!.putExtra("ids", mIdols)
        resultIntent!!.putExtra("isSelected", isSelected)
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }

    override fun onClick(idol: IdolModel) {
        isSelected = true
        if (mIdols[0].type.equals("G", ignoreCase = true)) {
            binding.group.setBackgroundResource(R.drawable.schedule_write_bg_select1)
            binding.groupTv.setTextColor(ContextCompat.getColor(this, R.color.gray200))
            binding.groupCheck.isChecked = false
            mIdols.clear()
        }
        var flag = true
        //체크 되어있을 때
        for (i in mIdols.indices) {
            if (mIdols[i].getId() == idol.getId()) {
                mIdols.removeAt(i)
                flag = false
                break
            }
        }

        //기존에 mids에 있는 값들로  선택된 아이돌들을 적용시켜버려서
        //첫클릭  (이전에는 아이돌 선택이 없던경우)에는 mids를 한번 비워주고  add한다.
        //이때부터는 아이돌 선택이 된거니까  isNoIdolSelected값은 false로 바꿔줌.
        if (isNoIdolSelected) {
            mIdols.clear()
            isNoIdolSelected = false
        }

        //체크 안되어있을 때
        if (flag) {
            mIdols.add(idol)
        }
        //아이돌 체크 다 해제했을 때
        if (mIdols.size == 0 || mIdols.size == mAdapter!!.count) {
            binding.group.callOnClick()
        }
    }

    companion object {
        private var mContext: Context? = null
        protected const val PARAM_GROUP_SELECTED: String = "group_selected"
        protected const val PARAM_NO_IDOL_SELECTED: String = "no_idol_select"

        fun createIntent(
            context: Context?,
            idol: ArrayList<IdolModel>,
            isGroupSelected: Boolean,
            isNoIdolSelected: Boolean
        ): Intent {
            val intent = Intent(
                context,
                ScheduleWriteIdolActivity::class.java
            )
            intent.putExtra(CommunityActivity.PARAM_IDOL, idol)
            intent.putExtra(PARAM_NO_IDOL_SELECTED, isNoIdolSelected)
            intent.putExtra(PARAM_GROUP_SELECTED, isGroupSelected)
            mContext = context
            return intent
        }
    }
}
