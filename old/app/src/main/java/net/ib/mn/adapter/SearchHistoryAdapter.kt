package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.SearchAutoItemBinding
import net.ib.mn.databinding.SearchHistoryItemBinding
import net.ib.mn.databinding.SearchHotTrendItemBinding
import net.ib.mn.model.SearchHistoryModel
import org.json.JSONArray

class SearchHistoryAdapter(
        private val context: Context,
        private val searchType: Int,
        private val onClickListener: OnClickListener,
        private val historyArray: ArrayList<SearchHistoryModel>?,
        private val jsonArray: JSONArray?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    interface OnClickListener{
        fun onItemClicked(position: Int, checkItem : Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TREND -> {
                val binding = SearchHotTrendItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TrendViewHolder(binding)
            }
            TYPE_HISTORY -> {
                val binding = SearchHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryViewHolder(binding)
            }
            TYPE_AUTO -> {
                val binding = SearchAutoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AutoViewHolder(binding)
            }
            else -> {
                val binding = SearchHotTrendItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TrendViewHolder(binding)
            }

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_TREND ->{
                if (jsonArray != null) {
                    (holder as TrendViewHolder).bind(position, jsonArray)
                }
            }
            TYPE_HISTORY ->{
                if (historyArray != null) {
                    (holder as HistoryViewHolder).bind(historyArray.size - 1 - position, historyArray)
                }
            }
            else -> {
                if(jsonArray !=null){
                    (holder as AutoViewHolder).bind(position, jsonArray)
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return when(searchType){
            TYPE_HISTORY ->{
                historyArray!!.size
            }
            else ->{
                jsonArray!!.length()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (searchType) {
            5 -> {
                TYPE_TREND
            }
            6 -> {
                TYPE_HISTORY
            }
            7 -> {
                TYPE_AUTO
            }
            else -> TYPE_TREND
        }
    }

    inner class TrendViewHolder(val binding: SearchHotTrendItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position : Int, jsonArray : JSONArray) = with(binding) {
            tvHotNum.text = (position+1).toString()
            tvHotTrend.text = jsonArray.getJSONObject(position).getString("text")
            clHotTrend.setOnClickListener{
                onClickListener.onItemClicked(position, CL_HOT_TREND)
            }
        }
    }

    inner class HistoryViewHolder(val binding: SearchHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position : Int, historyArray: ArrayList<SearchHistoryModel>) = with(binding) {
            tvMySearch.text = historyArray[position].search

            ibSearchX.setOnClickListener {
                onClickListener.onItemClicked(position, SEARCH_X)
            }
            tvMySearch.setOnClickListener{
                onClickListener.onItemClicked(position, MY_SEARCH)
            }
        }
    }

    inner class AutoViewHolder(val binding: SearchAutoItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(position : Int, jsonArray: JSONArray) = with(binding) {
            tvAutoComplete.text = jsonArray.getJSONObject(position).getString("text")
            clAutoComplete.setOnClickListener{
                onClickListener.onItemClicked(position, CL_AUTO_COMPLETE)
            }
        }
    }

    companion object{
        const val SEARCH_X = 1
        const val MY_SEARCH = 2
        const val CL_HOT_TREND = 3
        const val CL_AUTO_COMPLETE = 4

        const val TYPE_TREND = 5
        const val TYPE_HISTORY = 6
        const val TYPE_AUTO = 7
    }

}

