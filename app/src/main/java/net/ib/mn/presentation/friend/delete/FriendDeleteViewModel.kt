package net.ib.mn.presentation.friend.delete

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.repository.DeleteFriendsResult
import net.ib.mn.data.repository.FriendsRepository
import javax.inject.Inject

/**
 * FriendDelete (친구 삭제) 화면 ViewModel
 *
 * old 프로젝트의 FriendDeleteActivity 로직을 MVI 패턴으로 구현.
 */
@HiltViewModel
class FriendDeleteViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<FriendDeleteContract.State, FriendDeleteContract.Intent, FriendDeleteContract.Effect>() {

    override fun createInitialState(): FriendDeleteContract.State = FriendDeleteContract.State()

    init {
        loadFriends()
    }

    override fun handleIntent(intent: FriendDeleteContract.Intent) {
        when (intent) {
            is FriendDeleteContract.Intent.LoadFriends -> loadFriends()
            is FriendDeleteContract.Intent.ToggleSelection -> toggleSelection(intent.userId)
            is FriendDeleteContract.Intent.ChangeSortType -> changeSortType(intent.sortType)
            is FriendDeleteContract.Intent.DeleteSelectedFriends -> requestDelete()
            is FriendDeleteContract.Intent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun loadFriends() {
        if (currentState.isLoading) return

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val response = friendsRepository.getFriendsSelf()

            if (!response.success) {
                setState { copy(isLoading = false, error = response.errorMessage) }
                setEffect {
                    FriendDeleteContract.Effect.ShowDialog(
                        null,
                        response.errorMessage ?: context.getString(R.string.desc_failed_to_connect_internet)
                    )
                }
                return@launch
            }

            // 친구 목록만 필터링 (isFriend == "Y")
            val friendItems = response.friends.map { friendModel ->
                FriendDeleteContract.FriendDeleteItem(
                    friendModel = friendModel,
                    giveHeart = friendModel.giveHeart,
                    lastAct = friendModel.user.lastAct
                )
            }

            // 기본 정렬: 로그인 시간순
            val sortedItems = sortItems(friendItems, FriendDeleteContract.SortType.LOGIN_TIME)

            setState {
                copy(
                    friends = sortedItems,
                    selectedIds = emptySet(),
                    isLoading = false
                )
            }
        }
    }

    private fun toggleSelection(userId: Int) {
        val newSelectedIds = if (currentState.selectedIds.contains(userId)) {
            currentState.selectedIds - userId
        } else {
            currentState.selectedIds + userId
        }
        setState { copy(selectedIds = newSelectedIds) }
    }

    private fun changeSortType(sortType: FriendDeleteContract.SortType) {
        val sortedItems = sortItems(currentState.friends, sortType)
        setState {
            copy(
                friends = sortedItems,
                sortType = sortType
            )
        }
    }

    private fun sortItems(
        items: List<FriendDeleteContract.FriendDeleteItem>,
        sortType: FriendDeleteContract.SortType
    ): List<FriendDeleteContract.FriendDeleteItem> {
        return when (sortType) {
            FriendDeleteContract.SortType.NAME -> items.sortedBy { it.friendModel.user.nickname }
            FriendDeleteContract.SortType.LOGIN_TIME -> items.sortedBy { it.lastAct?.time ?: 0L }
            FriendDeleteContract.SortType.HEART -> items.sortedWith(
                compareBy({ it.giveHeart }, { it.lastAct?.time ?: 0L })
            )
        }
    }

    private fun requestDelete() {
        if (currentState.selectedIds.isEmpty()) return

        // 삭제 확인 다이얼로그 표시
        setEffect { FriendDeleteContract.Effect.ShowDeleteConfirmDialog }
    }

    private fun confirmDelete() {
        val selectedIds = currentState.selectedIds.toList()
        if (selectedIds.isEmpty()) return

        setState { copy(isDeleting = true) }

        viewModelScope.launch {
            when (val result = friendsRepository.deleteFriends(selectedIds)) {
                is DeleteFriendsResult.Success -> {
                    // 삭제된 친구 제거
                    val remainingFriends = currentState.friends.filter {
                        !selectedIds.contains(it.friendModel.user.id)
                    }

                    setState {
                        copy(
                            friends = remainingFriends,
                            selectedIds = emptySet(),
                            isDeleting = false
                        )
                    }

                    // 삭제 완료 다이얼로그 표시
                    setEffect { FriendDeleteContract.Effect.ShowDeleteCompleteDialog }
                }
                is DeleteFriendsResult.Error -> {
                    setState { copy(isDeleting = false) }
                    setEffect {
                        FriendDeleteContract.Effect.ShowDialog(
                            null,
                            result.message ?: context.getString(R.string.error_abnormal_exception)
                        )
                    }
                }
            }
        }
    }
}
