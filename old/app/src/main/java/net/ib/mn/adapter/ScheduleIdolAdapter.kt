package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.ScheduleWriteIdolItemBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Util

class ScheduleIdolAdapter : ArrayAdapter<IdolModel?> {
    interface OnIdolClickListener {
        fun onClick(idol: IdolModel)
    }

    private var mContext: Context
    private var mListener: OnIdolClickListener
    private var mIds: ArrayList<IdolModel>
    private var isNoIdolSelected = false // 애돌용

    constructor(
        context: Context,
        ids: ArrayList<IdolModel>,
        listener: OnIdolClickListener,
        isNoIdolSelected: Boolean
    ) : super(context, R.layout.schedule_write_idol_item) {
        this.mContext = context
        this.mListener = listener
        this.mIds = ids
        this.isNoIdolSelected = isNoIdolSelected
    }

    // celeb
    constructor(context: Context, ids: ArrayList<IdolModel>, listener: OnIdolClickListener) : super(
        context,
        R.layout.schedule_write_idol_item
    ) {
        this.mContext = context
        this.mListener = listener
        this.mIds = ids
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ScheduleWriteIdolItemBinding

        if(convertView == null) {
            binding = ScheduleWriteIdolItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as ScheduleWriteIdolItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: IdolModel?, position: Int) = with(view?.tag as ScheduleWriteIdolItemBinding) {
        val item = item ?: return

        //        item.setLocalizedName(mContext);
        idolTv.text = Util.nameSplit(mContext, item)[0]

        idol.setOnClickListener { v: View ->
            isNoIdolSelected = false //아이돌  선택되었으니까  false 값으로 바꿔줌.
            mListener.onClick(item)
            if (idolCheck.isChecked == true) {
                v.setBackgroundResource(R.drawable.schedule_write_bg_select1)
                idolTv.setTextColor(
                    ContextCompat.getColor(
                        mContext,
                        R.color.gray200
                    )
                )
            } else {
                v.setBackgroundResource(R.drawable.schedule_write_bg_select2)
                idolTv.setTextColor(
                    ContextCompat.getColor(
                        mContext,
                        R.color.gray580
                    )
                )
            }
            idolCheck.isChecked = !idolCheck.isChecked
        }

        for (i in mIds.indices) {
            //선택된 아이돌 뿌려줄때  isNoIdolSelected 가 true이면  아무 선택 안된것이므로 그냥 넘어감.

            if (item.getId() == mIds[i].getId() && !isNoIdolSelected) {
                idol.setBackgroundResource(R.drawable.schedule_write_bg_select2)
                idolTv.setTextColor(ContextCompat.getColor(mContext, R.color.gray580))
                idolCheck.isChecked = !idolCheck.isChecked
            }
        }
    }
}
