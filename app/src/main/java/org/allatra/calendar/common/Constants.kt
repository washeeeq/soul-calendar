package org.allatra.calendar.common

object Constants {
    const val API_URL = "https://calendar.allatra.info/motivators/get_image?"
    const val BASE_URL = "https://calendar.allatra.info/"
    const val API_PARAM_SC = "screen_resolution"
    const val API_PARAM_LI = "language_id"
    const val API_PARA_DAY = "day"
    const val API_PARAM_MONTH = "month"
    const val API_PARAM_YEAR = "year"
    const val CONNECT_TIMEOUT_SEC = 60L
    const val CONNECT_READ_TIMEOUT = 10L
    const val DEFAULT_LOCALE = "ru"
    const val API_REPEAT_UNSUCCESSFUL = 3
    const val API_WAIT_THRESHOLD = 5000L

    // api get lang
    const val LANG_ID = "id"
    const val LANG_CODE = "two_chars_code"
    const val LANG_NAME = "translated_name"

    enum class EnStatus {
        IDLE, DOWNLOADING, READY, FAILED
    }

    enum class EnLanguage(id: Int){
        RU(1)
    }

    enum class ApiStatus {
        SUCCESS,
        ERROR,
        LOADING
    }

    enum class ErrorType {
        BACKEND_API,
        LOCAL_DB,
        NETWORK
    }
}
