package org.allatra.calendar.data.api

import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.allatra.calendar.common.Constants.API_URL
import org.allatra.calendar.common.Constants.BASE_URL
import org.allatra.calendar.common.Constants.CONNECT_READ_TIMEOUT
import org.allatra.calendar.common.Constants.CONNECT_TIMEOUT_SEC
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    /**
     * Provides the Retrofit object.
     * @return the Retrofit object
     */
    @JvmStatic
    private fun provideRetrofitInterface(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getClient())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .build()
    }

    @JvmStatic
    private fun getClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(interceptor)

        // Set timeouts
        builder.connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        builder.readTimeout(CONNECT_READ_TIMEOUT, TimeUnit.SECONDS)

        return builder.build()
    }

    @JvmStatic
    private fun getApi(): ApiCalendarInterface = provideRetrofitInterface().create(ApiCalendarInterface::class.java)

    /**
     * Returns languages.
     */
    fun getLanguages() = getApi().getLanguages()
}
