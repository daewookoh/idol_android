package net.ib.mn.tutorial

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.ib.mn.BuildConfig
import net.ib.mn.utils.CelebTutorialBits

object TutorialManager {

    private var tutorialBitmask: Long = 0L
    private var tutorialIndex = TutorialBits.NO_TUTORIAL
    private var isAllCompleted = false

    /**
     * 서버에서 받은 비트마스크를 초기값으로 설정합니다.
     */
    fun init(bitmask: Long) {
        tutorialBitmask = bitmask
        setTutorialIndex()
    }

    /**
     * 현재 비트마스크를 반환합니다.
     * 서버에 저장하거나 로깅에 사용됩니다.
     */
    fun getBitmask(): Long = tutorialBitmask

    /**
     * 주어진 비트가 ON(1) 상태인지 확인합니다.
     * 즉, 해당 튜토리얼이 보여졌는지를 판단합니다.
     */
    fun isShown(bit: Int): Boolean {
        return (tutorialBitmask and (1L shl bit)) != 0L
    }

    /**
     * 주어진 비트를 OFF(0)로 설정하여 튜토리얼 완료 상태로 처리합니다.
     */
    fun complete(bit: Int) {
        tutorialBitmask = tutorialBitmask and (1L shl bit).inv()
    }

    /**
     * 주어진 비트를 ON(1)으로 설정하여 다시 튜토리얼을 보이도록 초기화합니다.
     */
    fun reset(bit: Int) {
        tutorialBitmask = tutorialBitmask or (1L shl bit)
    }

    /**
     * 여러 개의 비트를 한 번에 OFF 처리합니다.
     * 모든 튜토리얼을 완료한 상태로 만들고 싶을 때 사용됩니다.
     */
    fun completeAll(bits: List<Int>) {
        bits.forEach { complete(it) }
    }

    /**
     * 여러 개의 비트를 한 번에 ON 처리합니다.
     * 특정 그룹의 튜토리얼을 다시 보여주고자 할 때 사용됩니다.
     */
    fun resetAll(bits: List<Int>) {
        bits.forEach { reset(it) }
    }

    /**
     * 주어진 비트 리스트 중 아직 보여지지 않은 튜토리얼의 비트만 추출합니다.
     * 실제 화면에서 어떤 튜토리얼을 보여줄지 결정할 때 사용됩니다.
     */
    fun getUnshownBits(bits: List<Int>): List<Int> {
        return bits.filterNot { isShown(it) }
    }

    /**
     * 주어진 비트 리스트 중 아직 보여지지 않은 튜토리얼 중 랜덤으로 하나 선택합니다.
     * 모두 보여졌다면 기본값(defaultValue)을 반환합니다.
     *
     * @param bits       검사할 튜토리얼 비트 리스트
     * @param defaultVal 모든 튜토리얼이 이미 완료된 경우 반환할 기본값 (기본값: -1)
     * @return           아직 보여지지 않은 튜토리얼 비트 중 하나, 또는 기본값
     */
    fun getRandomUnshownBitOrDefault(bits: List<Int>, defaultVal: Int = TutorialBits.NO_TUTORIAL): Int {
        val unshown = getUnshownBits(bits)
        return if (unshown.isEmpty()) defaultVal else unshown.random()
    }

    /**
     * 모든 비트를 ON 상태로 설정합니다.
     * 전체 튜토리얼을 초기화하고 처음부터 다시 보여주고 싶을 때 사용합니다.
     * @param maxBit 체크할 최대 비트 인덱스 (기본값은 63)
     */
    fun resetAllBits(maxBit: Int = 63) {
        tutorialBitmask = 0L
        for (i in 0..maxBit) {
            reset(i)
        }
    }

    /**
     * 주어진 비트 리스트의 상태를 문자열로 출력합니다.
     * 디버깅 또는 로깅용으로 사용됩니다.
     * 예: "bit 0=true, bit 1=false"
     */
    fun debugBitStatus(bits: List<Int>): String {
        return bits.joinToString { "bit $it=${isShown(it)}" }
    }

    fun initializeMainTutorialIndex() {
        tutorialIndex = TutorialBits.NO_TUTORIAL
    }

    /*
    * 튜토리얼 인덱스를 설정하는 함수입니다.
    * */
    fun setTutorialIndex() {
        val tutorialBits = if (BuildConfig.CELEB) CelebTutorialBits.all else TutorialBits.all
        tutorialIndex = getRandomUnshownBitOrDefault(tutorialBits)
    }

    /*
    * 튜토리얼 인덱스를 가져오는 함수입니다.
    * */
    fun getTutorialIndex(): Int {
        return tutorialIndex
    }

    /*
    * 완료하지 않은 튜토리얼 확인용
    * */
    fun getCompletedBits(bits: List<Int>): List<Int> {
        return bits.filterNot { isShown(it) }
    }
}

