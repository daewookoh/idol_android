package net.ib.mn.feature.halloffame

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.databinding.HallTopItemBinding
import net.ib.mn.model.HallTopModel

class HallTopAdapter(
    private var items: ArrayList<HallTopModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = HallTopItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HallTopViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as HallTopViewHolder).bind(items[position])
    }

    fun setItems(newItems: ArrayList<HallTopModel>) {
        items.apply {
            clear()
            addAll(newItems)
        }
        notifyDataSetChanged()
    }
}