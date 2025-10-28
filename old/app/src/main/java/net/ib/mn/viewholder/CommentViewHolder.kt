package net.ib.mn.viewholder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ArrowKeyMovementMethod
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import net.ib.mn.utils.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.NewCommentAdapter.Companion.COPY_COMMENT
import net.ib.mn.adapter.NewCommentAdapter.Companion.DELETE_COMMENT
import net.ib.mn.adapter.NewCommentAdapter.Companion.MENTION_REGEX
import net.ib.mn.adapter.NewCommentAdapter.Companion.REPORT_COMMENT
import net.ib.mn.addon.IdolGson
import net.ib.mn.databinding.CommentItemBinding
import net.ib.mn.dialog.CommentRemoveDialogFragment
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.model.*
import net.ib.mn.utils.*
import net.ib.mn.utils.vote.TranslateUiHelper
import java.io.File
import java.text.DateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class CommentViewHolder(val binding: CommentItemBinding,
                        context: Context,
                        private val useTranslation: Boolean = false,
                        private val mArticle: ArticleModel?,
                        private val isCommentOnly: Boolean,
                        private val commentList: ArrayList<CommentModel>,
                        private val mGlideRequestManager: RequestManager
) : RecyclerView.ViewHolder(binding.root),View.OnCreateContextMenuListener {

    private val gson = IdolGson.getInstance()
    private val emoListType = object : TypeToken<ArrayList<EmoticonDetailModel>>() {}.type

    private val mContext: Context = context
    private var currentCommentItem: CommentModel? = null

    private val mMentionPattern = Pattern.compile(MENTION_REGEX)
    private val systemLanguage = Util.getSystemLanguage(context)

    fun bindEmpty() { with(binding) {
        commentContainer.visibility = View.GONE
        loading.visibility = View.GONE
        clEmptyView.visibility = View.VISIBLE
    }}

    fun bind(commentItem: CommentModel,
             position: Int,
             isNextCommentDataExist: Boolean,
             loadFailed: Boolean = false) { with(binding) {
        currentCommentItem = commentItem
        clEmptyView.visibility = View.GONE
        
        commentContainer.background = ResourcesCompat.getDrawable(
            mContext.resources,
            R.drawable.border_gray100,
            null
        )

        val getUserBlockList = Util.getPreference(mContext, Const.USER_BLOCK_LIST)

        //댓글 쓴 날짜
        val createdAt: Date = commentItem.createdAt
        val f = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT,
            LocaleUtil.getAppLocale(itemView.context)
        )
        val dateString = f.format(createdAt)
        createdTime.text = dateString
        //차단된 유저의 댓글일 경우
        if(getUserBlockList.contains(commentItem.user?.id.toString())){
            name.text = mContext.getString(R.string.user_blocked2)
            level.setImageDrawable(Util.getLevelImageDrawable(mContext, 0))
            photo.setImageResource(Util.noProfileImage(commentItem.user?.id ?: 0))
            tvCommentContent.text = ""
            tvCommentContent.visibility = View.VISIBLE
            ivImageContent.visibility = View.GONE
            name.setTextColor(ContextCompat.getColor(mContext,R.color.gray300))
        }
        else {
            //해줘야 이전 이미지 잔상이 안남음
            mGlideRequestManager.clear(ivImageContent)
            ivImageContent.setImageDrawable(null)
            loading.visibility = View.GONE
            clImage.visibility = View.GONE
            commentContainer.visibility = View.VISIBLE
            name.setTextColor(ContextCompat.getColor(mContext,R.color.text_gray))

            //댓글 모델의 이모티콘 아이디가 이모티콘 아님 값이랑 일치하면, 일반/움짤/이미지 댓글이다.
            if (commentItem.emoticonId == CommentModel.NO_EMOTICON_ID) {
                tvCommentContent.visibility = View.VISIBLE
                if(commentItem.contentAlt?.isImage == true || commentItem.contentAlt?.isUmjjal == true){
                    clImage.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background_100))
                    clImage.visibility = View.VISIBLE
                    ivImageContent.visibility = View.VISIBLE
                    if(commentItem.thumbnailUrl == null){
                        setImage(commentItem.imageUrl, 0, 0)
                    }
                    else{
                        setImage(commentItem.thumbnailUrl, commentItem.thumbHeight, commentItem.thumbWidth)
                    }
                    if(commentItem.contentAlt?.isUmjjal == true){
                        ivGif.visibility = View.VISIBLE
                    }
                    else{
                        ivGif.visibility = View.GONE
                    }

                    // content alt 값이 비어있지 않을때 ->  댓글 textview visible로 바꿔주고 alt 에 있는 content값을 넣어준다.
                    if (commentItem.contentAlt?.text?.isNotEmpty() == true) {
                        tvCommentContent.visibility = View.VISIBLE
                        tvCommentContent.text = commentItem.content
                    }
                    else {//그밖에는  이모티콘만 있는 경우라서  댓글 텍스트뷰 gone처리
                        tvCommentContent.visibility = View.GONE
                    }
                }
                else{
                    ivImageContent.visibility = View.GONE
                }

            } else {//이모티콘 아이디가 NO_EMOTICON_ID가 아니라면 이모티콘 댓글이다.
                ivGif.visibility = View.GONE
                clImage.background = ColorDrawable(Color.TRANSPARENT)   //이모티콘일 땐, 카드뷰의 백그라운드가 들어가면 안되어서 추가
                clImage.visibility = View.VISIBLE
                ivImageContent.layoutParams.width = Util.convertDpToPixel(itemView.context,136f).toInt()
                ivImageContent.layoutParams.height = Util.convertDpToPixel(itemView.context,136f).toInt()
                val emoAllInfoList = gson.fromJson<ArrayList<EmoticonDetailModel>>(
                    Util.getPreference(itemView.context,Const.EMOTICON_ALL_INFO), emoListType)

                if(!emoAllInfoList.isNullOrEmpty()){ //Exception 날수도 있으니 체크.
                    //이모티콘  id 랑  commentitem의  이모티콘 id 가 같고,  카테고리 이미지가 아닐때 -> filapth uri 로 변환
                    val uri = Uri.parse(emoAllInfoList.find { it.id == commentItem.emoticonId && !it.isSetCategoryImg }?.filePath + ".webp")

                    val file = File(uri.path)
                    if (file.exists()) { //파일이 있다면 로컬에있는거 보여줌.
                        mGlideRequestManager
                            .load(uri.path)
                            .transform(CenterCrop(), RoundedCorners(40))
                            .into(ivImageContent)
                    } else {//아니면 webp로 보여줍니다(상대방이 가지고있는 이모티콘이 없을떄).
                        val path = UtilK.fileImageUrl(mContext, commentItem.emoticonId)
                        mGlideRequestManager
                            .load(path)
                            .transform(CenterCrop(), RoundedCorners(40))
                            .into(ivImageContent)
                    }
                } else {
                    val path = UtilK.fileImageUrl(mContext, commentItem.emoticonId)
                    mGlideRequestManager
                        .load(path)
                        .transform(CenterCrop(), RoundedCorners(40))
                        .into(ivImageContent)
                }


                // content alt 값이 비어있지 않을때 ->  댓글 textview visible로 바꿔주고 alt 에 있는 content값을 넣어준다.
                if (commentItem.contentAlt?.text?.isNotEmpty() == true) {
                    tvCommentContent.visibility = View.VISIBLE
                    tvCommentContent.text = commentItem.contentAlt?.text
                } else {//그밖에는  이모티콘만 있는 경우라서  댓글 텍스트뷰 gone처리
                    tvCommentContent.visibility = View.GONE
                }
                ivImageContent.visibility = View.VISIBLE
            }


            val userId = commentItem.user?.id ?: 0
            //댓글다 유저 프로필 사진
            mGlideRequestManager
                .load(commentItem.user?.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(userId))
                .fallback(Util.noProfileImage(userId))
                .placeholder(Util.noProfileImage(userId))
                .into(photo)


            //레벨별  아이콘 적용 -> 레벨 숫자도 이미지에 같이 표기됨.
            level.setImageBitmap(Util.getLevelImage(mContext, commentItem.user))


            val content123: String = commentItem.content ?: ""
            val matcher: Matcher = mMentionPattern.matcher(content123)

            tvCommentContent.text = ""
            val e = Editable.Factory.getInstance().newEditable("")
            var lastMentionIdx = 0

            while (matcher.find()) {
                val startIdx = matcher.start()
                var endIdx = matcher.end()
                val prefix = content123.substring(lastMentionIdx, startIdx)
                e.append(prefix)
                lastMentionIdx = endIdx
                val id = matcher.group(1)
                var name = matcher.group(2)

                // name에 {} 가 들어있어서 매칭이 제대로 안되는 경우가 있음
                while (endIdx < content123.length - 1) {
                    val substr = content123.substring(endIdx, endIdx + 1)
                    if (substr == "}") {
                        name += "}"
                        endIdx++
                        lastMentionIdx++
                    } else {
                        break
                    }
                }
                val sb = SpannableStringBuilder()

                // 댓글 목록에서는 굳이 ImageSpan 적용할 필요가 없어서 변경. 메모리 문제가 많이 생김.
                sb.append(name)
                sb.setSpan(
                    StyleSpan(Typeface.BOLD), 0, sb.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.setSpan(
                    BackgroundColorSpan(ContextCompat.getColor(mContext, R.color.mention_bg)),
                    0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(mContext, R.color.mention_fg)),
                    0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                e.append(sb)
            }
            val postfix = content123.substring(lastMentionIdx, content123.length)
            e.append(postfix)

            //댓글 내용 -> 태그 처리도 같이
            tvCommentContent.text = e


            //댓글단 유저 이름.
            name.text = "\u200E" + commentItem.user?.nickname


            // 이모티콘
            if (commentItem.user?.emoticon != null
                && commentItem.user?.emoticon?.emojiUrl != null
            ) {
                emoticon.visibility = View.VISIBLE
                mGlideRequestManager
                    .load(commentItem.user?.emoticon?.emojiUrl)
                    .into(emoticon)
            } else {
                emoticon.visibility = View.GONE
                mGlideRequestManager.clear(emoticon)
            }



            //댓글 내용
            val content: String = commentItem?.content ?: ""

            //아이템  롱클릭시  해당 댓글 내용 복사됨.
            commentParent.setOnLongClickListener { true }
            itemView.setOnCreateContextMenuListener(this@CommentViewHolder)

            // 번역
            TranslateUiHelper.bindTranslateButton(
                context = itemView.context,
                view = viewTranslate,
                content = commentItem.content ?: "",
                systemLanguage = systemLanguage,
                nation = commentItem.nation,
                translateState = commentItem.translateState,
                isTranslatableCached = commentItem.isTranslatable,
                useTranslation = useTranslation,
            ).also { canTranslate ->
                if(commentItem.isTranslatable == null) {
                    commentItem.isTranslatable = canTranslate
                }
                viewTranslate.setOnClickListener {
                    (mContext as? CommentTranslation)?.translateComment(commentItem, position)
                }
            }
        }

        // 컨텍스트 메뉴 닫히고 notifyDatasetChanged()가 불리면 링크 동작 다시 재설정
        tvCommentContent.movementMethod = LinkMovementMethod.getInstance()

        if (commentList.isNotEmpty() && isNextCommentDataExist && commentItem.resourceUri == commentList.last().resourceUri) {
            loading.visibility = View.VISIBLE
            if( loadFailed ) {
                showRefresh()
            } else {
                showLoading()
            }
        } else {
            loading.visibility = View.GONE
        }
    }}

    fun showLoading() {
        with(binding) {
            progress.visibility = View.VISIBLE
            clRefresh.visibility = View.GONE
        }
    }

    fun showRefresh() {
        with(binding) {
            progress.visibility = View.GONE
            clRefresh.visibility = View.VISIBLE
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) { with(binding) {
        // 컨텍스트 메뉴가 열리면 링크가 동작하지 않도록 설정
        tvCommentContent.movementMethod = ArrowKeyMovementMethod.getInstance()

        var showDelete = false
        var showReport = true
        
        // commentList가 비어있으면 현재 바인딩된 댓글 정보를 사용
        val model: CommentModel = if (commentList.isEmpty()) {
            currentCommentItem ?: return
        } else {
            if(isCommentOnly) commentList[adapterPosition] else commentList[adapterPosition - 1]
        }
        val mAccount = IdolAccount.getAccount(mContext)
        val articleModel:ArticleModel? = mArticle


        //댓글 삭제가 안되는 라인 사용자를 위해 이메일 검사대신 id검사로 변경
        if (model.user != null
            && mAccount != null
            && mAccount.userModel != null
            && model.user?.id == mAccount.userModel?.id
            || mAccount?.heart == Const.LEVEL_ADMIN
        ) {
            showDelete = true
        }

        // 게시글 작성자에게 삭제권한 부여 -> 크래시 방지
        if ( articleModel?.user != null
            && mAccount != null
            && mAccount.userModel != null
            && articleModel.user.id == mAccount?.userModel?.id
        ) {
            showDelete = true//내가 쓴글에서도  삭제 버튼을 보여준다.
        }


        //내가쓴  댓글 및   관리자 계정 (heart 30)은  신고 버튼 안나오게
        if (model.user != null && IdolAccount.getAccount(mContext) != null
            && IdolAccount.getAccount(mContext)?.userModel != null
            && model.user?.id == IdolAccount.getAccount(mContext)?.userModel?.id
            || model.user?.heart == Const.LEVEL_MANAGER
        ) {
            showReport = false//내가 쓴글의 경우는  신고 버튼을 따로 안보여줌.
        }

        val copy = menu?.add(Menu.NONE, 1001, 1, android.R.string.copy)
        val report = menu?.add(Menu.NONE, 1003, 2, R.string.report)
        val delete = menu?.add(Menu.NONE, 1002, 3, R.string.title_remove)

        if (!showDelete) {//삭제버튼 보여주기 false이면  안보여줌. 메뉴에서 삭제버튼  제거.
            menu?.removeItem(1002)
        }

        //이모티콘만 있는 댓글일 경우  복사 버튼 안보여줌.
        if(model.emoticonId != CommentModel.NO_EMOTICON_ID && model.contentAlt?.text?.isNotEmpty() != true){
            menu?.removeItem(1001)
        }


        if(!showReport){//신고 버튼 보여주기 false이면  안보여줌. 메뉴에서 신고버튼 제거
            menu?.removeItem(1003)
        }

        report?.setOnMenuItemClickListener(onEditMenu)
        delete?.setOnMenuItemClickListener(onEditMenu)
        copy?.setOnMenuItemClickListener(onEditMenu)
    }}

    //컨텍스트 메뉴에서 항목 클릭시 동작을 설정합니다.
    private val onEditMenu = MenuItem.OnMenuItemClickListener {

        // commentList가 비어있으면 현재 바인딩된 댓글 정보를 사용
        val commentModel: CommentModel = if (commentList.isEmpty()) {
            currentCommentItem ?: return@OnMenuItemClickListener true
        } else {
            if(isCommentOnly) commentList[adapterPosition] else commentList[adapterPosition - 1]
        }

        when (it.order) {
            REPORT_COMMENT -> {//댓글 신고

                val report: ReportDialogFragment = ReportDialogFragment.getInstance(commentModel, isCommentOnly)
                val color = "#" + Integer.toHexString(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.main
                    )
                ).substring(2)
                val msg: String = kotlin.String.format(itemView.context.resources.getString(R.string.report_comment),
                    "<FONT color=" + color + ">" + ConfigModel.getInstance(itemView.context).reportHeart + "</FONT>"
                )
                val spanned = HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY)
                report.setMessage(spanned)
                report.show((itemView.context as BaseActivity).supportFragmentManager, "report")

            }

            COPY_COMMENT -> {//복사 일떄 -> 클립보드에 복사 진행

                val clipboard =
                    mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val text = commentModel.content
                val clip = ClipData.newPlainText("Copied text", text)

                clipboard!!.setPrimaryClip(clip)
                stripMentionInClipboard()
                Toast.makeText(mContext, R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
                    .show()
            }

            DELETE_COMMENT -> {//댓글 삭제
                val frag = CommentRemoveDialogFragment.getInstance(
                    commentModel,
                    adapterPosition
                )
                frag.setActivityRequestCode(RequestCode.ARTICLE_COMMENT_REMOVE.value)
                frag.show((mContext as AppCompatActivity).supportFragmentManager, "remove")
            }
        }
        return@OnMenuItemClickListener true
    }


    // 클립보드에서 @{...} 으로 멘션 한 부분을 제거한다.
    private fun stripMentionInClipboard() {
        val clipboard =
            mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var count = 0
        if (clipboard.primaryClip != null) {
            count = clipboard.primaryClip!!.itemCount
        }
        if (count > 0) {
            var text = clipboard.primaryClip!!.getItemAt(0).text.toString()
            try {
                text = text.replace("@\\{\\d+\\:([^\\}]+)\\}".toRegex(), "")
            } catch (e: java.lang.Exception) {
            }

            // text가 ""면 clipboard가 지워지지 않는 현상이 있어서
            if (text.isEmpty()) {
                text = " "
            }
            val clip = ClipData.newPlainText("Copied text", text)
            clipboard.setPrimaryClip(clip)
        }
    }

    private fun setImage(imageUrl : String?, imageHeight : Int , imageWidth : Int){ with(binding) {
        if(imageHeight != 0 && imageWidth != 0){
            when {
                imageHeight >= imageWidth -> {   //높이가 더 길때
                    ivImageContent.layoutParams.height = Util.convertDpToPixel(itemView.context, 136f).toInt()
                    val imageWidthRatio = imageWidth * 136 / imageHeight
                    ivImageContent.layoutParams.width = Util.convertDpToPixel(itemView.context, imageWidthRatio.toFloat()).toInt()
                }
                else -> {
                    ivImageContent.layoutParams.width = Util.convertDpToPixel(itemView.context, 136f).toInt()
                    val imageHeightRatio = imageHeight * 136 / imageWidth
                    ivImageContent.layoutParams.height = Util.convertDpToPixel(itemView.context, imageHeightRatio.toFloat()).toInt()
                }
            }
            mGlideRequestManager
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(20))
                .into(ivImageContent)
            ivImageContent.adjustViewBounds = true
        }
        else{
            ivImageContent.layoutParams.width = Util.convertDpToPixel(itemView.context, 136f).toInt()
            ivImageContent.layoutParams.height = Util.convertDpToPixel(itemView.context, 136f).toInt()

            mGlideRequestManager
                .asBitmap()
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(40))
                .into(object : SimpleTarget<Bitmap>(){
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        ivImageContent.adjustViewBounds = true
                        ivImageContent.setImageBitmap(resource)

                        val imgWidth = resource.width
                        val imgHeight = resource.height

                        when {
                            imgHeight >= imgWidth -> {   //높이가 더 길때
                                ivImageContent.layoutParams.height = Util.convertDpToPixel(itemView.context, 136f).toInt()
                                val imageWidthRatio = imgWidth * 136 / imgHeight
                                ivImageContent.layoutParams.width = Util.convertDpToPixel(itemView.context, imageWidthRatio.toFloat()).toInt()
                            }
                            else -> {
                                ivImageContent.layoutParams.width = Util.convertDpToPixel(itemView.context, 136f).toInt()
                                val imageHeightRatio = imgHeight * 136 / imgWidth
                                ivImageContent.layoutParams.height = Util.convertDpToPixel(itemView.context, imageHeightRatio.toFloat()).toInt()
                            }
                        }
                    }
                })
        }
    }}

}