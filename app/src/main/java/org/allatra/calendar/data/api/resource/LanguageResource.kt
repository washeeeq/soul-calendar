package org.allatra.calendar.data.api.resource

import com.google.gson.annotations.SerializedName
import org.allatra.calendar.data.api.model.LanguageData

data class LanguageResource(
    @SerializedName("ok")
    val ok: Boolean,

    @SerializedName("data")
    val data: LanguageData
)
