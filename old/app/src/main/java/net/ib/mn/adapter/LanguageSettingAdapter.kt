package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.activity.LanguageSettingActivity
import net.ib.mn.databinding.LanguageSettingItemBinding
import net.ib.mn.utils.Const

class LanguageSettingAdapter(
        private val context: Context,
        private val langList: IntArray,
        private val onClickListener: LanguageSettingActivity
        ) : RecyclerView.Adapter<LanguageSettingAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LanguageSettingItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.langText.text = context.getString(langList[position])
        holder.binding.langText.setOnClickListener {
            onClickListener.onItemClicked(position)
        }
    }

    override fun getItemCount(): Int {
        return Const.languages.size
    }

    inner class ViewHolder(val binding: LanguageSettingItemBinding) : RecyclerView.ViewHolder(binding.root) {
      fun bind(lagRes:Int){
          binding.langText.text= context.getString(lagRes)
      }
    }
}