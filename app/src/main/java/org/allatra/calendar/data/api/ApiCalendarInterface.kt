package org.allatra.calendar.data.api

import org.allatra.calendar.data.api.resource.LanguageResource
import retrofit2.Call
import retrofit2.http.GET

interface ApiCalendarInterface {
    @GET("/motivators/get_languages")
    fun getLanguages(): Call<LanguageResource>
}
