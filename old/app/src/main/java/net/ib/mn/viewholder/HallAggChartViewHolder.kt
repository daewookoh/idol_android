/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 한 아이돌의 명예전당 누적순위 변화 차트 보여주는 ViewHolder
 *
 * */

package net.ib.mn.viewholder

import android.content.Context
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import net.ib.mn.R
import net.ib.mn.databinding.HallAggChartItemBinding
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.utils.LocaleUtil
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

class HallAggChartViewHolder(
    val binding: HallAggChartItemBinding,
    val mContext: Context,
    private val averageHeart : Long
) : RecyclerView.ViewHolder(binding.root) {

    private var top = 999
    private var bottom = 1

    fun bind(itemsList: MutableList<HallAggHistoryModel>) {

        assert(!itemsList.isNullOrEmpty())  // 직접 넣은 데이터가 한개 있기 때문

        //itemsList의 첫번째 인덱스에는 가짜 데이터가 들어가있으므로 제거 후 사용
        val items = mutableListOf<HallAggHistoryModel>()
        items.addAll(itemsList)

        items.remove(items[0])

        //차트는 역순으로 쌓아줘야함
        items.reverse()

        val voteCountComma =
            NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(averageHeart)
        val voteCountText = String.format(mContext.resources.getString(R.string.vote_count_format), voteCountComma)

        //퍙균값 text 적용
        binding.tvVoteAverage.text = voteCountText
        val dayCount = String.format(mContext.resources.getString(R.string.rank_history_30_average), items.size.toString())

        //날짜  데이터 수로  평균일  title  날짜수값  지정
        binding.tvVoteAverageTitle.text = dayCount

        divideChart(items)
    }

    //필요한 차트 개수에 맞춰 items를 나눠 drawChart에 값 넘기는 함수
    private fun divideChart(items: List<HallAggHistoryModel>) {
        if (items.isEmpty()) {
            binding.chart.setNoDataText("")
            binding.chart.invalidate()
            return
        }

        val itemsList = ArrayList<ArrayList<HallAggHistoryModel>>()
        var itemsChart = ArrayList<HallAggHistoryModel>()

        for(index in items.indices){
            if(index == 0) {
                itemsChart.add(items[index])
            }
            else{
                if(items[index-1].league == items[index].league){
                    itemsChart.add(items[index])
                }else{
                    itemsList.add(itemsChart)
                    itemsChart = ArrayList()
                    itemsChart.add(items[index])
                }
            }

            //마지막까지 봤을 때
            if(index == items.size-1){
                itemsList.add(itemsChart)
            }
        }

        drawChart(itemsList, items)
    }

    //나누어진 차트 리스트들값에 따라 차트를 그리는 함수 (itemsList : 연속된 S A 리그값들을 다중 ArrayList에 저장, items : 최근 30일 아이템 전부 가지고 있는 ArrayList)
    private fun drawChart(itemsList: ArrayList<ArrayList<HallAggHistoryModel>>, items: List<HallAggHistoryModel>){
        val description = Description()
        description.text = ""

        with(binding.chart){
            //첫번째 차트
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 8f
            xAxis.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            this.description = description
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            setDrawGridBackground(false)
            legend.isEnabled = false
            // y축 숨기기
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(false)
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            isDragEnabled = true
        }

        // 맨 위는 최고 순위, 맨 아래는 최저 순위
        for (i in items.indices) {
            val model = items[i]
            if (model.rank < top) top = model.rank
            if (model.rank > bottom) bottom = model.rank
        }

        binding.chart.axisLeft.axisMinimum = (top - 2).toFloat()
        binding.chart.axisLeft.axisMaximum = (bottom + 2).toFloat()

        // x축 값 표시 제거
        binding.chart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return ""
            }
        }

        val entryList = ArrayList<ArrayList<Entry>>()
        var entry = ArrayList<Entry>()

        for(i in 0 until itemsList.size){      //차트 개수만큼 반복문
            var xPosition = 0  //x좌표값을 더해야 할 때 사용하는 변수(두,세번째 차트일 경우, 앞전의 x좌표만큼 더해줘야할 때 사용)
            if(i>0){    //두번째 차트부터 앞전의 x좌표를 더해줘야하므로 i>0일 때 실행
                for(idx in 0 until i){
                    xPosition += itemsList[idx].size
                }
            }
            for(index in 0 until itemsList[i].size){    //A S 분리된 차트내에서 반복문
                val rank = itemsList[i][index].rank
                val rankCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(rank.toLong())
                entry.add(Entry(index.toFloat() + xPosition, (bottom - rank + top).toFloat(), if (rank == 999) "-" else rankCount))

                if(index == itemsList[i].size-1){
                    entryList.add(entry)
                    entry = ArrayList()
                }
            }
        }

        val setList = ArrayList<LineDataSet>()
        val dataSets = ArrayList<ILineDataSet>()

        for(i in 0 until entryList.size){   //entryList 개수만큼 그래프 디자인 set
            val set = LineDataSet(entryList[i], "")

            with(set){
                setDrawFilled(true)
                if(itemsList[i].any { it.league == "A" }){
                    fillDrawable = ContextCompat.getDrawable(mContext, R.drawable.a_league_gradient_chart)
                    color = ContextCompat.getColor(mContext, R.color.s_league_progress)
                    setCircleColor(ContextCompat.getColor(mContext, R.color.s_league_progress))
                }
                else{
                    fillDrawable = ContextCompat.getDrawable(mContext, R.drawable.s_league_gradient_chart)
                    color = ContextCompat.getColor(mContext, R.color.main)
                    setCircleColor(ContextCompat.getColor(mContext, R.color.main))
                }
                lineWidth = 1f
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = ContextCompat.getColor(mContext, R.color.text_dimmed)
                //        set.setHighlightEnabled(false);
                setDrawCircleHole(false)
                circleRadius = 3f
            }
            setList.add(set)
            dataSets.add(setList[i])
        }

        // create a data object with the datasets
        val data = LineData(dataSets)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getPointLabel(entry: Entry): String {
                return entry.data as String
            }
        })

        with(binding.chart){
            this.data = data
            notifyDataSetChanged()
            invalidate()
        }
    }
}