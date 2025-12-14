package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.R
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.presentation.main.ranking.idol_subpage.VoteViewModel
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.NumberFormatUtil

/**
 * ExoArticleVoteDialog - 게시글 하트 투표 다이얼로그
 *
 * ExoVoteDialog와 유사하지만 게시글 투표용으로 사용
 *
 * @param articleId 게시글 ID
 * @param articleHeart 게시글의 현재 총 하트 수
 * @param onVote 투표 시 콜백 (투표 하트 개수, 성공 콜백, 에러 콜백)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param voteViewModel 투표 ViewModel (하트 정보 로드용)
 */
@Composable
fun ExoArticleVoteDialog(
    articleId: String,
    articleHeart: Long = 0L,
    onVote: (Long, (ArticleVoteResponse) -> Unit, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    voteViewModel: VoteViewModel = hiltViewModel()
) {
    // 다이얼로그 표시 시 사용자 하트 정보 로드
    LaunchedEffect(Unit) {
        voteViewModel.loadUserHearts()
    }

    // VoteViewModel의 state 직접 사용
    val totalHeart = voteViewModel.totalHeart
    val freeHeart = voteViewModel.freeHeart
    val strongHeart = totalHeart - freeHeart

    var heartInput by remember { mutableStateOf("") }
    var showVoteCompleteSheet by remember { mutableStateOf(false) }
    var voteResult by remember { mutableStateOf<ArticleVoteResponse?>(null) }
    var votedHeart by remember { mutableLongStateOf(0L) }

    val votePostingTitle = stringResource(R.string.vote_posting)


    // 투표 완료 바텀시트
    if (showVoteCompleteSheet && voteResult != null) {
        VoteCompleteBottomSheet(
            voteCount = votedHeart,
            bonusHeart = voteResult?.bonusHeart ?: 0,
            title = votePostingTitle,
            subtitle = voteResult?.msg ?: "",
            currentVoteCount = articleHeart,
            onConfirm = { },
            onDismiss = {
                showVoteCompleteSheet = false
                onDismiss()
            }
        )
    }

    // 기존 투표 다이얼로그 (showVoteCompleteSheet가 false일 때만)
    if (!showVoteCompleteSheet) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(270.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ColorPalette.textWhiteBlack)
                .border(1.dp, ColorPalette.gray150, RoundedCornerShape(6.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 타이틀
                Text(
                    text = stringResource(R.string.title_vote_heart),
                    style = ExoTypo.typo16Bold.copy(color = ColorPalette.main),
                    modifier = Modifier.padding(top = 20.dp, bottom = 15.dp),
                    textAlign = TextAlign.Center
                )

                // 하트 정보 카드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorPalette.background200
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 내 하트
                        Text(
                            text = stringResource(R.string.my_heart),
                            style = ExoTypo.typo12.copy(color = ColorPalette.textDefault)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = NumberFormatUtil.formatWithComma(totalHeart),
                            style = ExoTypo.typo18Bold.copy(color = ColorPalette.mainLight)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 에버하트 + 데일리하트
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 에버하트
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.ever_heart),
                                    style = ExoTypo.typo12.copy(color = ColorPalette.textDimmed),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = NumberFormatUtil.formatWithComma(strongHeart),
                                    style = ExoTypo.typo14Bold.copy(color = ColorPalette.textGray),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // +
                            Text(
                                text = "+",
                                style = ExoTypo.typo15.copy(color = ColorPalette.textDimmed),
                                modifier = Modifier.padding(horizontal = 5.dp)
                            )

                            // 데일리하트
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.weak_heart),
                                    style = ExoTypo.typo12.copy(color = ColorPalette.textDimmed),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = NumberFormatUtil.formatWithComma(freeHeart),
                                    style = ExoTypo.typo14Bold.copy(color = ColorPalette.textGray),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 하트 선택 버튼
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                ) {
                    // 첫 번째 줄: 1, 10
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ArticleHeartButton(
                            count = 1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 1).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )

                        ArticleHeartButton(
                            count = 10,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 10).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 두 번째 줄: 50, 100
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ArticleHeartButton(
                            count = 50,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 50).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )

                        ArticleHeartButton(
                            count = 100,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 100).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 세 번째 줄: ALL, X DAILY ALL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ArticleHeartAllButton(
                            label = "X ALL",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                heartInput = totalHeart.toString()
                            }
                        )

                        ArticleHeartAllButton(
                            label = stringResource(R.string.X_DAILY_ALL),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                heartInput = freeHeart.toString()
                            }
                        )
                    }
                }

                // 하트 입력 필드
                BasicTextField(
                    value = heartInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 9)) {
                            heartInput = newValue
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ColorPalette.background300)
                        .padding(horizontal = 10.dp),
                    textStyle = ExoTypo.typo14.copy(
                        color = ColorPalette.main,
                        textAlign = TextAlign.Start
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            innerTextField()
                        }
                    }
                )

                // 구분선
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ColorPalette.gray100,
                    modifier = Modifier.padding(top = 12.dp)
                )

                // 확인/취소 버튼
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 확인 버튼
                    TextButton(
                        onClick = {
                            val voteHeart = heartInput.toLongOrNull() ?: 0
                            if (voteHeart > 0 && voteHeart <= totalHeart) {
                                votedHeart = voteHeart
                                onVote(
                                    voteHeart,
                                    { response ->
                                        voteResult = response
                                        showVoteCompleteSheet = true
                                    },
                                    { errorMsg ->
                                        // TODO: 에러 토스트 표시
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            style = ExoTypo.typo13.copy(color = ColorPalette.textGray)
                        )
                    }

                    // 세로 구분선
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(ColorPalette.gray100)
                    )

                    // 취소 버튼
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_cancel),
                            style = ExoTypo.typo13.copy(color = ColorPalette.textGray)
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * 하트 개수 선택 버튼 (1, 10, 50, 100)
 */
@Composable
private fun ArticleHeartButton(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ColorPalette.background300)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_heart_vote),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "X $count",
                style = ExoTypo.typo11.copy(color = ColorPalette.textDefault)
            )
        }
    }
}

/**
 * 하트 전체 선택 버튼 (ALL, X DAILY ALL)
 */
@Composable
private fun ArticleHeartAllButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ColorPalette.background300)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_heart_vote),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = ExoTypo.typo11.copy(color = ColorPalette.textDefault)
            )
        }
    }
}
