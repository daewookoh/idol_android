package net.ib.mn.core.data.mapper

import net.ib.mn.core.data.model.VoteCertificateResponse
import net.ib.mn.core.model.VoteCertificateModel

internal fun VoteCertificateResponse.toData() : VoteCertificateModel =
    VoteCertificateModel(
        grade = this.grade,
        idol = this.idol.toData(),
        refDate = this.refDate,
        vote = this.vote
    )