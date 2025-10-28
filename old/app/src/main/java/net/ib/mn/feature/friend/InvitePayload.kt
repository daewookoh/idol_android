package net.ib.mn.feature.friend

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InvitePayload(
    val language: String = "en",
    val token: String
) : Parcelable