package net.ib.mn.presentation.friend.delete

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
 * 친구 삭제 화면
 */
@Composable
fun FriendDeleteScreen(
    modifier: Modifier = Modifier
) {
    val navigator = LocalAppNavigator.current

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.title_unfriend),
                onNavigationClick = { navigator.popBackStack() }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: 친구 삭제 컨텐츠
        }
    }
}
