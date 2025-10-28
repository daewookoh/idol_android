package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.UserBlockItemBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.UserBlockModel
import net.ib.mn.model.toPresentation
import net.ib.mn.utils.Util

class UserBlockAdapter(
    private val coroutineScope: CoroutineScope,
    private val mGlideRequestManager: RequestManager,
    private val getIdolByIdUseCase: GetIdolByIdUseCase
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var userBlockArray: ArrayList<UserBlockModel> = ArrayList()
    private var onItemClickListener: OnItemClickListener? = null


    interface OnItemClickListener{
        fun onUpdateUserBlockStatus(userId:Int,blockStatus:String,position: Int)
        fun feedActivityStart(email:String, userId : Int, position: Int)
    }

    //처음 차단 유저 리스트 불러오는 함수
    fun setBlockArray(userBlockArray: ArrayList<UserBlockModel>){
        this.userBlockArray.clear()
        this.userBlockArray = userBlockArray
        notifyDataSetChanged()
    }

    //차단, 차단해제 눌렀을 때
    fun updateUserBlockStatus(position: Int,userBlockModel: UserBlockModel?){
        if (userBlockModel != null) {
            this.userBlockArray[position] = userBlockModel
        }
        notifyItemChanged(position)
    }


    //외부에서 아이템 클릭 처리할 리스너
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = UserBlockItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserBlockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as UserBlockViewHolder).apply {
            this.bind(userBlockArray[position])
            binding.btnUserBlock.setOnClickListener {
              val userBlockModel = userBlockArray[position]
              onItemClickListener?.onUpdateUserBlockStatus(userBlockModel.id,changeBlockStatus(userBlockModel),position)
            }
            binding.clUserEvery.setOnClickListener{
                val userBlockModel = userBlockArray[position]
                if(userBlockModel.nickname.isNotEmpty()) { //닉네임이 비어있으면 탙퇴한 유저. 탈퇴한유저 피드 못들어가게 막음
                    onItemClickListener?.feedActivityStart(userBlockModel.email, userBlockModel.id, position)
                }
            }

        }
    }

    //유저 차단, 차단해제에 따른 모델 안 isBlocked 값 변경
    private fun changeBlockStatus(userBlockModel: UserBlockModel):String{
        return if(userBlockModel.isBlocked == "Y"){
            "N"
        }else{
            "Y"
        }
    }

    override fun getItemCount() = userBlockArray.size

    inner class UserBlockViewHolder(val binding: UserBlockItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(userBlockModel: UserBlockModel) = with(binding) {

            val options = RequestOptions()
                    .circleCrop()
                    .error(Util.noProfileImage(userBlockModel.id))
                    .fallback(Util.noProfileImage(userBlockModel.id))
                    .placeholder(Util.noProfileImage(userBlockModel.id))

            mGlideRequestManager.load(userBlockModel.imageUrl).apply(options).into(ivUserImg)
            ivUserLevel.setImageResource(Util.getLevelResId(itemView.context, userBlockModel.level))
            tvUserName.text = userBlockModel.nickname

            coroutineScope.launch(Dispatchers.IO) {
                val idol = getIdolByIdUseCase(userBlockModel.mostId)
                    .mapDataResource { it?.toPresentation() }
                    .awaitOrThrow()
                idol?.let {
                    withContext(Dispatchers.Main) {
                        if (idol.getName(itemView.context)?.contains("_") == true) {

                            tvFavoriteName.visibility = View.VISIBLE
                            tvFavoriteGroup.visibility = View.VISIBLE
                            tvFavoriteName.text =
                                "${itemView.context.getString(if(BuildConfig.CELEB) R.string.actor_most_favorite else R.string.most_favorite)} : ${
                                    Util.nameSplit(itemView.context, idol)[0]
                                }_"
                            tvFavoriteGroup.text = Util.nameSplit(itemView.context, idol)[1]
                        } else {
                            tvFavoriteName.visibility = View.VISIBLE
                            tvFavoriteGroup.visibility = View.INVISIBLE
                            tvFavoriteName.text =
                                "${itemView.context.getString(if(BuildConfig.CELEB) R.string.actor_most_favorite else R.string.most_favorite)} : ${
                                    idol?.getName(itemView.context) ?: itemView.context.getString(R.string.none)
                                }"
                        }
                        if (idol?.getId() == 0) {
                            tvFavoriteEmpty.visibility = View.VISIBLE
                        }
                    }
                }
            }

            if(userBlockModel.isBlocked == "Y"){
                btnUserBlock.text = itemView.context.getString(R.string.unblock)
                btnUserBlock.setTextColor(ContextCompat.getColor(itemView.context, R.color.main))
                btnUserBlock.background = ContextCompat.getDrawable(itemView.context,if(BuildConfig.CELEB) R.drawable.bg_btn_round_main_celeb else R.drawable.bg_btn_round_brand500)
            }else{
                btnUserBlock.text = itemView.context.getString(R.string.block)
                btnUserBlock.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_dimmed))
                btnUserBlock.background = ContextCompat.getDrawable(itemView.context,R.drawable.bg_btn_round_text_dimmed)
            }
        }
    }

}