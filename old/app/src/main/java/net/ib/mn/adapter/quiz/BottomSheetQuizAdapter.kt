/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 퀴즈 화면 정답 , 난이도 고르는 바텀 다이얼로그 어댑터입니다.
 *
 * */

package net.ib.mn.adapter.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.QuizAnswerItemBinding


/**
 * @see
 * */

class BottomSheetQuizAdapter(
    private val answerList: List<Int> = listOf(),
    private val difficultyList: List<String> = listOf(),
    private val type: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onAnswerItemClickListener: OnAnswerItemClickListener? = null
    private var onDifficultyClickListener: OnDifficultyClickListener? = null

    interface OnAnswerItemClickListener {
        fun onAnswerItemClick(position: Int)
    }

    interface OnDifficultyClickListener {
        fun onDifficultyItemClick(difficulty: String, position: Int)
    }

    fun setAnswerItemClickListener(itemClickListener: OnAnswerItemClickListener) {
        this.onAnswerItemClickListener = itemClickListener
    }

    fun setDifficultyItemClickListener(itemClickListener: OnDifficultyClickListener) {
        this.onDifficultyClickListener = itemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: QuizAnswerItemBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.quiz_answer_item,
                parent,
                false
            )

        return BottomSheetQuizVH(binding).apply {
            this.binding.tvContent.setOnClickListener {
                if (type == BOTTOM_SHEET_DIFFICULTY) {
                    onDifficultyClickListener?.onDifficultyItemClick(
                        difficultyList[bindingAdapterPosition],
                        bindingAdapterPosition
                    )
                } else {
                    onAnswerItemClickListener?.onAnswerItemClick(bindingAdapterPosition)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as BottomSheetQuizVH).apply {
            if (type == BOTTOM_SHEET_DIFFICULTY) {
                this.bind(difficultyList[position])
            } else {
                this.bind(answerList[position].toString())
            }

        }
    }

    override fun getItemCount(): Int {
        return if (type == BOTTOM_SHEET_DIFFICULTY) {
            difficultyList.size
        } else {
            answerList.size
        }
    }

    inner class BottomSheetQuizVH(
        val binding: QuizAnswerItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.tvContent.text = title
        }
    }

    companion object {
        const val BOTTOM_SHEET_ANSWER = "answer"
        const val BOTTOM_SHEET_DIFFICULTY = "difficulty"
    }


}