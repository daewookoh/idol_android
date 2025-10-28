package net.ib.mn.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.ItemSupportAdPickBinding
import net.ib.mn.model.SupportAdType
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList


class SupportAdPickAdapter(
        private val mContext: Context,
        private val mGlideRequestManager: RequestManager,
        private val isForAdPick:Boolean,
        private var onClickListener: OnClickListener
) : RecyclerView.Adapter<SupportAdPickAdapter.ViewHolder>() {

    private var mItems: ArrayList<SupportAdTypeListModel> = arrayListOf()

    fun setItems(newList: List<SupportAdTypeListModel>) {
        mItems.clear()
        mItems.addAll(newList)
        notifyDataSetChanged()
    }

    interface OnClickListener {
        fun onItemClicked(item: SupportAdTypeListModel, view: View, position: Int)
    }

    override fun getItemCount(): Int = mItems.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val supportAdPickItem: ItemSupportAdPickBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_support_ad_pick,
            parent,
            false
        )
        return SupportAdPickViewHolder(supportAdPickItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val adPick = mItems[position]
        holder.bind(adPick, position)
    }

    inner class SupportAdPickViewHolder(private val binding: ItemSupportAdPickBinding) : ViewHolder(binding.root) {
        private val transparentDrawable: Drawable = ColorDrawable(Color.TRANSPARENT)

        @SuppressLint("SetTextI18n")
        override fun bind(item: SupportAdTypeListModel, position: Int) {

            //마지막  아이템에는 밑줄 없애줌.
            if(mItems.lastIndex == position){
                binding.viewBottomBorder.visibility= View.INVISIBLE
            }

            mGlideRequestManager.load(item.imageUrl)
                    .into(binding.supportAdPickPhoto)

            val format = NumberFormat.getNumberInstance(Locale.US)
            val commaNum= format.format(item.goal)

            val categoryText = when(item.category) {
                SupportAdType.KOREA.label -> {
                    binding.root.context.getString(R.string.adtype_icon_korean)
                }
                SupportAdType.MOBILE.label -> {
                    binding.root.context.getString(R.string.adtype_icon_mobile)
                }
                SupportAdType.FOREIGN.label -> {
                    binding.root.context.getString(R.string.adtype_icon_global)
                }
                else -> {
                    ""
                }
            }

            binding.supportAdPickCheckBox.text = "$categoryText ${item.name.replace("&#39;"," \' ")}"

            //ad pick 용일때
            if(isForAdPick){
                binding.llAdPickGoalDiamond.visibility =View.VISIBLE
                binding.supportAdPickCheckBox.visibility= View.VISIBLE
                binding.supportAdPickCondition.visibility= View.VISIBLE

                //ad 선택시에는  화살표 없앰
                binding.ivArrowGo.visibility = View.GONE

                binding.supportAdPickCondition.text = item.location.replace("&#39;"," \' ")
                binding.supportAdPickGoalDiamond.text = commaNum
                binding.supportAdPickCheckBox.isChecked = item.selected

            }else{//광고 종류 볼때

                //화살표 나오게
                binding.ivArrowGo.visibility = View.VISIBLE

                //체크 박스는 text만 보여주기 위해 transparent 적용
                binding.supportAdPickCheckBox.buttonDrawable = transparentDrawable

                //체크 박스 눌렀을때 ripple 효과  제거 위해  background  투명도 적용
                binding.supportAdPickCheckBox.background = transparentDrawable

                binding.llAdPickGoalDiamond.visibility= View.GONE
                binding.supportAdPickCondition.visibility= View.GONE
            }




            val listener = View.OnClickListener { view ->
                onClickListener.onItemClicked(item, view, position)
            }

            itemView.setOnClickListener(listener)

        }

    }


    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal abstract fun bind(item: SupportAdTypeListModel, position: Int)
    }

}