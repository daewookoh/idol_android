package net.ib.mn.domain.model

/**
 * 서버 응답 gcode 정의
 *
 * old 프로젝트의 ErrorControl.kt 기반으로 작성
 * 서버에서 반환하는 비즈니스 로직 에러 코드
 */
object GCode {
    /** 성공 */
    const val SUCCESS = 0

    /** 서버 점검 중 */
    const val MAINTENANCE = 88888

    // ========================================
    // 회원가입/로그인 관련 (1xxx)
    // ========================================
    /** 가입제한 사용자 */
    const val ERROR_1001 = 1001
    /** 비밀번호 찾기 실패 */
    const val ERROR_1002 = 1002
    /** 가입제한 */
    const val ERROR_1010 = 1010
    /** 닉네임 중복 */
    const val ERROR_1011 = 1011
    /** 닉네임 금지어 포함 */
    const val ERROR_1012 = 1012
    /** 닉네임 길이 초과 */
    const val ERROR_1013 = 1013
    /** 비밀번호 규칙 위반 */
    const val ERROR_1030 = 1030
    /** 로그인 실패 */
    const val ERROR_1031 = 1031

    // ========================================
    // 글쓰기/게시물 관련 (1xxx, 2xxx)
    // ========================================
    /** 글쓰기 금지된 사용자 */
    const val ERROR_1111 = 1111
    /** 원픽 밴 */
    const val ERROR_1117 = 1117
    /** 최애설정 API */
    const val ERROR_1143 = 1143
    const val ERROR_1144 = 1144
    const val ERROR_1145 = 1145
    /** 즐겨찾기 해제 실패 */
    const val ERROR_1164 = 1164

    // ========================================
    // 하트 관련 (1xxx)
    // ========================================
    /** 하트지급 실패 */
    const val ERROR_1200 = 1200
    const val ERROR_1201 = 1201
    /** 코플 적립금 하트 구매 실패 */
    const val ERROR_1912 = 1912
    /** 코플 계정 연동 */
    const val ERROR_1990 = 1990
    const val ERROR_1991 = 1991

    // ========================================
    // 게시물/댓글 작성 (2xxx)
    // ========================================
    /** 반복 작성 오류 */
    const val ERROR_2000 = 2000
    const val ERROR_2090 = 2090
    const val ERROR_2091 = 2091

    // ========================================
    // 신고 관련 (2xxx)
    // ========================================
    /** 게시물 신고 */
    const val ERROR_2200 = 2200
    /** 이미 신고됨 */
    const val ERROR_2201 = 2201
    const val ERROR_2202 = 2202
    const val ERROR_2203 = 2203
    /** 하트 부족 */
    const val ERROR_2204 = 2204

    /** 유저 신고 */
    const val ERROR_2300 = 2300
    const val ERROR_2301 = 2301
    const val ERROR_2302 = 2302
    const val ERROR_2303 = 2303
    const val ERROR_2304 = 2304

    /** 채팅방 신고 */
    const val ERROR_2400 = 2400
    const val ERROR_2401 = 2401
    const val ERROR_2402 = 2402
    const val ERROR_2403 = 2403
    const val ERROR_2404 = 2404

    /** 댓글 신고 */
    const val ERROR_2500 = 2500
    const val ERROR_2501 = 2501
    const val ERROR_2502 = 2502
    const val ERROR_2503 = 2503
    const val ERROR_2504 = 2504

    // ========================================
    // 결제/영수증 관련 (3xxx)
    // ========================================
    /** 하트상점 영수증 검증 */
    const val ERROR_3000 = 3000
    const val ERROR_3010 = 3010
    const val ERROR_3011 = 3011
    /** 비디오 광고 미적립 */
    const val ERROR_3333 = 3333
    /** 이미지 업로드 중복 */
    const val ERROR_3900 = 3900
    /** 게시글 작업중 */
    const val ERROR_3901 = 3901
    /** 이미지 처리 오류 */
    const val ERROR_3902 = 3902

    // ========================================
    // 구독 관련 (4xxx, 5xxx, 6xxx)
    // ========================================
    /** 구독 하트 수령 실패 */
    const val ERROR_4000 = 4000
    /** 투표 API */
    const val ERROR_4010 = 4010
    const val ERROR_4011 = 4011
    const val ERROR_4020 = 4020
    /** 구매 복원 실패 */
    const val ERROR_5000 = 5000
    /** 이미 구독중 */
    const val ERROR_5010 = 5010
    /** 다른 계정에서 구독중 */
    const val ERROR_5011 = 5011
    /** iOS에서 구독중 */
    const val ERROR_6000 = 6000

    // ========================================
    // 친구 관련 (8xxx)
    // ========================================
    const val ERROR_8000 = 8000
    const val ERROR_8001 = 8001
    const val ERROR_8003 = 8003

    // ========================================
    // 일반 오류 (9xxx)
    // ========================================
    const val ERROR_9000 = 9000
    const val ERROR_9900 = 9900
    const val ERROR_9901 = 9901
    const val ERROR_9902 = 9902
    const val ERROR_9903 = 9903
    const val ERROR_9904 = 9904
    const val ERROR_9996 = 9996
    const val ERROR_9997 = 9997
    const val ERROR_9998 = 9998
    const val ERROR_9999 = 9999

    /**
     * gcode가 점검 상태인지 확인
     */
    fun isMaintenance(gcode: Int?): Boolean = gcode == MAINTENANCE

    /**
     * gcode가 성공인지 확인
     */
    fun isSuccess(gcode: Int?): Boolean = gcode == SUCCESS || gcode == null
}
