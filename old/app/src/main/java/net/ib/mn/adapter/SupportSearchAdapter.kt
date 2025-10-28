package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ScrollView
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.ItemSupportSearchBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.UtilK.Companion.setName

class SupportSearchAdapter(
    context: Context,
    private val listener: OnClickListener,
    private val scrollView: ScrollView
) : ArrayAdapter<IdolModel>(context, R.layout.item_support_search) {
    interface OnClickListener {
        fun onItemClicked(item: IdolModel, view: View, position: Int)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ItemSupportSearchBinding

        if(convertView == null) {
            binding = ItemSupportSearchBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as ItemSupportSearchBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: IdolModel, position: Int): Unit = with(view?.tag as ItemSupportSearchBinding) {
        setName(context, item, supportSearchNameTv, supportSearchGroupTv)

        supportSearchLi.setOnClickListener(View.OnClickListener { v: View? ->
            listener.onItemClicked(
                item,
                view,
                position
            )
        })

        //리스트 눌렀을시 스크롤 될 수 있게 바깥 스크롤 고정.
        supportSearchLi.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            scrollView.requestDisallowInterceptTouchEvent(true)
            false
        })
    }
}
