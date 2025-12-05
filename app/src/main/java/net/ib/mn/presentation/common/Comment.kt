package net.ib.mn.presentation.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.ib.mn.R
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.domain.model.TranslateState
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.Constants
import net.ib.mn.util.DateTimeUtil
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.LocaleUtil

/**
 * ExoCommentItem - 댓글 아이템 공용 컴포넌트
 *
 * old 프로젝트의 CommentViewHolder.kt 롱클릭 메뉴 로직 구현:
 * - 복사: 텍스트가 있는 댓글만 (이모티콘만 있는 경우 숨김)
 * - 신고: 내가 쓴 댓글이 아니고, 작성자가 매니저가 아닐 때
 * - 삭제: 내가 쓴 댓글이거나, 관리자거나, 게시글 작성자일 때
 *
 * @param comment 댓글 모델
 * @param currentUserId 현재 로그인한 사용자 ID
 * @param currentUserLevel 현재 로그인한 사용자 레벨 (heart)
 * @param articleAuthorId 게시글 작성자 ID (게시글 작성자는 모든 댓글 삭제 가능)
 * @param cdnUrl CDN 베이스 URL (이모티콘 이미지 URL 생성용)
 * @param useTranslation 번역 기능 사용 여부
 * @param translationLocales 번역 지원 로케일 목록
 * @param onProfileClick 프로필 클릭 콜백
 * @param onReportClick 신고 클릭 콜백
 * @param onDeleteClick 삭제 클릭 콜백
 * @param onImageClick 이미지/움짤 클릭 콜백
 * @param onTranslateClick 번역 클릭 콜백
 * @param onTranslatableChecked 번역 가능 여부 체크 콜백
 * @param modifier Modifier
 */
@Composable
fun ExoCommentItem(
    comment: CommentModel,
    currentUserId: Int = 0,
    currentUserLevel: Int = 0,
    articleAuthorId: Int = 0,
    cdnUrl: String,
    useTranslation: Boolean = false,
    translationLocales: List<String> = emptyList(),
    onProfileClick: ((CommentModel) -> Unit)? = null,
    onReportClick: ((CommentModel) -> Unit)? = null,
    onDeleteClick: ((CommentModel) -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    onTranslateClick: ((CommentModel) -> Unit)? = null,
    onTranslatableChecked: ((Int, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    // 메뉴 표시 조건 계산
    val commentUserId = comment.user?.id ?: 0
    val commentUserLevel = comment.user?.heart ?: 0
    val isMyComment = currentUserId != 0 && commentUserId == currentUserId
    val isAdmin = currentUserLevel == Constants.LEVEL_ADMIN
    val isArticleAuthor = currentUserId != 0 && articleAuthorId == currentUserId
    val isCommentAuthorManager = commentUserLevel == Constants.LEVEL_MANAGER

    // 복사 버튼: 텍스트가 있는 댓글만 (이모티콘만 있는 경우 숨김)
    val hasText = !comment.content.isNullOrEmpty() || !comment.contentAlt?.text.isNullOrEmpty()
    val showCopy = hasText

    // 신고 버튼: 내가 쓴 댓글이 아니고, 작성자가 매니저가 아닐 때
    val showReport = !isMyComment && !isCommentAuthorManager

    // 삭제 버튼: 내가 쓴 댓글이거나, 관리자거나, 게시글 작성자일 때
    val showDelete = isMyComment || isAdmin || isArticleAuthor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                with(density) {
                                    pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                                }
                                showMenu = true
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
            // 프로필 이미지
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onProfileClick?.invoke(comment) }
            ) {
                ExoProfileImage(
                    imageUrl = comment.user?.imageUrlCommunity,
                    type = ProfileImageType.SMALL
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 댓글 내용
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 닉네임, 레벨, 시간
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // 이모티콘
                        val emoticonUrl = comment.user?.emoticon?.emojiUrl
                        if (!emoticonUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = emoticonUrl,
                                contentDescription = "Emoticon",
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 1.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // 레벨 아이콘
                        val userLevel = comment.user?.level ?: 0
                        val maxLevel = 40
                        val clampedLevel = userLevel.coerceIn(0, maxLevel)
                        val levelIconRes = context.resources.getIdentifier(
                            "icon_level_$clampedLevel",
                            "drawable",
                            context.packageName
                        ).takeIf { it != 0 } ?: R.drawable.icon_level_0

                        Image(
                            painter = painterResource(levelIconRes),
                            contentDescription = "Level $userLevel",
                            modifier = Modifier.padding(end = 2.dp)
                        )

                        // 몰빵일 뱃지 (레벨 아이콘 우측)
                        if (comment.user?.hasAllInDayBadge == true) {
                            Image(
                                painter = painterResource(R.drawable.allinbadge_level_icon),
                                contentDescription = "All-in Day Badge",
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }

                        // 닉네임 (textDefault + bold)
                        Text(
                            text = comment.user?.nickname ?: "",
                            style = ExoTypo.body14.copy(
                                color = ColorPalette.textDefault,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 시간 (KST 풀타임)
                    val createdAt: String = remember(comment.createdAt) {
                        DateTimeUtil.formatFullDateKorea(comment.createdAt)
                    }
                    Text(
                        text = createdAt,
                        style = ExoTypo.body11.copy(color = ColorPalette.gray200),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 이모티콘 (emoticonId로 URL 생성 - old: UtilK.fileImageUrl)
                if (comment.hasEmoticon) {
                    val emoticonUrl = "$cdnUrl/e/${comment.effectiveEmoticonId}.t.webp".toSecureUrl()
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = emoticonUrl,
                            contentDescription = "Emoticon",
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // 이미지/움짤
                val imageUrl = comment.thumbnailUrl ?: comment.imageUrl ?: comment.umjjalUrl
                if (comment.hasImage || comment.hasUmjjal) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { imageUrl?.let { onImageClick?.invoke(it) } }
                    ) {
                        AsyncImage(
                            model = imageUrl.toSecureUrl(),
                            contentDescription = "Comment Image",
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // GIF 표시
                        if (comment.hasUmjjal) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(
                                        color = ColorPalette.textDefault.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GIF",
                                    style = ExoTypo.stat10.copy(color = ColorPalette.textWhiteBlack)
                                )
                            }
                        }
                    }
                }

                // 댓글 텍스트 (번역 상태에 따라 원본/번역 텍스트 표시)
                val contentText = when (comment.translateState) {
                    TranslateState.TRANSLATED -> comment.content  // 번역된 텍스트
                    else -> comment.originalContent ?: comment.contentAlt?.text ?: comment.content  // 원본 텍스트
                }
                if (!contentText.isNullOrEmpty()) {
                    Text(
                        text = contentText,
                        style = ExoTypo.body14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 번역 버튼 (old 프로젝트의 TranslateUiHelper.bindTranslateButton 로직)
                val systemLanguage = remember { LocaleUtil.getSystemLanguage(context) }
                val isTranslatable = remember(comment.id, comment.content, comment.nation, useTranslation) {
                    comment.isTranslatable ?: run {
                        val content = comment.content ?: ""
                        val nation = comment.nation
                        val shouldTranslate = useTranslation &&
                            content.isNotEmpty() &&
                            !nation.isNullOrEmpty() &&
                            !systemLanguage.lowercase().startsWith(nation.lowercase()) &&
                            LocaleUtil.extractTranslatable(content).isNotEmpty()
                        // 번역 가능 여부 콜백
                        onTranslatableChecked?.invoke(comment.id, shouldTranslate)
                        shouldTranslate
                    }
                }

                if (isTranslatable) {
                    Spacer(modifier = Modifier.height(6.dp))

                    val translateButtonText = when (comment.translateState) {
                        TranslateState.ORIGINAL -> {
                            // 한중일영서 이외의 언어는 "영어로 번역하기" 보여줌
                            val locale = systemLanguage.lowercase().replace("_", "-")
                            val isSupported = translationLocales.any { locale.startsWith(it) }
                            if (isSupported) {
                                stringResource(R.string.see_translate)
                            } else {
                                stringResource(R.string.see_translate_to_english)
                            }
                        }
                        TranslateState.TRANSLATING -> stringResource(R.string.translating)
                        TranslateState.TRANSLATED -> stringResource(R.string.see_original)
                    }

                    val translateTextColor = when (comment.translateState) {
                        TranslateState.TRANSLATING -> ColorPalette.textDimmed
                        else -> ColorPalette.textGray
                    }

                    Text(
                        text = translateButtonText,
                        style = ExoTypo.body12.copy(color = translateTextColor),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = comment.translateState != TranslateState.TRANSLATING
                            ) {
                                onTranslateClick?.invoke(comment)
                            }
                    )
                }
            }
            }

            // 롱클릭 메뉴 (old 프로젝트의 onCreateContextMenu 로직)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = pressOffset,
                modifier = Modifier.background(ColorPalette.background200)
            ) {
                // 복사 (텍스트가 있는 경우만)
                if (showCopy) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(android.R.string.copy),
                                style = ExoTypo.body14
                            )
                        },
                        onClick = {
                            showMenu = false
                            // 클립보드에 복사
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = comment.content ?: comment.contentAlt?.text ?: ""
                            // 멘션 패턴 제거: @{id:name} -> name
                            val cleanText = text.replace(Regex("@\\{\\d+:([^\\}]+)\\}"), "$1")
                            val clip = ClipData.newPlainText("Copied text", cleanText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 신고
                if (showReport) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.report),
                                style = ExoTypo.body14
                            )
                        },
                        onClick = {
                            showMenu = false
                            onReportClick?.invoke(comment)
                        }
                    )
                }

                // 삭제
                if (showDelete) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.title_remove),
                                style = ExoTypo.body14
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick?.invoke(comment)
                        }
                    )
                }
            }
        }

        HorizontalDivider(
            color = ColorPalette.gray100,
            thickness = 0.5.dp
        )
    }
}

/**
 * 댓글 빈 상태 표시
 */
@Composable
fun CommentEmpty(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.empty_comment),
            style = ExoTypo.body14.copy(color = ColorPalette.textDimmed)
        )
    }
}

/**
 * 댓글 로딩 인디케이터
 */
@Composable
fun CommentLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = ColorPalette.main,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}
