package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.presentation.main.ranking.idol_subpage.VoteViewModel

/**
 * ExoVoteIcon - 독립적으로 사용 가능한 하트 투표 버튼
 *
 * old 프로젝트의 ranking_item.xml line 327-335 기반
 *
 * 주요 기능:
 * 1. 투표 다이얼로그 표시 (다이얼로그에서 자동으로 하트 정보 로드)
 * 2. 투표 API 호출
 * 3. 투표 성공/실패 처리
 * 4. 어디서나 독립적으로 사용 가능
 *
 * 사용 예:
 * ```
 * ExoVoteIcon(
 *     idolId = idol.id,
 *     fullName = "슬기_레드벨벳"
 * )
 * ```
 *
 * @param idolId 아이돌 ID
 * @param fullName "이름_그룹명" 형식의 전체 이름
 * @param onVoteSuccess 투표 성공 시 콜백 (옵션, 투표한 하트 개수)
 * @param modifier Modifier
 */
@Composable
fun ExoVoteIcon(
    idolId: Int,
    fullName: String,
    type: String = "DEFAULT",
    onVoteSuccess: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    voteViewModel: VoteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showVoteDialog by remember { mutableStateOf(false) }

    // 투표 다이얼로그 (다이얼로그 내부에서 하트 정보 로드)
    if (showVoteDialog) {
        ExoVoteDialog(
            fullName = fullName,
            onVote = { heart: Long ->
                scope.launch {
                    voteViewModel.voteIdol(
                        idolId = idolId,
                        heart = heart,
                        onSuccess = { response ->
                            showVoteDialog = false
                            onVoteSuccess?.invoke(heart)

                            // 투표 성공 메시지 (서버 응답 메시지 사용)
                            response.msg?.let {
                                android.widget.Toast.makeText(
                                    context,
                                    it,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onError = { errorMessage ->
                            showVoteDialog = false

                            // 에러 메시지
                            android.widget.Toast.makeText(
                                context,
                                errorMessage,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            onDismiss = {
                showVoteDialog = false
            },
            voteViewModel = voteViewModel
        )
    }

    // 타입에 따른 아이콘 렌더링
    when (type) {
        "CIRCLE" -> {
            Box(modifier=modifier){
                Icon(
                    painter = painterResource(R.drawable.btn_ranking_vote_heart),
                    contentDescription = "투표",
                    tint = ColorPalette.main,
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .clickable() {
                            showVoteDialog = true
                        }.background(ColorPalette.background100, CircleShape)
                )
            }
        }
        else -> {
            Box(
                modifier = modifier.padding(5.dp)
            ) {
                IconButton(
                    onClick = {
                        // 다이얼로그만 표시 (하트 정보는 다이얼로그에서 로드)
                        showVoteDialog = true
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.btn_ranking_vote_heart),
                        contentDescription = "투표",
                        tint = ColorPalette.main,
                        modifier = Modifier
                            .size(60.dp)
                            .padding(15.dp)
                    )
                }
            }
        }
    }
}
