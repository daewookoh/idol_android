package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.account.IdolAccount.Companion.sAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.NewCommentActivity.Companion.createIntent
import net.ib.mn.adapter.ScheduleDetailAdapter
import net.ib.mn.adapter.ScheduleDetailAdapter.ScheduleDeleteClickListener
import net.ib.mn.addon.IdolGson.getInstance
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.ScheduleRepositoryImpl
import net.ib.mn.core.data.repository.friends.FriendsRepository
import net.ib.mn.databinding.ActivityScheduleDetailBinding
import net.ib.mn.fragment.ScheduleFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.ScheduleModel
import net.ib.mn.schedule.IdolSchedule.Companion.getInstance
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger.Companion.v
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.UtilK.Companion.getTypeList
import net.ib.mn.utils.ext.applySystemBarInsets
import org.json.JSONObject
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleDetailActivity : BaseActivity(), ScheduleDetailAdapter.ScheduleEditClickListener,
    ScheduleDeleteClickListener, ScheduleDetailAdapter.ScheduleVoteClickListener,
    View.OnClickListener, ScheduleDetailAdapter.ScheduleCommentClickListener {
    private var schedule: ArrayList<ScheduleModel> = ArrayList()
    private var scheduleId = 0
    private var mIds: HashMap<Int, String>? = null
    private var mAdapter: ScheduleDetailAdapter? = null
    private var mAccount: IdolAccount? = null
    private var most = false
    private var levelAdd = false
    private var levelVote = false
    private var mIdol: IdolModel? = null
    private var date: Date? = null

    @Inject
    lateinit var scheduleRepository: ScheduleRepositoryImpl
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var friendsRepository: FriendsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private lateinit var binding: ActivityScheduleDetailBinding

    override fun onResume() {
        super.onResume()
        //        schedule = IdolSchedule.getInstance().getSchedules();
        mAdapter!!.notifyDataSetChanged()
        if (sAccount == null) {
            mAccount = getAccount(this)
            if (mAccount != null) {
                accountManager.fetchUserInfo(this, {
                    getAccount(this)?.fetchFriendsInfo(this, friendsRepository, null)
                })
            }
        }
    }

    override fun onClick(idol: String, item: ScheduleModel) {
        val i = ScheduleWriteActivity.createIntent(
            this@ScheduleDetailActivity,
            if (BuildConfig.CELEB) mIdol else null,
            item,
            true
        )

        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "schedule_detail_schedule_edit"
        )
        // 수정 후 갱신되게...
        startActivityForResult(i, EDIT_REQUEST_CODE)
    }

    override fun onClick(id: Int) {
        setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "schedule_detail_schedule_delete"
        )

        Util.showDefaultIdolDialogWithBtn2(
            this,
            getString(R.string.title_remove),
            getString(R.string.confirm_delete),
            { v: View? ->
                MainScope().launch {
                    scheduleRepository.delete(
                        id,
                        { response ->
                            if (response.optBoolean("success")) {
                                for (sch in schedule) {
                                    if (sch.id == id) {
                                        getInstance().deleteScheduleItem(id)
                                        schedule.remove(sch)
                                        break
                                    }
                                }
                                if (mAdapter!!.count > 0) binding.empty.visibility = View.GONE
                                else binding.empty.visibility = View.VISIBLE
                                mAdapter!!.notifyDataSetChanged()
                            } else {
                                UtilK.handleCommonError(this@ScheduleDetailActivity, response)
                            }
                        }, { throwable ->
                            makeText(
                                this@ScheduleDetailActivity,
                                R.string.error_abnormal_exception,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                Util.closeIdolDialog()
            },
            { v: View? -> Util.closeIdolDialog() })
    }

    override fun onClick(id: Int, vote: String, positon: Int) {
        vote(id, vote, positon)
    }

    fun vote(id: Int, vote: String?, itemPosition: Int) {
        val scheduleItem = schedule[itemPosition]

        val listener: (JSONObject) -> Unit = { response ->
            if (!response.optBoolean("success")) {
                val responseMsg = ErrorControl.parseError(this@ScheduleDetailActivity, response)
                if (responseMsg != null) {
                    Util.showDefaultIdolDialogWithBtn1(
                        this@ScheduleDetailActivity,
                        null,
                        responseMsg,
                        { v1: View? -> Util.closeIdolDialog() },
                        true
                    )
                }
            } else { //vote 가  success 일때

                v("스케쥴 vote 얍얍얍 ->$response")
                val v = binding.list.getChildAt(itemPosition - binding.list.firstVisiblePosition)

                val tvReport1 = v.findViewById<View>(R.id.tv_report1) as TextView
                val ivReport1 = v.findViewById<View>(R.id.iv_report1) as ImageView

                val tvReport2 = v.findViewById<View>(R.id.tv_report2) as TextView
                val ivReport2 = v.findViewById<View>(R.id.iv_report2) as ImageView

                val tvReport3 = v.findViewById<View>(R.id.tv_report3) as TextView
                val ivReport3 = v.findViewById<View>(R.id.iv_report3) as ImageView


                if (schedule[itemPosition].vote.isNullOrEmpty()) {
                    when (vote) {
                        "Y" -> {
                            ivReport1.apply {
                                setImageResource(R.drawable.btn_schedule_yes_on)
                                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                                imageTintMode = PorterDuff.Mode.SRC_IN
                            }
                            tvReport1.setTextColor(
                                ContextCompat.getColor(
                                    this@ScheduleDetailActivity, R.color.main
                                )
                            )
                            scheduleItem.vote = vote
                            scheduleItem.num_yes = scheduleItem.num_yes + 1
                            tvReport1.text = scheduleItem.num_yes.toString()
                            ScheduleFragment.loadOnce = false
                        }

                        "N" -> {
                            ivReport2.apply {
                                setImageResource(R.drawable.btn_schedule_no_on)
                                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                                imageTintMode = PorterDuff.Mode.SRC_IN
                            }
                            tvReport2.setTextColor(
                                ContextCompat.getColor(
                                    this@ScheduleDetailActivity, R.color.main
                                )
                            )
                            scheduleItem.vote = vote
                            scheduleItem.num_no = scheduleItem.num_no + 1
                            tvReport2.text = scheduleItem.num_no.toString()
                            ScheduleFragment.loadOnce = false
                        }

                        "D" -> {
                            ivReport3.setImageResource(R.drawable.btn_schedule_overlap_on)
                            tvReport3.setTextColor(
                                ContextCompat.getColor(
                                    this@ScheduleDetailActivity, R.color.main
                                )
                            )
                            ScheduleFragment.loadOnce = false
                            scheduleItem.vote = "D"
                        }
                    }
                }
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(msg)) {
                errorText += msg
            }
            makeText(
                this@ScheduleDetailActivity, errorText,
                Toast.LENGTH_SHORT
            ).show()
        }

        MainScope().launch {
            scheduleRepository.vote(id, vote, listener, errorListener)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScheduleDetailBinding.inflate(layoutInflater)
        binding.flContainer.applySystemBarInsets()
        setContentView(binding.root)

        schedule = ArrayList()

        val actionbar = supportActionBar
        actionbar!!.title = getString(R.string.schedule_detail)
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeButtonEnabled(false)

        binding.btnScheduleWrite.setOnClickListener(this)

        //        schedule = IdolSchedule.getInstance().getSchedules();
        mAccount = getAccount(this)
        date = intent.getSerializableExtra("date") as Date?
        scheduleId = intent.getIntExtra("schedule_id", 1234)
        most = intent.getBooleanExtra("most", false)
        levelAdd = intent.getBooleanExtra("level_add", false)
        levelVote = intent.getBooleanExtra("level_vote", false)
        mIdol = intent.getSerializableExtra("idol") as IdolModel?
        mIds = intent.getSerializableExtra("ids") as? HashMap<Int, String>
        mAdapter = ScheduleDetailAdapter(
            this,
            mIds,
            schedule,
            mAccount!!.userModel!!.id,
            most,
            levelVote,
            this,
            this,
            this,
            this
        )
        binding.list.setAdapter(mAdapter)
        binding.list.setDivider(null)
        //스케줄 상세화면은 전부다 한번에 가져옴.
        loadAllSchedule()

        //        mAdapter.notifyDataSetChanged();
        if (BuildConfig.CELEB) {
            val TypeList = getTypeList(this, mIdol!!.type + mIdol!!.category)
            binding.btnScheduleWrite.setColorFilter(
                if (TypeList.type == null) resources.getColor(R.color.main) else Color.parseColor(
                    Util.getFontColor(
                        this, TypeList
                    )
                ), PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun loadAllSchedule() {
        val listener: (JSONObject) -> Unit = { response ->
            schedule.clear() //수정하고 나왔을때 리로드 해야되니까 이전거 제거해줌.
            val gson = getInstance(false)
            val listType = object : TypeToken<List<ScheduleModel?>?>() {
            }.type
            Util.log("list is::$response")
            if (response.optBoolean("success")) {
                val idols = gson.fromJson<List<ScheduleModel>>(
                    response.optJSONArray("objects").toString(), listType
                )
                Util.log("idols size ->" + idols.size)
                for (idol in idols) {
                    val month = idol.dtstart.month
                    if (month != date!!.month) continue
                    if (idol.allday == 1) {
                        idol.dtstart = Util.dateToUTC(idol.dtstart)
                    }
                    schedule.add(idol)
                }
                mAdapter!!.notifyDataSetChanged()

                var i = 0
                if (!schedule.isEmpty()) {
                    for (sch in schedule) {
                        if (sch.id == scheduleId) break
                        i++
                    }
                }
                binding.list.setSelection(i)
            }
        }

        val errorListener: (Throwable) -> Unit = { throwable ->
            val msg = throwable.message
            var errorText = getString(R.string.error_abnormal_default)
            if (!TextUtils.isEmpty(msg)) {
                errorText += msg
            }
            makeText(this@ScheduleDetailActivity, errorText, Toast.LENGTH_SHORT).show()
        }

        Util.log("Date -> " + date!!.year)
        lifecycleScope.launch {
            scheduleRepository.getSchedules(
                idolId = if (BuildConfig.CELEB) mIdol!!.getId() else mIdol!!.groupId,
                yearmonth = (date!!.year + 1900).toString() + String.format(Locale.US, "%02d", (date!!.month + 1)),
                vote = 1,
                locale = getInstance().scheduleLocaleString,
                listener = listener,
                errorListener = errorListener
            )
        }
    }

    // 댓글 아이콘 클릭
    override fun onClick(v: View, item: ScheduleModel, pos: Int) {
        when (v.id) {
            R.id.comment -> lifecycleScope.launch {
                miscRepository.getResource(
                    "articles/" + item.article_id + "/",
                    { response ->
                        val article = instance.fromJson(response.toString(), ArticleModel::class.java)
                        val intent = createIntent(this@ScheduleDetailActivity, article, -1, item, true, mIds, false)
                        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivityForResult(intent, COMMENT_REQUEST_CODE)
                    },
                    {
                        makeText(
                            this@ScheduleDetailActivity,
                            R.string.error_abnormal_default,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_schedule_write -> if (!most) {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    getString(if (BuildConfig.CELEB) R.string.schedule_write_most_actor else R.string.schedule_write_most),
                    { v1: View? -> Util.closeIdolDialog() },
                    true
                )
            } else if (!levelAdd) {
                Util.showDefaultIdolDialogWithBtn1(
                    this,
                    null,
                    String.format(
                        getString(R.string.schedule_write_level),
                        ConfigModel.getInstance(this).scheduleAddLevel.toString()
                    ),
                    { v12: View? -> Util.closeIdolDialog() },
                    true
                )
            } else startActivityForResult(
                ScheduleWriteActivity.createIntent(this, mIdol, date),
                WRITE_REQUEST_CODE
            )
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK || resultCode == ResultCode.EDITED.value) {
            when (requestCode) {
                WRITE_REQUEST_CODE -> {}
                COMMENT_REQUEST_CODE -> if (getInstance().lastSchedule != null) {
                    if (getInstance().lastSchedule != null) {
                        val id = getInstance().lastSchedule!!.id
                        val comment = getInstance().lastSchedule!!.num_comments
                        var i = 0
                        while (i < mAdapter!!.count) {
                            if (mAdapter!!.getItem(i).id == id) mAdapter!!.getItem(i).num_comments =
                                comment
                            i++
                        }
                        mAdapter!!.notifyDataSetChanged()
                        getInstance().lastSchedule = null
                    }
                }
            }
        }

        if (requestCode == EDIT_REQUEST_CODE && resultCode == RESULT_OK) {
            loadAllSchedule() //수정하고나오면 리스트 다시불러줌.
        } else if (requestCode == COMMENT_REQUEST_CODE &&
            (resultCode in intArrayOf(ResultCode.COMMENTED.value, ResultCode.COMMENT_REMOVED.value)) ){
            loadAllSchedule() // 댓글 작성하고오면 리스트 다시 불러줌
        }
    }

    companion object {
        private const val WRITE_REQUEST_CODE = 1000
        private const val COMMENT_REQUEST_CODE = 2000
        const val EDIT_REQUEST_CODE: Int = 3000

        fun createIntent(
            context: Context?,
            idol: IdolModel?,
            mIds: HashMap<*, *>?,
            date: Date?,
            scheduleId: Int,
            most: Boolean,
            levelAdd: Boolean,
            levelVote: Boolean
        ): Intent {
            val intent = Intent(context, ScheduleDetailActivity::class.java)
            intent.putExtra("date", date)
            intent.putExtra("schedule_id", scheduleId)
            intent.putExtra("most", most)
            intent.putExtra("level_add", levelAdd)
            intent.putExtra("level_vote", levelVote)
            intent.putExtra("idol", idol as Parcelable?)
            intent.putExtra("ids", mIds)
            return intent
        }
    }
}
