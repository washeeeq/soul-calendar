package org.allatra.calendar.common

object EnumDefinition {
    const val API_URL = "https://calendar.allatra.info/motivators/get_image?"
    const val API_PARAM_SC = "screen_resolution"
    const val API_PARAM_LI = "language_id"
    const val API_PARA_DAY = "day"
    const val API_PARAM_MONTH = "month"
    const val API_PARAM_YEAR = "year"

    enum class EnStatus {
        IDLE, DOWNLOADING, READY, FAILED
    }

    enum class EnLanguage(id: Int){
        RU(1)
    }
}