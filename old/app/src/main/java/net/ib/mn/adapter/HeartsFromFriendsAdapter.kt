package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.HeartsFromFriendsItemBinding
import net.ib.mn.model.HeartsFriendsModel
import net.ib.mn.utils.Util

class HeartsFromFriendsAdapter(context: Context) :
    ArrayAdapter<HeartsFriendsModel?>(context, R.layout.hearts_from_friends_item) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: HeartsFromFriendsItemBinding

        if(convertView == null) {
            binding = HeartsFromFriendsItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as HeartsFromFriendsItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: HeartsFriendsModel?, position: Int) = with(view?.tag as HeartsFromFriendsItemBinding) {
        val view = view ?: return
        val item = item ?: return

        friendName.text = item.txt
        friendHeart.text = item.heart.toString()
        ivLevel.setImageDrawable(
            Util.getLevelImageDrawable(
                context,
                item.level
            )
        )
    }
}
