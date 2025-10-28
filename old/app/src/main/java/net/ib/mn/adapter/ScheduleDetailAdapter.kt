package net.ib.mn.adapter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.databinding.ScheduleDetailItemBinding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.ScheduleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import java.text.DateFormat
import java.text.SimpleDateFormat

class ScheduleDetailAdapter(
    private val mContext: Context,
    private val mIds: HashMap<Int, String>?,
    private var schedule: ArrayList<ScheduleModel>?,
    private val userId: Int,
    private val most: Boolean,
    private val level: Boolean,
    private val mListener: ScheduleDeleteClickListener,
    private val editClickListener: ScheduleEditClickListener,
    private val voteListener: ScheduleVoteClickListener,
    private val commentListener: ScheduleCommentClickListener
) : BaseAdapter() {
    interface ScheduleEditClickListener {
        fun onClick(idol: String, item: ScheduleModel)
    }

    interface ScheduleDeleteClickListener {
        fun onClick(id: Int)
    }

    interface ScheduleVoteClickListener {
        fun onClick(id: Int, vote: String, itemPosition: Int)
    }

    interface ScheduleCommentClickListener {
        fun onClick(view: View, item: ScheduleModel, pos: Int)
    }

    private val inflater: LayoutInflater
    private var idolIds: Array<String> = emptyArray()

    init {
        this.inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    // 스케줄 수정 후 반영
    fun setScheduleData(schedule: ArrayList<ScheduleModel>?) {
        this.schedule = schedule
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return schedule?.size ?: 0
    }

    override fun getItem(position: Int): ScheduleModel {
        return schedule!![position]
    }

    fun getDateString(item: ScheduleModel): String {
        val formatter = DateFormat.getDateInstance(DateFormat.FULL, getAppLocale(mContext))
        val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
        val sdf = SimpleDateFormat(localPattern, getAppLocale(mContext))
        val dateString = sdf.format(item.dtstart)
        return dateString
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ScheduleDetailItemBinding

        if (convertView == null) {
            binding = ScheduleDetailItemBinding.inflate(
                LayoutInflater.from(mContext),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as ScheduleDetailItemBinding
        }

        val item = getItem(position)

        val dateString = getDateString(item)

        if (position == 0) {
            binding.date.setText(dateString)
            binding.date.setVisibility(View.VISIBLE)
            binding.viewDate.setVisibility(View.VISIBLE)
        } else {
            prevDate = getDateString(getItem(position - 1))
            if (prevDate.startsWith(dateString)) {
                binding.date.setVisibility(View.GONE)
                binding.viewDate.setVisibility(View.GONE)
            } else {
                binding.date.setText(dateString)
                binding.date.setVisibility(View.VISIBLE)
                binding.viewDate.setVisibility(View.VISIBLE)
            }
        }

        binding.comment.setOnClickListener(View.OnClickListener { v ->
            commentListener.onClick(
                v,
                item,
                position
            )
        })

        binding.scheduleTitle.setText(item.title)
        if (item.allday == 1) {
            binding.scheduleDate.setVisibility(View.GONE)
        } else {
            val sdf = SimpleDateFormat("a h:mm", getAppLocale(mContext))
            val time = sdf.format(item.dtstart)
            binding.scheduleDate.setText(time)
            binding.scheduleDate.setVisibility(View.VISIBLE)
        }
        idolIds = item.idol_ids
            .toString()
            .replace("[", "")
            .replace("]", "")
            .replace("\\.0".toRegex(), "")
            .split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var idsString = ""
        for (idolId in idolIds!!) {
            if (!idolId.isEmpty() && mIds!!.get(idolId.trim { it <= ' ' }.toInt()) != null) {
                if (idsString.isEmpty()) {
                    idsString = mIds.get(
                        idolId.trim { it <= ' ' }.toInt()
                    ).toString()
                } else {
                    idsString += "," + mIds.get(idolId.trim { it <= ' ' }.toInt()).toString()
                }
            }
        }
        binding.scheduleIdol.setText(Util.orderByString(idsString))
        binding.scheduleIdolWrapper.setVisibility(if (BuildConfig.CELEB) View.GONE else View.VISIBLE)
        if (item.url.isNullOrEmpty()) {
            binding.scheduleUrlWrapper.setVisibility(
                View.GONE
            )
        } else {
            binding.scheduleUrl.setText(item.url)
            binding.scheduleUrlWrapper.setVisibility(View.VISIBLE)
            binding.scheduleUrl.setLinkURL()
        }
        if (item.extra.isNullOrEmpty()) {
            binding.scheduleInfoWrapper.setVisibility(
                View.GONE
            )
        } else {
            binding.scheduleInfo.setText(item.extra)
            binding.scheduleInfoWrapper.setVisibility(View.VISIBLE)
        }
        if (item.location.isNullOrEmpty()) {
            binding.scheduleLocation.setVisibility(View.GONE)
        } else {
            binding.scheduleLocation.setVisibility(View.VISIBLE)
            binding.mapTv.setText(item.location)
            binding.scheduleLocation.setOnClickListener(View.OnClickListener { v ->
                if (BuildConfig.CHINA) {
                    return@OnClickListener
                } // 중국버전은 지도 막음
                try {
                    val i = Intent(
                        Intent.ACTION_VIEW,
                        ("geo:0,0?q=" + item.lat + "," + item.lng + "("
                            + item.location + ")").toUri()
                    )
                    i.setClassName(
                        "com.google.android.apps.maps",
                        "com.google.android.maps.MapsActivity"
                    )
                    mContext.startActivity(i)
                } catch (e: ActivityNotFoundException) {
                    Util.showIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(R.string.msg_error_ok),
                        View.OnClickListener { v1 -> Util.closeIdolDialog() })

                    e.printStackTrace()
                }
            })
        }
        if (item.user == null) {
            binding.scheduleUserLevel.setVisibility(View.GONE)
            binding.scheduleUserName.setVisibility(View.GONE)
        } else {
            binding.scheduleUserLevel.setVisibility(View.VISIBLE)
            binding.scheduleUserName.setVisibility(View.VISIBLE)
            binding.scheduleUserLevel.setImageBitmap(Util.getLevelImage(mContext, item.user))
            binding.scheduleUserName.setText(item.user?.nickname)
        }

        binding.scheduleComment.setText(item.num_comments.toString())
        if (item.category != null) {
            binding.scheduleIcon.setImageResource(Util.getScheduleIcon(item.category))
        }

        val account = getAccount(mContext)
        if ((item.user != null && item.user?.id == userId)
            || account?.heart == Const.LEVEL_ADMIN || account?.heart == Const.LEVEL_MANAGER
        ) {
            binding.scheduleEdit.setVisibility(View.VISIBLE)
            binding.scheduleDelete.setVisibility(View.VISIBLE)
            val idol = binding.scheduleIdol.getText().toString()
            binding.scheduleEdit.setOnClickListener(View.OnClickListener { v ->
                editClickListener.onClick(idol, item)
            })
            binding.scheduleDelete.setOnClickListener(View.OnClickListener { v ->
                mListener.onClick(
                    item.id
                )
            })
        } else {
            binding.scheduleEdit.setVisibility(View.GONE)
            binding.scheduleDelete.setVisibility(View.GONE)
        }

        binding.report1.setOnClickListener(View.OnClickListener { v ->
            if (!most) {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    mContext.getString(if (BuildConfig.CELEB) R.string.schedule_eval_most_actor else R.string.schedule_eval_most),
                    View.OnClickListener { v1 -> Util.closeIdolDialog() },
                    true
                )
            } else if (!level) {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    String.format(
                        mContext.getString(R.string.schedule_eval_level),
                        getInstance(mContext).scheduleVoteLevel.toString()
                    ),
                    View.OnClickListener { v12 -> Util.closeIdolDialog() },
                    true
                )
            } else if (item.vote.isNullOrEmpty()) {
                val msg = mContext.getString(R.string.schedule_yes)
                Util.showDefaultIdolDialogWithBtn2(
                    mContext,
                    null,
                    msg,
                    View.OnClickListener { v13 ->
                        voteListener.onClick(item.id, "Y", position)
                        Util.closeIdolDialog()
                    },
                    View.OnClickListener { v14 -> Util.closeIdolDialog() })
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    mContext.getString(R.string.schedule_eval_done),
                    View.OnClickListener { v15 -> Util.closeIdolDialog() },
                    true
                )
            }
        })

        binding.report2.setOnClickListener(View.OnClickListener { v ->
            if (!most) {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    mContext.getString(if (BuildConfig.CELEB) R.string.schedule_eval_most_actor else R.string.schedule_eval_most),
                    View.OnClickListener { v16 -> Util.closeIdolDialog() },
                    true
                )
            } else if (!level) {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    String.format(
                        mContext.getString(R.string.schedule_eval_level),
                        getInstance(mContext).scheduleVoteLevel.toString()
                    ),
                    View.OnClickListener { v18 -> Util.closeIdolDialog() },
                    true
                )
            } else if (item.vote.isNullOrEmpty()) {
                val msg = mContext.getString(R.string.schedule_no)
                Util.showDefaultIdolDialogWithBtn2(
                    mContext,
                    null,
                    msg,
                    View.OnClickListener { v17 ->
                        voteListener.onClick(item.id, "N", position)
                        Util.closeIdolDialog()
                    },
                    View.OnClickListener { v19 -> Util.closeIdolDialog() })
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    mContext.getString(R.string.schedule_eval_done),
                    View.OnClickListener { v110 -> Util.closeIdolDialog() },
                    true
                )
            }
        })
        binding.report3.setOnClickListener(View.OnClickListener { v ->
            if (!most) {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    mContext.getString(if (BuildConfig.CELEB) R.string.schedule_eval_most_actor else R.string.schedule_eval_most),
                    View.OnClickListener { v111 -> Util.closeIdolDialog() },
                    true
                )
            } else if (!level) {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    String.format(
                        mContext.getString(R.string.schedule_eval_level),
                        getInstance(mContext).scheduleVoteLevel.toString()
                    ),
                    View.OnClickListener { v112 -> Util.closeIdolDialog() },
                    true
                )
            } else if (item.vote.isNullOrEmpty()) {
                val msg = mContext.getString(R.string.schedule_dupl)
                Util.showDefaultIdolDialogWithBtn2(
                    mContext,
                    null,
                    msg,
                    View.OnClickListener { v113 ->
                        voteListener.onClick(item.id, "D", position)
                        Util.closeIdolDialog()
                    },
                    View.OnClickListener { v114 -> Util.closeIdolDialog() })
            } else {
                Util.showDefaultIdolDialogWithBtn1(
                    mContext,
                    null,
                    mContext.getString(R.string.schedule_eval_done),
                    View.OnClickListener { v115 -> Util.closeIdolDialog() },
                    true
                )
            }
        })

        if (item.vote.equals("Y", ignoreCase = true)) {
            binding.ivReport1.apply {
                setImageResource(R.drawable.btn_schedule_yes_on)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                imageTintMode = PorterDuff.Mode.SRC_IN
            }
            binding.tvReport1.setTextColor(ContextCompat.getColor(mContext, R.color.main))
        } else {
            binding.ivReport1.apply {
                setImageResource(R.drawable.btn_schedule_yes_off)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray300))
                imageTintMode = PorterDuff.Mode.SRC_IN
            }
            binding.tvReport1.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
        }
        if (item.vote.equals("N", ignoreCase = true)) {
            binding.ivReport2.apply {
                setImageResource(R.drawable.btn_schedule_no_on)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                imageTintMode = PorterDuff.Mode.SRC_IN
            }
            binding.tvReport2.setTextColor(ContextCompat.getColor(mContext, R.color.main))
        } else {
            binding.ivReport2.apply {
                setImageResource(R.drawable.btn_schedule_no_off)
                imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray300))
                imageTintMode = PorterDuff.Mode.SRC_IN
            }
            binding.tvReport2.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
        }
        if (item.vote.equals("D", ignoreCase = true)) {
            binding.ivReport3.setImageResource(R.drawable.btn_schedule_overlap_on)
            binding.tvReport3.setTextColor(ContextCompat.getColor(mContext, R.color.main))
        } else {
            binding.ivReport3.setImageResource(R.drawable.btn_schedule_overlap_off)
            binding.tvReport3.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
        }

        // 190406 맞아요/틀려요 갯수
        binding.tvReport1.setText(item.num_yes.toString())
        binding.tvReport2.setText(item.num_no.toString())

        return binding.root
    }

    companion object {
        private var prevDate = ""
    }
}

