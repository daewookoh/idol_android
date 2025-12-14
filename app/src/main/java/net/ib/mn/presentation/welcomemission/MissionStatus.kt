package net.ib.mn.presentation.welcomemission

/**
 * 미션 상태
 *
 * old 프로젝트와 동일한 구조
 */
enum class MissionStatus(val status: String) {
    /**
     * 진행 중 - "GO" 버튼 표시
     */
    GOING("G"),

    /**
     * 보상 수령 가능 - 하트/다이아 개수 표시
     */
    REWARD("R"),

    /**
     * 완료됨 - "완료" 표시
     */
    DONE("D");

    companion object {
        fun fromStatus(status: String): MissionStatus {
            return entries.find { it.status == status } ?: GOING
        }
    }
}
