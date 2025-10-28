package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.model.IdolTypeModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.utils.Logger
import net.ib.mn.utils.UtilK

class IdolShowCheckAdapter(
    private val typeListModel : ArrayList<TypeListModel>,
    private val idolTypeModel : ArrayList<IdolTypeModel>,
    private val categoryClick: (String?, View, TypeListModel, Int) -> Unit
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnItemClickListener{
        fun categoryOnOff(category : String?, view : View, typeListModel: TypeListModel, position :Int)  // 남녀 나누는 것 위해 male 넣음. 위아래 체크박스 구분해줘야되기 때문
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val itemShowCheckOne = LayoutInflater.from(parent.context).inflate(R.layout.item_show_check_one, parent, false)

        return ShowCheckOneViewHolder(itemShowCheckOne)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        (holder as ShowCheckOneViewHolder).apply {
            this.bind(position, typeListModel)
            btnPushCategory.setOnClickListener { v ->
                categoryClick(category, v, typeListModel[position], position) //category -> M/F,
            }
        }
    }

    override fun getItemCount(): Int {
        return typeListModel.size   //TopBanner 때문에 + 1
    }

    //남녀 안나눈 클래스
    inner class ShowCheckOneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var tvCelebCategory : AppCompatTextView = itemView.findViewById(R.id.tv_celeb_category)
        private var dividedView : View = itemView.findViewById(R.id.divided_view)
        var btnPushCategory : CheckBox = itemView.findViewById(R.id.btn_push_category)
        var category:String? = "M"
        fun bind(position: Int, typeListModel: ArrayList<TypeListModel>){
            val model = typeListModel[position]

            if(model.isDivided == "N" && !model.isFemale) {
                tvCelebCategory.text = model.name
                Logger.v("TYPECHECK:: ${model.type}")
            }
            //남녀 나눈 것 중 남자
            else if(model.isDivided == "Y" && !model.isFemale){
                if(model.type=="S"){
                    tvCelebCategory.text = itemView.context.getString(R.string.actor_male_singer)
                }
                else if(model.type=="A"){
                    tvCelebCategory.text = itemView.context.getString(R.string.lable_actors)
                }
            }
            //남녀 나눈 것 중 여자
            else{
                if(model.type=="S"){
                    tvCelebCategory.text = itemView.context.getString(R.string.actor_female_singer)
                }
                else if(model.type=="A"){
                    tvCelebCategory.text = itemView.context.getString(R.string.lable_actresses)
                }
            }

            category = UtilK.getTypeCheck(model.isDivided, model.isFemale)

            btnPushCategory.isChecked = idolTypeModel[position].checkFilter

            if(model.showDivider) {
                dividedView.visibility = View.VISIBLE
            } else {
                dividedView.visibility = View.GONE
            }
        }
    }
}