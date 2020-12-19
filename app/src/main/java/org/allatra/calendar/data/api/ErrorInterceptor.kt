package org.allatra.calendar.data.api

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.allatra.calendar.common.Constants
import org.allatra.calendar.common.Constants.API_WAIT_THRESHOLD
import timber.log.Timber
import java.io.IOException

class ErrorInterceptor : Interceptor {
    var response: Response? = null
    var tryCount = 0
    var sleepInMillis: Long = API_WAIT_THRESHOLD

    override fun intercept(chain: Interceptor.Chain): Response? {
        val request: Request = chain.request()
        response = sendRequest(chain, request)

        while (response == null && tryCount < Constants.API_REPEAT_UNSUCCESSFUL) {
            Timber.e("Request failed: tryCount = $tryCount, sleepIn: $sleepInMillis")
            tryCount++
            try {
                Thread.sleep(sleepInMillis)
                sleepInMillis *= API_WAIT_THRESHOLD
            } catch (e: InterruptedException) {
                Timber.e("Could not make thread sleep.")
                e.printStackTrace()
            }
            response = sendRequest(chain, request)
        }
        return response
    }

    private fun sendRequest(chain: Interceptor.Chain, request: Request): Response? {
        return try {
            response = chain.proceed(request)

            response?.let {
                if (!it.isSuccessful) {
                    Timber.e("Request unsuccessful, Response code: ${it.code()}")
                    null
                } else response
            } ?: kotlin.run {
                Timber.e("Request unsuccessful, Response object null!!")
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e("Request unsuccessful. Exception caught.")
            null
        }
    }
}
