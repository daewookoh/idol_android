package net.ib.mn.presentation.friend.waiting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.ib.mn.R
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold

/**
 * 친구 신청 관리 화면
 */
@Composable
fun FriendWaitingScreen(
    modifier: Modifier = Modifier
) {
    val navigator = LocalAppNavigator.current

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.friends_request),
                onNavigationClick = { navigator.popBackStack() }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: 친구 신청 관리 컨텐츠
        }
    }
}
