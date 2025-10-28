/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 출석체크 할 수 있는 총 일수를 나타내는 클래스이다.
 * 현재는 10개가 최대이므로 Day10까지 넣어둠.
 *
 * */

package net.ib.mn.attendance


/**
 * @see
 * */

enum class AttendanceDays(val day: String, val days: Int) {
    DAY1("day1", 1),
    DAY2("day2", 2),
    DAY3("day3", 3),
    DAY4("day4", 4),
    DAY5("day5", 5),
    DAY6("day6", 6),
    DAY7("day7", 7),
    DAY8("day8", 8),
    DAY9("day9", 9),
    DAY10("day10", 10)
}