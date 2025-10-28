package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.ib.mn.R
import net.ib.mn.databinding.DayItemBinding
import net.ib.mn.utils.Util
import java.util.Calendar

//public class DayAdapter extends ArrayAdapter<ScheduleModel> {
//    private static final int LAYOUT_ID = R.layout.day_item;
//    private ImageLoader mImageLoader;
//    private Context context;
//    private Calendar mCal;
//
//    public DayAdapter(Context context, ImageLoader imageLoader) {
//        super(context, LAYOUT_ID);
//        this.context = context;
//        mImageLoader = imageLoader;
//    }
//
//    @Override
//    public void notifyDataSetChanged() {
//        super.notifyDataSetChanged();
//    }
//
//
//    protected void update(View view, ScheduleModel item, int position) {
//        TextView tvItemGridView = view.findViewById(R.id.tv_item_gridview);
//        ImageView ivItemGridView = view.findViewById(R.id.iv_item_gridview);
//
//        tvItemGridView.setText("" + getItem(position));
//
//        //해당 날짜 텍스트 컬러,배경 변경
//        mCal = Calendar.getInstance();
//        //오늘 day 가져옴
//        Integer today = mCal.get(Calendar.DAY_OF_MONTH);
//        String sToday = String.valueOf(today);
//        if (sToday.equals(getItem(position))) { //오늘 day 텍스트 컬러 변경
//            tvItemGridView.setTextColor(Color.parseColor("#000000"));
//        }
//    }
//}
class DayAdapter(
    private val mContext: Context,
    private val list: MutableList<String>,
    private val dayIconList: HashMap<*, *>,
    private var year: Int,
    private var month: Int,
    private var nowDay: Int,
    private val mListener: OnDayClickListener
) : BaseAdapter() {
    private val inflater: LayoutInflater
    private var prevView: View? = null
    private var prevDay = 0

    interface OnDayClickListener {
        fun onClick(position: Int)
    }

    init {
        this.inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): String? {
        return list.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setYear(year: Int) {
        this.year = year
    }

    fun setMonth(month: Int) {
        this.month = month
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: DayItemBinding

        if (convertView == null) {
            binding = DayItemBinding.inflate(inflater, parent, false)
            binding.root.tag = binding
        } else {
            binding = convertView.tag as DayItemBinding
        }
        binding.tvItemGridview.setText("" + getItem(position))
        if (!getItem(position)!!.isEmpty()) {
            if (getItem(position) == nowDay.toString()) {
                if (prevView != null) {
                    val tv = prevView!!.findViewById<TextView>(R.id.tv_item_gridview)
                    tv.setTextColor(getDayColor(prevDay))
                    prevView!!.setBackgroundResource(R.drawable.gray_border_bottom)
                }
                prevView = binding.root
                prevDay = getItem(position)!!.toInt()
                binding.tvItemGridview.setTextColor(
                    ContextCompat.getColor(
                        mContext,
                        R.color.text_white_black
                    )
                )
                prevView!!.setBackgroundColor(ContextCompat.getColor(mContext, R.color.main))
            } else {
                binding.tvItemGridview.setTextColor(getDayColor(getItem(position)!!.toInt()))
                binding.root.setBackgroundResource(R.drawable.gray_border_bottom)
            }
        }
        if (!getItem(position)!!.isEmpty() && dayIconList.get(getItem(position)!!.toInt()) != null) {
            binding.ivItemGridview.setImageResource(
                Util.getScheduleIcon(
                    dayIconList.get(
                        getItem(
                            position
                        )!!.toInt()
                    ).toString()
                )
            )
        } else binding.ivItemGridview.setImageBitmap(null)

        binding.root.setOnClickListener(View.OnClickListener { v: View? ->
            if (getCount() < position || getItem(position)!!.isEmpty()) return@OnClickListener
            else mListener.onClick(getItem(position)!!.toInt())
            if (prevView !== v) {
                var tv: TextView
                if (prevView != null) {
                    tv = prevView!!.findViewById<TextView>(R.id.tv_item_gridview)
                    Util.log("day >>>>>>>> " + getItem(position))
                    tv.setTextColor(getDayColor(prevDay))
                    prevView!!.setBackgroundResource(R.drawable.gray_border_bottom)
                }
                tv = v!!.findViewById<TextView>(R.id.tv_item_gridview)
                tv.setTextColor(ContextCompat.getColor(mContext, R.color.text_white_black))
                v.setBackgroundColor(ContextCompat.getColor(mContext, R.color.main))
                prevView = v
                prevDay = getItem(position)!!.toInt()
            }
        })
        return binding.root
    }

    private fun getDayColor(day: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        val week = cal.get(Calendar.DAY_OF_WEEK) - 1
        if (week == 0) return ContextCompat.getColor(mContext, R.color.main)
        else if (week == 6) return ContextCompat.getColor(mContext, R.color.gray300)
        else return ContextCompat.getColor(mContext, R.color.gray580)
    }

    fun nextMonthInit() {
        if (prevView != null) {
            val tv = prevView!!.findViewById<TextView>(R.id.tv_item_gridview)
            tv.setTextColor(getDayColor(tv.getText().toString().toInt()))
            prevView!!.setBackgroundResource(R.drawable.gray_border_bottom)
            prevView = null
        }
    }

    fun setNowDay(nowDay: Int) {
        this.nowDay = nowDay
    }

}
