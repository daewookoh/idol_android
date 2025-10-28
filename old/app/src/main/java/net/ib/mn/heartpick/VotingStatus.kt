/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 하트픽 관련 투표 가능 여부.
 *
 * */

package net.ib.mn.heartpick


/**
 * @see
 * */

enum class VotingStatus(val status: Int) {
    BEFORE_VOTE(0),
    VOTING(1),
    VOTE_FINISHED(2)
}