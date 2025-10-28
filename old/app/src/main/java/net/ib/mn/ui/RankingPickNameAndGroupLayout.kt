/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 랭킹픽 화면쪽 아이돌 이름, 그룹 전용 레이아웃이다
 *
 * */

package net.ib.mn.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.databinding.RankingPickNameAndGroupBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK

/**
 * @see setRecompositionNameAndGroup 아이돌 이름이 1줄이 넘어갈 경우 뷰가 잘리는 현상이 있어서 위치 재조정하는 함수.
 * @see setTitleTextSize 아이돌 이름,그룹 텍스트 사이즈 세팅.
 * @see setNameAndGroup 아이돌 이름, 그룹 세팅 ( 아이돌 모델 존재 할경우)
 * @see setNameAndGroupForNoIdol 아이돌 이름, 그룹 세팅 ( 아이돌 모델 존재 X)
 * */

class RankingPickNameAndGroupLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr,
) {

    private var binding: RankingPickNameAndGroupBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.ranking_pick_name_and_group,
        this,
        true,
    )

    private var recompose: Boolean = false

    init {

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RecomposeNameAndGroup,
            0,
            0,
        ).apply {
            try {
                recompose = getBoolean(R.styleable.RecomposeNameAndGroup_recompose, false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                recycle()
            }
        }
    }

    fun setTitleTextSize(titleSize: Float = 13f, subTitleSize: Float = 11f) = with(binding) {
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSize)
        tvGroup.setTextSize(TypedValue.COMPLEX_UNIT_SP, subTitleSize)
    }

    fun setNameAndGroup(idol: IdolModel?, maxLine: Int) = with(binding) {
        UtilK.setName(
            context,
            idol,
            tvName,
            tvGroup,
        )
        tvName.maxLines = maxLine
        setRecompositionNameAndGroup()
    }

    private fun toggleTextViewVisibility(textView: AppCompatTextView, text: String?) {
        textView.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    fun setNameAndGroupForNoIdol(idolName: String?, idolGroup: String?, nameMaxLine: Int, groupMaxLine : Int) =
        with(binding) {
            tvName.text = idolName
            tvGroup.text = idolGroup
            tvName.maxLines = nameMaxLine
            tvGroup.maxLines = groupMaxLine
            toggleTextViewVisibility(binding.tvName, idolName)
            toggleTextViewVisibility(binding.tvGroup, idolGroup)
            setRecompositionNameAndGroup()
        }

    private fun setRecompositionNameAndGroup() = binding.tvName.post {
        // 아이돌 이름이 1줄이 넘어가게 되면 배치가 이상해지므로 다시 제약을 연결해줍니다.
        if (!recompose) {
            return@post
        }

        val constraintSet = ConstraintSet().apply {
            clone(binding.clRankNameAndGroup)

            connect(
                binding.tvName.id,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
            )
            connect(
                binding.tvName.id,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
            )
            connect(
                binding.tvName.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
            )
            connect(
                binding.tvName.id,
                ConstraintSet.BOTTOM,
                binding.tvGroup.id,
                ConstraintSet.TOP,
            )

            connect(
                binding.tvGroup.id,
                ConstraintSet.START,
                binding.tvName.id,
                ConstraintSet.START,
                0,
            )
            connect(
                binding.tvGroup.id,
                ConstraintSet.TOP,
                binding.tvName.id,
                ConstraintSet.BOTTOM,
                Util.convertDpToPixel(context, 2f).toInt(),
            )
            connect(
                binding.tvGroup.id,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
            )
            connect(
                binding.tvGroup.id,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.BOTTOM,
            )
        }

        constraintSet.applyTo(binding.clRankNameAndGroup)
    }

    fun setRecompose(recompose : Boolean) {
        this.recompose = recompose
    }
}