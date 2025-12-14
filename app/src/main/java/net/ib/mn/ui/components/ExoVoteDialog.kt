package net.ib.mn.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.remote.dto.VoteResponse
import net.ib.mn.presentation.main.ranking.idol_subpage.VoteViewModel
import net.ib.mn.util.NumberFormatUtil

/**
 * ExoVoteDialog - 하트 투표 다이얼로그
 *
 * old 프로젝트의 VoteDialogFragment와 dialog_vote.xml 완전 동일하게 구현
 *
 * 주요 기능:
 * 1. 다국어 지원 (title_vote_heart, my_heart, ever_heart, weak_heart, confirm, btn_cancel 등)
 * 2. 하트 개수 선택 버튼 (1, 10, 50, 100, ALL, X DAILY ALL)
 * 3. 직접 입력 (EditText)
 * 4. 투표 확인 및 취소
 * 5. 다이얼로그 표시 시 자동으로 사용자 하트 정보 로드
 * 6. 투표 완료 후 바텀시트 표시 (100개 미만/이상 차이)
 *
 * @param idolId 아이돌 ID (투표 API 호출에 필요)
 * @param fullName "이름_그룹명" 형식의 전체 이름 (예: "슬기_레드벨벳")
 * @param onVote 투표 시 콜백 (투표 하트 개수)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onNavigateToCertificate 투표 인증서 화면 이동 콜백 (100개 이상 투표 시)
 * @param voteViewModel 투표 ViewModel (하트 정보 로드 및 투표 처리)
 */
@Composable
fun ExoVoteDialog(
    idolId: Int,
    fullName: String,
    idolHeart: Long = 0L,  // 아이돌의 현재 총 투표 수
    onVote: (Long) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToCertificate: (Int) -> Unit = {},
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
    var voteResult by remember { mutableStateOf<VoteResponse?>(null) }
    var votedHeart by remember { mutableLongStateOf(0L) }

    val coroutineScope = rememberCoroutineScope()
    val voteRankingTitle = stringResource(R.string.vote_ranking)


    // 투표 완료 바텀시트
    if (showVoteCompleteSheet && voteResult != null) {
        VoteCompleteBottomSheet(
            voteCount = votedHeart,
            bonusHeart = voteResult?.bonusHeart ?: 0,
            title = voteRankingTitle,
            subtitle = voteResult?.msg ?: "",
            idolName = fullName.substringBefore("_"),
            currentVoteCount = idolHeart - votedHeart,  // 내 투표수를 더하기 전 값
            onConfirm = { onNavigateToCertificate(idolId) },
            onDismiss = {
                showVoteCompleteSheet = false
                onDismiss()
            }
        )
    }

    // 기존 투표 다이얼로그 (showVoteCompleteSheet가 false일 때만)
    if (!showVoteCompleteSheet) {
        Dialog(onDismissRequest = onDismiss) {
        // old: line 10-15 - LinearLayoutCompat with bg_popup
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
                // 타이틀 (old: line 17-26)
                Text(
                    text = stringResource(R.string.title_vote_heart),
                    style = ExoTypo.typo16Bold.copy(color = ColorPalette.main),
                    modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
                    textAlign = TextAlign.Center
                )

                // 아이돌 이름 + 그룹명 (old: line 28-66)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExoNameWithGroup(
                        fullName = fullName,
                        nameFontSize = 14.sp,
                        groupFontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // 하트 정보 카드 (old: line 68-195)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 15.dp),
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
                        // 내 하트 (old: line 86-105)
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

                        // 에버하트 + 데일리하트 (old: line 107-191)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 에버하트 (old: line 107-142)
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

                            // + (old: line 144-156)
                            Text(
                                text = "+",
                                style = ExoTypo.typo15.copy(color = ColorPalette.textDimmed),
                                modifier = Modifier.padding(horizontal = 5.dp)
                            )

                            // 데일리하트 (old: line 158-191)
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

                // 하트 선택 버튼 (old: line 197-397)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                ) {
                    // 첫 번째 줄: 1, 10 (old: line 204-265)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // X 1
                        HeartButton(
                            count = 1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 1).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )

                        // X 10
                        HeartButton(
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

                    // 두 번째 줄: 50, 100 (old: line 267-330)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // X 50
                        HeartButton(
                            count = 50,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 50).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )

                        // X 100
                        HeartButton(
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

                    // 세 번째 줄: ALL, X DAILY ALL (old: line 332-395)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // X ALL (전체 하트)
                        HeartAllButton(
                            label = "X ALL",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                heartInput = totalHeart.toString()
                            }
                        )

                        // X DAILY ALL (데일리하트 전체)
                        HeartAllButton(
                            label = stringResource(R.string.X_DAILY_ALL),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                heartInput = freeHeart.toString()
                            }
                        )
                    }
                }

                // 하트 입력 필드 (old: line 399-415)
                BasicTextField(
                    value = heartInput,
                    onValueChange = { newValue ->
                        // 숫자만 입력 (old: digits="0123456789", maxLength="9")
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

                // 구분선 (old: line 417-421)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ColorPalette.gray100,
                    modifier = Modifier.padding(top = 12.dp)
                )

                // 확인/취소 버튼 (old: line 423-456)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 확인 버튼
                    TextButton(
                        onClick = {
                            val voteHeart = heartInput.toLongOrNull() ?: 0
                            if (voteHeart > 0 && voteHeart <= totalHeart) {
                                votedHeart = voteHeart
                                coroutineScope.launch {
                                    voteViewModel.voteIdol(
                                        idolId = idolId,
                                        heart = voteHeart,
                                        onSuccess = { response ->
                                            voteResult = response
                                            showVoteCompleteSheet = true
                                            onVote(voteHeart)
                                        },
                                        onError = { errorMsg ->
                                            // TODO: 에러 토스트 표시
                                        }
                                    )
                                }
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
 * old: line 204-330
 */
@Composable
private fun HeartButton(
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
 * old: line 332-395
 */
@Composable
private fun HeartAllButton(
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

/**
 * 투표 완료 바텀시트
 *
 * old 프로젝트의 VoteBottomSheetFragment와 동일
 * old: bottom_sheet_vote.xml
 *
 * 100개 미만 vs 100개 이상 차이:
 * - 100개 미만: 보너스 하트 표시 없음, 버튼 텍스트 "확인"
 * - 100개 이상: 보너스 하트 표시 (+N ❤️), 버튼 텍스트 "투표 인증서 확인", 클릭 시 인증서 화면 이동
 *
 * @param voteCount 투표한 하트 개수
 * @param bonusHeart 보너스 하트 (0이면 100개 미만 투표)
 * @param title 타이틀 (예: "순위 투표", "게시물 투표")
 * @param subtitle 서브타이틀 (API에서 받은 메시지 - 빈 문자열이면 기본 메시지 생성)
 * @param idolName 아이돌 이름 (기본 메시지 생성에 사용)
 * @param currentVoteCount 현재 총 투표 수 (기본 메시지 생성에 사용)
 * @param onConfirm 확인 버튼 클릭 (100개 이상일 때 인증서 화면 이동)
 * @param onDismiss 닫기 콜백
 */
@Composable
fun VoteCompleteBottomSheet(
    voteCount: Long,
    bonusHeart: Int,
    title: String,
    subtitle: String,
    idolName: String = "",
    currentVoteCount: Long = 0L,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasBonusHeart = bonusHeart > 0

    // subtitle이 비어있으면 기본 메시지 생성 (old 프로젝트와 동일)
    val displaySubtitle = if (subtitle.isEmpty()) {
        stringResource(
            R.string.response_v1_articles_give_heart,
            NumberFormatUtil.formatWithComma(voteCount),
            idolName,
            NumberFormatUtil.formatWithComma(currentVoteCount),
            NumberFormatUtil.formatWithComma(voteCount)
        )
    } else {
        subtitle
    }

    ExoRewardBottomSheet(
        imageRes = R.drawable.img_popup_vote,
        onDismiss = onDismiss
    ) {
        // old: tv_title - 22sp bold
        Text(
            text = title,
            style = ExoTypo.typo22Bold.copy(color = ColorPalette.textChat),
            textAlign = TextAlign.Center
        )

        // old: cl_heart - 보너스 하트 (100개 이상일 때만)
        if (hasBonusHeart) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "+",
                    style = ExoTypo.typo24Bold.copy(color = ColorPalette.main)
                )
                Text(
                    text = bonusHeart.toString(),
                    style = ExoTypo.typo24Bold.copy(color = ColorPalette.main)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Image(
                    painter = painterResource(R.drawable.img_popup_heart),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp, 17.dp)
                )
            }
        }

        // old: tv_subtitle - 14sp, marginTop 12dp, marginHorizontal 24dp
        Text(
            text = displaySubtitle,
            style = ExoTypo.typo14.copy(
                lineHeight = 20.sp,
                letterSpacing = (-0.3).sp,
                color = ColorPalette.textDimmed
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
        )

        // old: tv_confirm - 55dp height, marginTop 26dp, marginHorizontal 24dp, radius 28dp
        Button(
            onClick = {
                if (hasBonusHeart) {
                    onConfirm()
                }
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 26.dp)
                .height(55.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorPalette.main
            )
        ) {
            Text(
                text = if (hasBonusHeart)
                    stringResource(R.string.vote_certificate_popup_button)
                else
                    stringResource(R.string.btn_confirm),
                style = ExoTypo.typo16Bold.copy(color = Color.White)
            )
        }
    }
}
