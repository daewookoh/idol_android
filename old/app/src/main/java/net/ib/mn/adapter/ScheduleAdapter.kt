package net.ib.mn.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.ScheduleItemBinding
import net.ib.mn.databinding.ScheduleOlderItemBinding
import net.ib.mn.databinding.TextureGalleryItemBinding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.ScheduleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import java.text.SimpleDateFormat

class ScheduleAdapter(
    private val mContext: Context,
    private val resID: Int,
    private val userId: Int,
    private val most: Boolean,
    private val level: Boolean,
//    private static final int LAYOUT_ID = R.layout.schedule_item;
    private val mEditClickListener: ScheduleEditClickListener,
    private val mRemoveClickListener: ScheduleRemoveClickListener,
    private val mListener: ScheduleDetailClickListener,
    private val voteListener: ScheduleVoteClickListener,
    private val commentListener: ScheduleCommentClickListener
) : ArrayAdapter<ScheduleModel>(
    mContext, resID
) {
    interface ScheduleEditClickListener {
        fun onClick(view: View, item: ScheduleModel)
    }

    interface ScheduleRemoveClickListener {
        fun onClick(view: View, id: Int, position: Int)
    }

    interface ScheduleDetailClickListener {
        fun onClick(view: View, item: ScheduleModel)
    }

    interface ScheduleVoteClickListener {
        fun onClick(id: Int, vote: String, view: View, position: Int)
    }

    interface ScheduleCommentClickListener {
        fun onClick(view: View, item: ScheduleModel, pos: Int)
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ViewBinding

        if (resID == R.layout.schedule_older_item) {
            if(convertView == null) {
                binding = ScheduleOlderItemBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                binding.root.tag = binding
            } else {
                binding = convertView.tag as ScheduleOlderItemBinding
            }
        } else {
            if(convertView == null) {
                binding = ScheduleItemBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                binding.root.tag = binding
            } else {
                binding = convertView.tag as ScheduleItemBinding
            }
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    inner class BindingProxy {
        private val viewBinding: ViewBinding
        val root: View
        val date: AppCompatTextView
        val icon: AppCompatImageView
        val title: AppCompatTextView
        val comment: AppCompatTextView
        val btnEdit: AppCompatImageView
        val btnRemove: AppCompatImageView
        val ivReport1: AppCompatImageView
        val tvReport1: AppCompatTextView
        val ivReport2: AppCompatImageView
        val tvReport2: AppCompatTextView
        val ivReport3: AppCompatImageView
        val tvReport3: AppCompatTextView
        val report1: View
        val report2: View
        val report3: View
        val commentWrapper: View
        val scheduleDetail: View

        constructor(binding: ScheduleOlderItemBinding) {
            viewBinding = binding
            root = binding.root
            date = binding.scheduleDate
            icon = binding.scheduleIcon
            title = binding.scheduleTitle
            comment = binding.scheduleComment
            btnEdit = binding.btnEdit
            btnRemove = binding.btnRemove
            ivReport1 = binding.ivReport1
            tvReport1 = binding.tvReport1
            ivReport2 = binding.ivReport2
            tvReport2 = binding.tvReport2
            ivReport3 = binding.ivReport3
            tvReport3 = binding.tvReport3
            report1 = binding.report1
            report2 = binding.report2
            report3 = binding.report3
            commentWrapper = binding.comment
            scheduleDetail = binding.scheduleDetail
        }

        constructor(binding: ScheduleItemBinding) {
            viewBinding = binding
            root = binding.root
            date = binding.scheduleDate
            icon = binding.scheduleIcon
            title = binding.scheduleTitle
            comment = binding.scheduleComment
            btnEdit = binding.btnEdit
            btnRemove = binding.btnRemove
            ivReport1 = binding.ivReport1
            tvReport1 = binding.tvReport1
            ivReport2 = binding.ivReport2
            tvReport2 = binding.tvReport2
            ivReport3 = binding.ivReport3
            tvReport3 = binding.tvReport3
            report1 = binding.report1
            report2 = binding.report2
            report3 = binding.report3
            commentWrapper = binding.comment
            scheduleDetail = binding.scheduleDetail
        }
    }

    override fun update(view: View?, item: ScheduleModel, position: Int) {
        val binding: BindingProxy
        if(resID == R.layout.schedule_older_item) {
            binding = BindingProxy(view?.tag as ScheduleOlderItemBinding)
        } else {
            binding = BindingProxy(view?.tag as ScheduleItemBinding)
        }

        with(binding) {
            title.setText(item.title)

            if (item.allday == 1) {
                date.setVisibility(View.GONE)
            } else {
                val sdf = SimpleDateFormat("a h:mm", getAppLocale(mContext))
                val time = sdf.format(item.dtstart)
                date.setText(time)
                date.setVisibility(View.VISIBLE)
            }

            comment.setText(item.num_comments.toString())

            if (item.category != null) {
                icon.setImageResource(Util.getScheduleIcon(item.category))
                icon.setVisibility(View.VISIBLE)
            } else {
                icon.setVisibility(View.GONE)
            }

            val account = getAccount(mContext)
            if ((item.user != null && item.user?.id == userId)
                || account!!.heart == Const.LEVEL_ADMIN || account.heart == Const.LEVEL_MANAGER
            ) {
                btnEdit.setVisibility(View.VISIBLE)
                btnRemove.setVisibility(View.VISIBLE)
                btnEdit.setOnClickListener(View.OnClickListener { v ->
                    mEditClickListener.onClick(
                        v,
                        item
                    )
                })
                btnRemove.setOnClickListener(
                    View.OnClickListener { v ->
                        mRemoveClickListener.onClick(
                            v,
                            item.id,
                            position
                        )
                    })
            } else {
                btnEdit.setVisibility(View.GONE)
                btnRemove.setVisibility(View.GONE)
            }

            scheduleDetail.setOnClickListener(View.OnClickListener { v ->
                mListener.onClick(
                    v,
                    item
                )
            })

            commentWrapper.setOnClickListener(View.OnClickListener { v ->
                commentListener.onClick(
                    v,
                    item,
                    position
                )
            })

            report1.setOnClickListener(View.OnClickListener { v ->
                if (!most) {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(if (BuildConfig.CELEB) R.string.schedule_eval_most_actor else R.string.schedule_eval_most),
                        View.OnClickListener { Util.closeIdolDialog() },
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
                        View.OnClickListener { Util.closeIdolDialog() },
                        true
                    )
                } else if (item.vote.isNullOrEmpty()) {
                    val msg = mContext.getString(R.string.schedule_yes)
                    Util.showDefaultIdolDialogWithBtn2(
                        mContext,
                        null,
                        msg,
                        View.OnClickListener {
                            voteListener.onClick(item.id, "Y", view, position)
                            Util.closeIdolDialog()
                        },
                        View.OnClickListener { Util.closeIdolDialog() })
                } else {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(R.string.schedule_eval_done),
                        View.OnClickListener { Util.closeIdolDialog() },
                        true
                    )
                }
            })
            report2.setOnClickListener(View.OnClickListener { v ->
                if (!most) {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(if (BuildConfig.CELEB) R.string.schedule_eval_most_actor else R.string.schedule_eval_most),
                        View.OnClickListener { Util.closeIdolDialog() },
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
                        View.OnClickListener { Util.closeIdolDialog() },
                        true
                    )
                } else if (item.vote.isNullOrEmpty()) {
                    val msg = mContext.getString(R.string.schedule_no)
                    Util.showDefaultIdolDialogWithBtn2(
                        mContext,
                        null,
                        msg,
                        View.OnClickListener {
                            voteListener.onClick(item.id, "N", view, position)
                            Util.closeIdolDialog()
                        },
                        View.OnClickListener { Util.closeIdolDialog() })
                } else {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(R.string.schedule_eval_done),
                        View.OnClickListener { Util.closeIdolDialog() },
                        true
                    )
                }
            })
            report3.setOnClickListener(View.OnClickListener { v ->
                if (!most) {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(if (BuildConfig.CELEB) R.string.schedule_eval_most_actor else R.string.schedule_eval_most),
                        View.OnClickListener { Util.closeIdolDialog() },
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
                        View.OnClickListener { Util.closeIdolDialog() }, true
                    )
                } else if (item.vote.isNullOrEmpty()) {
                    val msg = mContext.getString(R.string.schedule_dupl)
                    Util.showDefaultIdolDialogWithBtn2(
                        mContext,
                        null,
                        msg,
                        View.OnClickListener {
                            voteListener.onClick(item.id, "D", view, position)
                            Util.closeIdolDialog()
                        },
                        View.OnClickListener { Util.closeIdolDialog() })
                } else {
                    Util.showDefaultIdolDialogWithBtn1(
                        mContext,
                        null,
                        mContext.getString(R.string.schedule_eval_done),
                        View.OnClickListener { Util.closeIdolDialog() },
                        true
                    )
                }
            })


            if (item.vote.equals("Y", ignoreCase = true)) {
                ivReport1.apply {
                    setImageResource(R.drawable.btn_schedule_yes_on)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                    imageTintMode = PorterDuff.Mode.SRC_IN
                }
                tvReport1.setTextColor(ContextCompat.getColor(mContext, R.color.main))
            } else {
                ivReport1.apply {
                    setImageResource(R.drawable.btn_schedule_yes_off)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray300))
                    imageTintMode = PorterDuff.Mode.SRC_IN
                }
                tvReport1.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
            }
            if (item.vote.equals("N", ignoreCase = true)) {
                ivReport2.apply {
                    setImageResource(R.drawable.btn_schedule_no_on)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
                    imageTintMode = PorterDuff.Mode.SRC_IN
                }
                tvReport2.setTextColor(ContextCompat.getColor(mContext, R.color.main))
            } else {
                ivReport2.apply {
                    setImageResource(R.drawable.btn_schedule_no_off)
                    imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray300))
                    imageTintMode = PorterDuff.Mode.SRC_IN
                }
                tvReport2.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
            }
            if (item.vote.equals("D", ignoreCase = true)) {
                ivReport3.setImageResource(R.drawable.btn_schedule_overlap_on)
                tvReport3.setTextColor(ContextCompat.getColor(mContext, R.color.main))
            } else {
                ivReport3.setImageResource(R.drawable.btn_schedule_overlap_off)
                tvReport3.setTextColor(ContextCompat.getColor(mContext, R.color.gray300))
            }

            // 190406 맞아요/틀려요 갯수
            tvReport1.setText(item.num_yes.toString())
            tvReport2.setText(item.num_no.toString())
        }
    }
}

