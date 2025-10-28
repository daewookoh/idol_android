package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.SmallTalkLocaleItemBinding

class BottomSheetQuizRankingFilterAdapter(
    private val items: List<String>,
    private val itemClickListener: (Int) -> Unit
) : RecyclerView.Adapter<BottomSheetQuizRankingFilterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<SmallTalkLocaleItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.small_talk_locale_item,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: SmallTalkLocaleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvSmallTalkLanguage.setOnClickListener { // Set listener on the TextView
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    itemClickListener(bindingAdapterPosition)
                }
            }
        }

        fun bind(item: String) {
            binding.tvSmallTalkLanguage.text = item
        }
    }
}