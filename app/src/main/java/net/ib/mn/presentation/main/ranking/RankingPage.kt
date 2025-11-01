package net.ib.mn.presentation.main.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.data.local.UserInfo
import net.ib.mn.util.ServerUrl
import com.google.gson.GsonBuilder

/**
 * Ranking 페이지
 */
@Composable
fun RankingPage(
    userInfo: UserInfo?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = ServerUrl.BASE_URL,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        // User Info as Pretty JSON
        if (userInfo != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
            ) {
                val horizontalScroll = rememberScrollState()

                Text(
                    text = userInfo.toPrettyJson(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.horizontalScroll(horizontalScroll)
                )
            }
        } else {
            Text(
                "Loading user info...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        // 로그아웃 버튼
        androidx.compose.material3.Button(
            onClick = onLogout,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("로그아웃")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * UserInfo를 Pretty JSON 문자열로 변환
 */
private fun UserInfo.toPrettyJson(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return try {
        gson.toJson(this)
    } catch (e: Exception) {
        "Error converting to JSON: ${e.message}"
    }
}
