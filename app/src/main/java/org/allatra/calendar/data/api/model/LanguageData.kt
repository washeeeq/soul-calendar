package org.allatra.calendar.data.api.model

import com.google.gson.annotations.SerializedName

data class LanguageData(
    @SerializedName("columns_order")
    val columnsOrder: Array<String>,

    @SerializedName("table")
    val table: Array<Array<Any>>
)
