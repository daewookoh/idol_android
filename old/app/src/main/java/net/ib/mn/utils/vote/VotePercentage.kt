/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 투표 프로그래스바 비율 세팅 관련.
 *
 * */

package net.ib.mn.utils.vote

import kotlin.math.sqrt

/**
 * @see HEART_PICK_2ST 하트픽 2등 프로그래스바 비율.
 * @see MAX_PERCENTAGE 최대 퍼센테이지
 * @see DEFAULT_MIN_PERCENTAGE 디폴트로 들어가는 최소 퍼센테이지(값이 null일경우에 쓰임)
 * @see getVotePercentage 투표 프로그래스바 비율을 가져와줍니다.
 * @see doubleSquareRoot 단순 제곱근 구하는 확장함수.
 */
object VotePercentage {

    const val MAX_PERCENTAGE = 100
    const val MAIN_RANKING_MIN_PERCENTAGE = 45
    const val DEFAULT_MIN_PERCENTAGE = 32

    fun getVotePercentage(
        minPercentage: Int?,
        currentPlaceVote: Long,
        firstPlaceVote: Long,
        lastPlaceVote: Long,
    ): Int {
        // null이면 32로 설정.
        val minPercentageDiff = MAX_PERCENTAGE - (minPercentage ?: DEFAULT_MIN_PERCENTAGE)

        val sqrtDividend = currentPlaceVote.toDouble().doubleSquareRoot() - lastPlaceVote.toDouble()
            .doubleSquareRoot()
        val sqrtDivisor = firstPlaceVote.toDouble().doubleSquareRoot() - lastPlaceVote.toDouble()
            .doubleSquareRoot()

        val votePercentage: Double = (minPercentage?.toDouble()
            ?: DEFAULT_MIN_PERCENTAGE.toDouble()) + (sqrtDividend * minPercentageDiff / sqrtDivisor)

        // 실수로 계산하기 때문에 ArithmeticException이 발생할 수 없음.
        // 부동 IEEE 754 표준에 따라 (실수) / (0.0) 은 예외가 아니라 무한대 값임
        return if (votePercentage.isNaN() || votePercentage.isInfinite()) {
            MAX_PERCENTAGE // iOS와 동일하게 1등~마지막등까지 투표수가 같을 경우 100%로 표시
        } else {
            votePercentage.toInt()
        }
    }

    private fun Double.doubleSquareRoot() = sqrt(sqrt(this))

}