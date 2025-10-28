package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.SmallTalkLocaleItemBinding
import net.ib.mn.utils.BoardLanguage
import net.ib.mn.utils.Util

class BottomSheetLanguageFilterAdapter(
    private val context: Context,
    private val langList: IntArray,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var languageClickListener : OnClickListener? = null

    interface OnClickListener {
        fun onItemClicked(locale: String?, langText: String)
    }

    fun languageClickListener(languageTagClickListener : OnClickListener){
        this.languageClickListener = languageTagClickListener

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<SmallTalkLocaleItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.small_talk_locale_item,
            parent,
            false,
        )

        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).apply {
            binding.tvSmallTalkLanguage.text = context.getString(langList[position])
            binding.tvSmallTalkLanguage.setOnClickListener {
                val locale = when (position) {
                    0 -> {  //모든 언어
                        null
                    }
                    1 -> {  //설정 언어
                        Util.getSystemLanguage(context)
                    }
                    else -> {   //선택 언어
                        BoardLanguage.entries[position - 2].code
                    }
                }
                languageClickListener?.onItemClicked(locale, binding.tvSmallTalkLanguage.text.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return BoardLanguage.entries.size
    }

    inner class ViewHolder(val binding: SmallTalkLocaleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lagRes:Int){
            binding.tvSmallTalkLanguage.text= context.getString(lagRes)
        }
    }

}