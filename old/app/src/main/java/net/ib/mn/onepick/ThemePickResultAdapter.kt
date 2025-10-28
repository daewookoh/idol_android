package net.ib.mn.onepick

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import kotlinx.coroutines.CoroutineScope
import net.ib.mn.R
import net.ib.mn.databinding.ItemThemePickRank1stBinding
import net.ib.mn.databinding.ItemThemePickRankBinding
import net.ib.mn.databinding.ItemThemePickRankHeaderBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.onepick.viewholder.themepick.ThemePick1stViewHolder
import net.ib.mn.onepick.viewholder.themepick.ThemePickHeaderViewHolder
import net.ib.mn.onepick.viewholder.themepick.ThemePickRankViewHolder
import kotlin.collections.ArrayList

class ThemePickResultAdapter(
    private val context: Context,
    private val themepickRankModel: ArrayList<ThemepickRankModel>,
    private val glideRequestManager: RequestManager,
    private val coroutineScope: CoroutineScope,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
    private var mTheme: ThemepickModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> {
                TYPE_HEADER
            }
            1 -> {
                TYPE_FIRSTPLACE
            }
            else -> {
                TYPE_REST
            }
        }
    }

    fun setTheme(theme: ThemepickModel) {
        mTheme = theme
    }

    override fun getItemCount(): Int = themepickRankModel.size + 1 //헤더뷰 때문에 +1 해주기.

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> { // 헤더뷰.
                val themePickHeader : ItemThemePickRankHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_theme_pick_rank_header,
                    parent,
                    false
                )
                ThemePickHeaderViewHolder(themePickHeader, mTheme, glideRequestManager)
            }
            TYPE_FIRSTPLACE -> { // 1등일때.
                val theme1stRanking :ItemThemePickRank1stBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_theme_pick_rank_1st,
                    parent,
                    false
                )
                ThemePick1stViewHolder(theme1stRanking, glideRequestManager, coroutineScope, getIdolByIdUseCase)
            }
            else -> {
                val themeRestRanking : ItemThemePickRankBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_theme_pick_rank,
                    parent,
                    false
                )
                ThemePickRankViewHolder(themeRestRanking, mTheme, glideRequestManager, themepickRankModel, coroutineScope, getIdolByIdUseCase)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (themepickRankModel.isEmpty()) return

        if(position==0){ //헤더뷰.
                (holder as ThemePickHeaderViewHolder).bind(themepickRankModel[position], position)
        }else{
            if(holder.itemViewType == TYPE_FIRSTPLACE){ // 1등.
                (holder as ThemePick1stViewHolder).bind(
                    themepickRankModel[position - 1],
                    mTheme,
                    position,
                )
            }else{ //나머지 등수.
                (holder as ThemePickRankViewHolder).bind(
                    themepickRankModel[position - 1],
                    mTheme,
                    position,
                )
            }
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_FIRSTPLACE = 1
        const val TYPE_REST = 2
    }
}