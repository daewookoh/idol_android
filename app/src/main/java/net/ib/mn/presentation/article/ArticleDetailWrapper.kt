package net.ib.mn.presentation.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.common.ExoArticleNavigation
import net.ib.mn.presentation.common.ExoArticleViewModel
import net.ib.mn.ui.theme.ColorPalette

/**
 * ArticleDetailWrapper - articleId로 게시글 데이터를 로드하여 ArticleDetailScreen을 표시
 * Navigation에서 articleId만 전달받고, API를 호출하여 ArticleModel을 가져옴
 */
@Composable
fun ArticleDetailWrapper(
    articleId: String,
    isFeed: Boolean = true,
    onBackClick: () -> Unit = {},
    viewModel: ExoArticleViewModel = hiltViewModel(),
    wrapperViewModel: ArticleDetailWrapperViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val scope = rememberCoroutineScope()

    var article by remember { mutableStateOf<ArticleModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // articleId로 게시글 데이터 로드
    LaunchedEffect(articleId) {
        val articleIdLong = articleId.toLongOrNull()
        if (articleIdLong != null) {
            viewModel.loadArticle(
                articleId = articleIdLong,
                onSuccess = { loadedArticle ->
                    article = loadedArticle
                    isLoading = false
                },
                onError = {
                    errorMessage = "게시글을 불러올 수 없습니다."
                    isLoading = false
                }
            )
        } else {
            errorMessage = "잘못된 게시글 ID입니다."
            isLoading = false
        }
    }

    // ExoArticleViewModel의 네비게이션 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ExoArticleNavigation.Profile -> {
                    // TODO: 프로필 화면으로 이동
                }
                is ExoArticleNavigation.MediaDetail -> {
                    // PhotoDetail로 이동
                    val imageUrls = event.article.mediaFiles.mapNotNull { it.originUrl }
                    val selectedUrl = imageUrls.getOrNull(event.mediaIndex) ?: imageUrls.firstOrNull()
                    if (selectedUrl != null) {
                        navigator.navigate(
                            Screen.PhotoDetail(imageUrl = selectedUrl)
                        )
                    }
                }
                is ExoArticleNavigation.Community -> {
                    navigator.navigate(Screen.Community(idolId = event.idolId))
                }
                is ExoArticleNavigation.EditArticle -> {
                    navigator.navigate(
                        Screen.ArticleWrite(
                            writeType = if (isFeed) "FEED" else "FREE_BOARD",
                            idolId = event.article.idol?.id,
                            editingArticleId = event.article.id
                        )
                    )
                }
                else -> { /* 다른 이벤트 무시 */ }
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorPalette.main)
            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ColorPalette.textGray
                )
            }
        }
        article != null -> {
            ArticleDetailScreen(
                article = article!!,
                isFeed = isFeed,
                onBackClick = onBackClick,
                onArticleUpdated = { updatedArticle ->
                    article = updatedArticle
                    // ArticleUpdateManager를 통해 다른 화면에 업데이트 전파
                    scope.launch {
                        wrapperViewModel.emitArticleUpdate(updatedArticle)
                    }
                },
                onArticleDeleted = {
                    // 삭제 이벤트 전파
                    scope.launch {
                        wrapperViewModel.emitArticleDeleted(articleId)
                    }
                    onBackClick()
                },
                onNavigateToProfile = { userId, nickname, imageUrl, level, mostIdolName ->
                    // TODO: 프로필 화면으로 이동
                },
                onNavigateToPhotoDetail = { photoArticle, mediaIndex ->
                    val imageUrls = photoArticle.mediaFiles.mapNotNull { it.originUrl }
                    val selectedUrl = imageUrls.getOrNull(mediaIndex) ?: imageUrls.firstOrNull()
                    if (selectedUrl != null) {
                        navigator.navigate(
                            Screen.PhotoDetail(imageUrl = selectedUrl)
                        )
                    }
                }
            )
        }
    }
}
