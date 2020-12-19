package org.allatra.calendar.data.api.model

import org.allatra.calendar.common.Constants.ApiStatus
import org.allatra.calendar.common.Constants.ErrorType

data class Resource<out T>(val apiStatus: ApiStatus, val data: T?, val errorType: ErrorType?) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(ApiStatus.SUCCESS, data, null)
        }

        fun <T> error(errorType: ErrorType, data: T?): Resource<T> {
            return Resource(ApiStatus.ERROR, data, errorType)
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(ApiStatus.LOADING, data, null)
        }
    }
}
