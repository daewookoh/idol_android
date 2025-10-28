package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.QuizListItemBinding
import net.ib.mn.model.QuizModel
import net.ib.mn.viewholder.IdolQuizStatusViewHolder

class IdolQuizStatusAdapter(
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClickListener: OnItemClickListener? = null
    private var myQuizListModel : ArrayList<QuizModel> = ArrayList()

    interface OnItemClickListener{
        fun quizMyList(status :String, userId : Int, position: Int)
    }


    //외부에서 아이템 클릭 처리할 리스너
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    //퀴즈 조회 받아옴
    fun quizInfoArray(quizInfoArray : ArrayList<QuizModel>){
        myQuizListModel = quizInfoArray
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: QuizListItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.quiz_list_item,
            parent,
            false)
        return when(viewType){
            TYPE_CONFIRM_QUIZ -> IdolQuizStatusViewHolder(binding, TYPE_CONFIRM_QUIZ)      //서버에서 값 정렬 해주지만, 텍스트 및 이미지 다르게 해야돼서 타입 나눠줌
            TYPE_REJECTED_QUIZ -> IdolQuizStatusViewHolder(binding, TYPE_REJECTED_QUIZ)
            else -> IdolQuizStatusViewHolder(binding, TYPE_WAITING_QUIZ)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as IdolQuizStatusViewHolder).apply {
            bind(myQuizListModel[position])
        }
    }

    override fun getItemCount(): Int {
        return myQuizListModel.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (myQuizListModel[position].isViewable) {
            "Y" -> {
                TYPE_CONFIRM_QUIZ
            }
            "P" -> {
                TYPE_WAITING_QUIZ
            }
            else -> {   //N
                TYPE_REJECTED_QUIZ
            }
        }
    }


    companion object{
        const val TYPE_CONFIRM_QUIZ = 0
        const val TYPE_REJECTED_QUIZ = 1
        const val TYPE_WAITING_QUIZ = 2
    }
}