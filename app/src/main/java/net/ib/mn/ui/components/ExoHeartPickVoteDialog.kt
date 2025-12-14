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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.NumberFormatUtil

/**
 * ExoHeartPickVoteDialog - 하트픽 전용 투표 다이얼로그
 *
 * old 프로젝트의 HeartPickVoteDialogFragment와 동일
 * 일반 아이돌 투표(ExoVoteDialog)와 다른 점:
 * - heartpick/vote/ 엔드포인트 사용
 * - heartpick_id, heartpick_idol_id 파라미터 전달
 * - 응답: bonus_heart, voted 필드
 *
 * @param heartPickIdolId 하트픽 아이돌 ID (HeartPickIdol.id)
 * @param idolId 아이돌 ID
 * @param fullName "이름_그룹명" 형식의 전체 이름
 * @param totalHeart 사용자 총 하트
 * @param freeHeart 사용자 데일리 하트
 * @param isVoting 투표 진행 중 여부
 * @param onVote 투표 실행 콜백 (heartPickIdolId, idolId, heart)
 * @param onDismiss 다이얼로그 닫기 콜백
 */
@Composable
fun ExoHeartPickVoteDialog(
    heartPickIdolId: Int,
    idolId: Int,
    fullName: String,
    totalHeart: Long,
    freeHeart: Long,
    isVoting: Boolean = false,
    onVote: (heartPickIdolId: Int, idolId: Int, heart: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val strongHeart = totalHeart - freeHeart
    var heartInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!isVoting) onDismiss() }) {
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
                    modifier = Modifier.padding(top = 20.dp, bottom = 5.dp),
                    textAlign = TextAlign.Center
                )

                // 아이돌 이름 + 그룹명
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

                // 하트 정보 카드
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
                        HeartPickHeartButton(
                            count = 1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 1).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )
                        HeartPickHeartButton(
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
                        HeartPickHeartButton(
                            count = 50,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val current = heartInput.toLongOrNull() ?: 0
                                val newValue = (current + 50).coerceAtMost(totalHeart)
                                heartInput = newValue.toString()
                            }
                        )
                        HeartPickHeartButton(
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
                        HeartPickHeartAllButton(
                            label = "X ALL",
                            modifier = Modifier.weight(1f),
                            onClick = { heartInput = totalHeart.toString() }
                        )
                        HeartPickHeartAllButton(
                            label = stringResource(R.string.X_DAILY_ALL),
                            modifier = Modifier.weight(1f),
                            onClick = { heartInput = freeHeart.toString() }
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
                            if (voteHeart > 0 && voteHeart <= totalHeart && !isVoting) {
                                onVote(heartPickIdolId, idolId, voteHeart)
                            }
                        },
                        enabled = !isVoting,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (isVoting) stringResource(R.string.loading) else stringResource(R.string.confirm),
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
                        onClick = { if (!isVoting) onDismiss() },
                        enabled = !isVoting,
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

@Composable
private fun HeartPickHeartButton(
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

@Composable
private fun HeartPickHeartAllButton(
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
 * 하트픽 투표 완료 바텀시트
 *
 * old 프로젝트의 HeartPickRewardDialogFragment와 동일
 *
 * @param voteCount 투표한 하트 개수
 * @param bonusHeart 보너스 하트
 * @param idolName 아이돌 이름 (이름_그룹명)
 * @param onShare 공유하기 버튼 클릭
 * @param onDismiss 닫기 콜백
 */
@Composable
fun HeartPickVoteCompleteBottomSheet(
    voteCount: Long,
    bonusHeart: Int,
    idolName: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasBonusHeart = bonusHeart > 0

    // old 프로젝트: msg_heartpick_vote_result
    // 포맷: "{아이돌명}\n{N}표 투표 완료"
    val displaySubtitle = stringResource(
        R.string.msg_heartpick_vote_result,
        NumberFormatUtil.formatWithComma(voteCount)
    )

    ExoBottomSheet(
        onDismissRequest = onDismiss,
        type = ExoBottomSheetType.DESIGN
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            // 콘텐츠 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 84.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ColorPalette.textWhiteBlack),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(84.dp + 18.dp))

                // 아이돌 이름
                Text(
                    text = idolName.replace("_", "\n"),
                    style = ExoTypo.typo22Bold.copy(color = ColorPalette.textChat),
                    textAlign = TextAlign.Center
                )

                // 보너스 하트 (있을 때만)
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

                // 투표 완료 메시지
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

                // 공유하기 버튼
                Button(
                    onClick = onShare,
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
                        text = stringResource(R.string.label_share),
                        style = ExoTypo.typo16Bold.copy(color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // 이미지
            Image(
                painter = painterResource(R.drawable.img_popup_vote),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .align(Alignment.TopCenter)
            )

            // 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 94.dp, end = 10.dp)
                    .size(62.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_popup_close),
                    contentDescription = "Close",
                    modifier = Modifier.padding(25.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}
