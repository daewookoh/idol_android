package net.ib.mn.tutorial

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 튜토리얼 상태 관리자
 *
 * 비트마스크 기반으로 튜토리얼 완료 상태를 관리합니다.
 * Compose와 호환되도록 StateFlow를 사용합니다.
 *
 * 비트 상태:
 * - ON(1) = 튜토리얼 표시 필요 (아직 안 본 상태)
 * - OFF(0) = 튜토리얼 완료됨 (이미 본 상태)
 */
object TutorialManager {

    private val _tutorialBitmask = MutableStateFlow(0L)
    val tutorialBitmask: StateFlow<Long> = _tutorialBitmask.asStateFlow()

    private val _currentTutorialIndex = MutableStateFlow(TutorialBits.NO_TUTORIAL)
    val currentTutorialIndex: StateFlow<Int> = _currentTutorialIndex.asStateFlow()

    private var isAllCompleted = false

    /**
     * 서버에서 받은 비트마스크를 초기값으로 설정합니다.
     * 앱 시작 시 또는 튜토리얼 업데이트 API 응답 후 호출됩니다.
     */
    fun init(bitmask: Long) {
        _tutorialBitmask.value = bitmask
        setTutorialIndex()
    }


    /**
     * 현재 비트마스크를 반환합니다.
     */
    fun getBitmask(): Long = _tutorialBitmask.value

    /**
     * 주어진 비트가 ON(1) 상태인지 확인합니다.
     * 즉, 해당 튜토리얼이 아직 표시되지 않았는지를 판단합니다.
     *
     * @param bit 확인할 튜토리얼 비트
     * @return true = 표시 필요, false = 이미 완료됨
     */
    fun isShown(bit: Int): Boolean {
        return (_tutorialBitmask.value and (1L shl bit)) != 0L
    }

    /**
     * 주어진 비트를 OFF(0)로 설정하여 튜토리얼 완료 상태로 처리합니다.
     * 로컬에서만 변경하며, 서버 동기화는 별도로 처리해야 합니다.
     */
    fun complete(bit: Int) {
        _tutorialBitmask.value = _tutorialBitmask.value and (1L shl bit).inv()
    }

    /**
     * 주어진 비트를 ON(1)으로 설정하여 다시 튜토리얼을 보이도록 초기화합니다.
     */
    fun reset(bit: Int) {
        _tutorialBitmask.value = _tutorialBitmask.value or (1L shl bit)
    }

    /**
     * 여러 개의 비트를 한 번에 OFF 처리합니다.
     */
    fun completeAll(bits: List<Int>) {
        bits.forEach { complete(it) }
    }

    /**
     * 여러 개의 비트를 한 번에 ON 처리합니다.
     */
    fun resetAll(bits: List<Int>) {
        bits.forEach { reset(it) }
    }

    /**
     * 주어진 비트 리스트 중 아직 보여지지 않은 튜토리얼의 비트만 추출합니다.
     */
    fun getUnshownBits(bits: List<Int>): List<Int> {
        return bits.filter { isShown(it) }
    }

    /**
     * 주어진 비트 리스트 중 아직 보여지지 않은 튜토리얼 중 랜덤으로 하나 선택합니다.
     *
     * @param bits 검사할 튜토리얼 비트 리스트
     * @param defaultVal 모든 튜토리얼이 이미 완료된 경우 반환할 기본값
     * @return 아직 보여지지 않은 튜토리얼 비트 중 하나, 또는 기본값
     */
    fun getRandomUnshownBitOrDefault(bits: List<Int>, defaultVal: Int = TutorialBits.NO_TUTORIAL): Int {
        val unshown = getUnshownBits(bits)
        return if (unshown.isEmpty()) defaultVal else unshown.random()
    }

    /**
     * 모든 비트를 ON 상태로 설정합니다.
     * 전체 튜토리얼을 초기화하고 처음부터 다시 보여주고 싶을 때 사용합니다.
     *
     * @param maxBit 체크할 최대 비트 인덱스 (기본값은 63)
     */
    fun resetAllBits(maxBit: Int = 63) {
        var newBitmask = 0L
        for (i in 0..maxBit) {
            newBitmask = newBitmask or (1L shl i)
        }
        _tutorialBitmask.value = newBitmask
    }

    /**
     * 주어진 비트 리스트의 상태를 문자열로 출력합니다.
     * 디버깅 또는 로깅용으로 사용됩니다.
     */
    fun debugBitStatus(bits: List<Int>): String {
        return bits.joinToString { "bit $it=${isShown(it)}" }
    }

    /**
     * 메인 튜토리얼 인덱스를 초기화합니다.
     */
    fun initializeMainTutorialIndex() {
        _currentTutorialIndex.value = TutorialBits.NO_TUTORIAL
    }

    /**
     * 튜토리얼 인덱스를 설정합니다.
     * 아직 완료되지 않은 튜토리얼 중 랜덤으로 하나를 선택합니다.
     */
    fun setTutorialIndex() {
        _currentTutorialIndex.value = getRandomUnshownBitOrDefault(TutorialBits.all)
    }

    /**
     * 현재 튜토리얼 인덱스를 가져옵니다.
     */
    fun getTutorialIndex(): Int = _currentTutorialIndex.value

    /**
     * 완료되지 않은 튜토리얼 비트 리스트를 반환합니다.
     */
    fun getUncompletedBits(bits: List<Int>): List<Int> {
        return bits.filter { isShown(it) }
    }

    /**
     * 퀴즈를 제외한 인덱스 설정
     */
    fun setTutorialIndexWithoutQuiz() {
        val excludedBits = listOf(TutorialBits.MENU_QUIZ)
        _currentTutorialIndex.value = getRandomUnshownBitExcluding(TutorialBits.all, excludedBits)
    }

    /**
     * 특정 비트를 제외하고 아직 표시되지 않은 튜토리얼 비트를 무작위로 반환합니다.
     */
    fun getRandomUnshownBitExcluding(bits: List<Int>, excluded: List<Int>): Int {
        val unshown = getUnshownBits(bits).filterNot { it in excluded }
        return if (unshown.isEmpty()) TutorialBits.NO_TUTORIAL else unshown.random()
    }

    /**
     * 특정 비트가 현재 표시해야 할 튜토리얼인지 확인합니다.
     * Compose에서 특정 위치에 튜토리얼을 표시할지 결정할 때 사용합니다.
     */
    fun shouldShowTutorial(bit: Int): Boolean {
        return _currentTutorialIndex.value == bit && isShown(bit)
    }
}