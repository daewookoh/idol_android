package net.ib.mn.viewholder

import android.os.Build
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.adapter.IdolQuizStatusAdapter
import net.ib.mn.databinding.QuizListItemBinding
import net.ib.mn.model.QuizModel

class IdolQuizStatusViewHolder(
    val binding: QuizListItemBinding,
    type : Int
) : RecyclerView.ViewHolder(binding.root)  {
    private val type = type
    fun bind(myQuizListModel: QuizModel) { with(binding) {
        //채택된 퀴즈일 경우
        if(type == IdolQuizStatusAdapter.TYPE_CONFIRM_QUIZ){
            if(myQuizListModel.rewarded == null) {  //보상 받지 못헀을 경우
                ivShowCheck.visibility = View.VISIBLE
                ivQuestion.setImageResource(R.drawable.icon_inquiry_qna_q)
                setTextColor(tvQuestion, R.color.text_default)
            }
            else{   //보상을 받았을 경우
                ivShowCheck.visibility = View.INVISIBLE
                ivQuestion.setImageResource(R.drawable.icon_quiz_q)
                setTextColor(tvQuestion, R.color.text_gray)
            }
        }
        //거절,신고,대기중인 퀴즈일 경우
        else{
            setTextColor(tvQuestion, R.color.text_gray)
        }
        tvQuestion.text = myQuizListModel.content
    }}

    fun setTextColor(text : AppCompatTextView ,color : Int){
        text.setTextColor(itemView.context.resources.getColor(color, itemView.context.theme))
    }
}