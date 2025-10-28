package net.ib.mn.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import net.ib.mn.R
import net.ib.mn.activity.MyCouponActivity
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.core.model.CouponModel
import net.ib.mn.databinding.ItemCouponBinding
import net.ib.mn.databinding.TextureGalleryItemBinding
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import java.text.DateFormat

class MyCouponAdapter(
    mContext: Context,
    private val mListener: OnCouponClickListener
) : ArrayAdapter<CouponModel?>(
    mContext, LAYOUT_ID
) {
    interface OnCouponClickListener {
        fun onCouponClick(v: View, coupon: CouponModel, position: Int)
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: ItemCouponBinding

        if(convertView == null) {
            binding = ItemCouponBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as ItemCouponBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: CouponModel?, position: Int): Unit = with(view?.tag as ItemCouponBinding) {
        val view = view ?: return
        val item = item ?: return

        if (item.value == null || item.value!!.isEmpty()) {
            coupon.setVisibility(View.VISIBLE)
            coupon2.setVisibility(View.GONE)
            couponTitle.setText(item.title)
            couponDescription.setText(item.message)

            val f = DateFormat.getDateInstance(DateFormat.LONG, getAppLocale(context))
            val dateString = f.format(item.expiredAt)
            couponDate.setText(context.getString(R.string.expire_coupon) + ": " + dateString)
            Util.log("id > " + item.id + " getUsed_at > " + item.expiredAt + " position > " + position)
            if (item.expiredAt == null) {
                couponTitle.setTextColor(ContextCompat.getColor(context, R.color.main_light))
                couponDescription.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponDate.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponUse.setText(context.getString(R.string.use_coupon))
                coupon.setBackgroundResource(R.drawable.coupon_bg_on)
                llCouponUse.setOnClickListener(View.OnClickListener { v ->
                    mListener.onCouponClick(
                        v,
                        item,
                        position
                    )
                })
            } else {
                couponTitle.setTextColor(Color.GRAY)
                couponDescription.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponDate.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponUse.setText(context.getString(R.string.used_coupon))
                coupon.setBackgroundResource(R.drawable.coupon_bg_2)
                llCouponUse.setOnClickListener(null)
            }
        } else {
            coupon.setVisibility(View.GONE)
            coupon2.setVisibility(View.VISIBLE)

            couponTitle2.setText(item.title)
            couponDescription2.setText(item.message)
            couponNumber.setText(item.value)

            val f = DateFormat.getDateInstance(DateFormat.LONG, getAppLocale(context))
            val dateString = f.format(item.expiredAt)
            couponDate2.setText(context.getString(R.string.expire_coupon) + ": " + dateString)
            Util.log("id > " + item.id + " getUsed_at > " + item.expiredAt + " position > " + position)
            if (item.usedAt == null) {
                couponTitle2.setTextColor(ContextCompat.getColor(context, R.color.main_light))
                couponDescription2.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponDate2.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponUse2.setText(context.getString(R.string.use_coupon))
                //                coupon2.setBackgroundResource(R.drawable.coupon_bg_on);
                llCouponUse2.setOnClickListener(View.OnClickListener { v ->
                    mListener.onCouponClick(
                        v,
                        item,
                        position
                    )
                })
                couponNumberCp.setOnClickListener(View.OnClickListener { v ->
                    mListener.onCouponClick(
                        v,
                        item,
                        position
                    )
                })

                if (item.value == MyCouponActivity.COUPON_CHANGE_NICKNAME) {
                    couponTitle2.setTextColor(ContextCompat.getColor(context, R.color.main_yellow))
                    imageCoupon2Right.setColorFilter(
                        ContextCompat.getColor(
                            context,
                            R.color.main_yellow
                        )
                    )
                    couponNumber.setTextColor(ContextCompat.getColor(context, R.color.main_yellow))
                    couponNumberCp.setVisibility(View.GONE)
                } else {
                    couponTitle2.setTextColor(ContextCompat.getColor(context, R.color.main_light))
                    imageCoupon2Right.setColorFilter(ContextCompat.getColor(context, R.color.main_light))
                    couponNumber.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                }
            } else {
                couponTitle2.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponDescription2.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponDate2.setTextColor(ContextCompat.getColor(context, R.color.gray580))
                couponUse2.setText(context.getString(R.string.used_coupon))
                //                coupon2.setBackgroundResource(R.drawable.coupon_bg_off);
                imageCoupon2Right.setColorFilter(ContextCompat.getColor(context, R.color.gray300))
                llCouponUse2.setOnClickListener(null)
                couponNumberCp.setOnClickListener(null)
            }
        }
    }

    companion object {
        private val LAYOUT_ID = R.layout.item_coupon
    }
}
