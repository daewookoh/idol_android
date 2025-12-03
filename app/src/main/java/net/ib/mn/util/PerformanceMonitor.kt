package net.ib.mn.util

/**
 * 성능 측정 유틸리티
 *
 * 사용 예시:
 * ```
 * val perfMonitor = PerformanceMonitor()
 *
 * perfMonitor.start("DataLoad")
 * // ... DB 쿼리
 * perfMonitor.checkpoint("DataLoad", "DB Query Complete")
 * // ... 데이터 가공
 * perfMonitor.checkpoint("DataLoad", "Processing Complete")
 * // ... 완료
 * perfMonitor.end("DataLoad", itemCount = 100)
 * ```
 *
 * 출력 예시:
 * ```
 * D/Performance: [DataLoad] 📍 Started
 * D/Performance: [DataLoad] 🔵 Checkpoint: DB Query Complete
 *                ⏱️  Elapsed: 45ms
 *                💾 Memory Delta: 128KB
 * D/Performance: [DataLoad] ✅ Completed
 *                ⏱️  Duration: 158ms
 *                💾 Memory Delta: 568KB
 *                💾 Current Memory: 45MB
 *                📊 Items: 100 (1.58ms/item)
 * ```
 */
class PerformanceMonitor {
    private var startTime = 0L
    private var startMemory = 0L

    companion object {
        const val TAG = "Performance"

        /**
         * 성능 측정 로그 출력 여부
         * 프로덕션 빌드에서는 false로 설정하여 로그 비활성화
         */
        var ENABLED = true

        /**
         * 코드 블록의 실행 시간 측정
         *
         * @param tag 측정 태그
         * @param block 측정할 코드 블록
         * @return 코드 블록의 반환값
         */
        inline fun <T> measure(tag: String, block: () -> T): T {
            if (!ENABLED) return block()

            val startTime = System.currentTimeMillis()
            val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

            val result = block()

            val duration = System.currentTimeMillis() - startTime
            val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryDelta = currentMemory - startMemory

            logD(TAG, """
                [$tag] ⚡ Quick Measure
                ⏱️  Duration: ${duration}ms
                💾 Memory Delta: ${memoryDelta / 1024}KB
            """.trimIndent())

            return result
        }

        /**
         * suspend 함수의 실행 시간 측정
         *
         * @param tag 측정 태그
         * @param block 측정할 suspend 블록
         * @return 코드 블록의 반환값
         */
        suspend inline fun <T> measureSuspend(tag: String, crossinline block: suspend () -> T): T {
            if (!ENABLED) return block()

            val startTime = System.currentTimeMillis()
            val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

            val result = block()

            val duration = System.currentTimeMillis() - startTime
            val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val memoryDelta = currentMemory - startMemory

            logD(TAG, """
                [$tag] ⚡ Quick Measure (Suspend)
                ⏱️  Duration: ${duration}ms
                💾 Memory Delta: ${memoryDelta / 1024}KB
            """.trimIndent())

            return result
        }
    }

    /**
     * 성능 측정 시작
     *
     * @param tag 측정 태그 (예: "QueryDB", "DataMapping")
     */
    fun start(tag: String) {
        if (!ENABLED) return

        startTime = System.currentTimeMillis()
        startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        logD(TAG, "[$tag] 📍 Started")
    }

    /**
     * 성능 측정 종료 및 결과 출력
     *
     * @param tag 측정 태그
     * @param itemCount 처리한 아이템 수 (선택, 아이템당 평균 시간 계산에 사용)
     */
    fun end(tag: String, itemCount: Int = 0) {
        if (!ENABLED) return

        val duration = System.currentTimeMillis() - startTime
        val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryDelta = currentMemory - startMemory

        val itemStats = if (itemCount > 0) {
            "\n📊 Items: $itemCount (${String.format("%.2f", duration.toFloat() / itemCount)}ms/item)"
        } else {
            ""
        }

        logD(TAG, """
            [$tag] ✅ Completed
            ⏱️  Duration: ${duration}ms
            💾 Memory Delta: ${memoryDelta / 1024}KB
            💾 Current Memory: ${currentMemory / 1024 / 1024}MB$itemStats
        """.trimIndent())
    }

    /**
     * 중간 체크포인트 기록
     *
     * @param tag 측정 태그
     * @param checkpointName 체크포인트 이름 (예: "DB Query Complete", "Sorting Complete")
     */
    fun checkpoint(tag: String, checkpointName: String) {
        if (!ENABLED) return

        val duration = System.currentTimeMillis() - startTime
        val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryDelta = currentMemory - startMemory

        logD(TAG, """
            [$tag] 🔵 Checkpoint: $checkpointName
            ⏱️  Elapsed: ${duration}ms
            💾 Memory Delta: ${memoryDelta / 1024}KB
        """.trimIndent())
    }
}
