package net.ib.mn.feature.votingcertificate

import net.ib.mn.R

enum class VotingCertificateGrade(val grade: String, val medal: Int) {
    BRONZE("bronze", R.drawable.img_votingcertificate_bronze),
    SILVER("silver", R.drawable.img_votingcertificate_silver),
    GOLD("gold", R.drawable.img_votingcertificate_gold),
    PLATINUM("platinum", R.drawable.img_votingcertificate_platinum)
}