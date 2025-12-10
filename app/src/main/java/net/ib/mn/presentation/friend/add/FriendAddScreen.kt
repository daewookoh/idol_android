package net.ib.mn.presentation.friend.add

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
 * 뉴프렌즈 (새 친구 추가) 화면
 */
@Composable
fun FriendAddScreen(
    modifier: Modifier = Modifier
) {
    val navigator = LocalAppNavigator.current

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.new_friends),
                onNavigationClick = { navigator.popBackStack() }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: 뉴프렌즈 컨텐츠
        }
    }
}
