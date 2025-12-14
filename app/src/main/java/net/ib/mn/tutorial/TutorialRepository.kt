package net.ib.mn.tutorial

import kotlinx.coroutines.flow.StateFlow
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.api.UsersApi
import net.ib.mn.data.remote.dto.UpdateTutorialRequest
import net.ib.mn.util.logD
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 튜토리얼 Repository
 *
 * 튜토리얼 상태 관리와 서버 동기화를 담당합니다.
 * - 서버 API 호출 (튜토리얼 완료 처리)
 * - 로컬 저장소 동기화 (PreferencesManager)
 * - TutorialManager 상태 업데이트
 */
@Singleton
class TutorialRepository @Inject constructor(
    private val usersApi: UsersApi,
    private val preferencesManager: PreferencesManager
) {

    /**
     * 현재 튜토리얼 인덱스 StateFlow
     * Compose에서 collectAsState로 관찰 가능
     */
    val currentTutorialIndex: StateFlow<Int> = TutorialManager.currentTutorialIndex

    /**
     * 튜토리얼 비트마스크 StateFlow
     */
    val tutorialBitmask: StateFlow<Long> = TutorialManager.tutorialBitmask

    /**
     * 로컬 저장소에서 튜토리얼 비트마스크를 로드하여 TutorialManager를 초기화합니다.
     * 앱 시작 시 호출됩니다.
     */
    fun initFromLocal() {
        val bitmask = preferencesManager.getTutorialBitmaskSync()
        TutorialManager.init(bitmask)
    }

    /**
     * 서버에서 받은 튜토리얼 비트마스크로 초기화합니다.
     * ConfigSelf 또는 UserSelf API 응답 후 호출됩니다.
     */
    suspend fun initFromServer(bitmask: Long) {
        TutorialManager.init(bitmask)
        preferencesManager.setTutorialBitmask(bitmask)
    }

    /**
     * 특정 튜토리얼을 완료 처리합니다.
     *
     * 1. 즉시 로컬 상태 업데이트 (UI가 즉시 반영되도록)
     * 2. 로컬 저장소에 즉시 저장 (앱 재실행 시에도 유지되도록)
     * 3. 서버에 완료 요청
     * 4. 응답으로 받은 새 비트마스크로 TutorialManager 재동기화
     *
     * @param tutorialIndex 완료할 튜토리얼 비트 인덱스
     * @return API 성공 여부
     */
    suspend fun updateTutorial(tutorialIndex: Int): Result<Long> {
        logD(TAG, "updateTutorial: tutorialIndex=$tutorialIndex")

        // 즉시 로컬 상태 업데이트 (API 응답 전에 UI가 변경되도록)
        // 이렇게 하면 튜토리얼 하트가 즉시 사라지고, 같은 튜토리얼이 다시 표시되지 않음
        TutorialManager.complete(tutorialIndex)
        TutorialManager.setTutorialIndexWithoutQuiz()

        // 로컬 저장소에 즉시 저장 (앱 재실행 시에도 완료 상태 유지)
        val localBitmask = TutorialManager.getBitmask()
        preferencesManager.setTutorialBitmask(localBitmask)
        logD(TAG, "updateTutorial: localBitmask saved = $localBitmask")

        return try {
            val response = usersApi.updateTutorial(
                UpdateTutorialRequest(tutorialIndex)
            )

            val responseBody = response.body()
            logD(TAG, "updateTutorial: API response code=${response.code()}, success=${responseBody?.success}, tutorial=${responseBody?.tutorial}, raw=${response.errorBody()?.string()}")

            if (response.isSuccessful && responseBody?.success == true) {
                val serverBitmask = response.body()?.tutorial ?: 0L
                logD(TAG, "updateTutorial: serverBitmask=$serverBitmask")

                // 서버 응답과 현재 로컬 bitmask를 AND 연산으로 병합
                // API 호출 중 다른 튜토리얼이 로컬에서 완료되었을 수 있으므로
                // 어느 쪽에서든 완료(0)된 튜토리얼은 완료 상태 유지
                val currentLocalBitmask = TutorialManager.getBitmask()
                val mergedBitmask = serverBitmask and currentLocalBitmask
                logD(TAG, "updateTutorial: currentLocal=$currentLocalBitmask, merged=$mergedBitmask")

                TutorialManager.init(mergedBitmask)
                preferencesManager.setTutorialBitmask(mergedBitmask)

                Result.success(mergedBitmask)
            } else {
                // API 실패해도 로컬 상태는 유지 (사용자 경험 우선)
                logD(TAG, "updateTutorial: API failed, keeping local state")
                Result.failure(Exception("Tutorial update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            // 네트워크 오류 시에도 로컬 상태는 유지 (사용자 경험 우선)
            logD(TAG, "updateTutorial: Exception - ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "TutorialRepository"
    }

    /**
     * 특정 비트가 현재 표시해야 할 튜토리얼인지 확인합니다.
     */
    fun shouldShowTutorial(bit: Int): Boolean {
        return TutorialManager.shouldShowTutorial(bit)
    }

    /**
     * 현재 표시할 튜토리얼 인덱스를 반환합니다.
     */
    fun getTutorialIndex(): Int {
        return TutorialManager.getTutorialIndex()
    }

    /**
     * 튜토리얼 인덱스를 다시 계산합니다.
     * 화면 전환 시 호출하여 새로운 튜토리얼을 표시할 수 있습니다.
     */
    fun refreshTutorialIndex() {
        TutorialManager.setTutorialIndex()
    }
}
